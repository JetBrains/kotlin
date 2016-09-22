/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.child
import org.jetbrains.kotlin.load.java.lazy.resolveAnnotations
import org.jetbrains.kotlin.load.java.lazy.types.LazyJavaTypeAttributes
import org.jetbrains.kotlin.load.java.structure.JavaArrayType
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.load.java.typeEnhancement.enhanceSignatures
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude.NonExtensions
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

abstract class LazyJavaScope(protected val c: LazyJavaResolverContext) : MemberScopeImpl() {
    protected abstract val ownerDescriptor: DeclarationDescriptor

    // this lazy value is not used at all in LazyPackageFragmentScopeForJavaPackage because we do not use caching there
    // but is placed in the base class to not duplicate code
    private val allDescriptors = c.storageManager.createRecursionTolerantLazyValue<Collection<DeclarationDescriptor>>(
            { computeDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS) },
            // This is to avoid the following recursive case:
            //    when computing getAllPackageNames() we ask the JavaPsiFacade for all subpackages of foo
            //    it, in turn, asks JavaElementFinder for subpackages of Kotlin package foo, which calls getAllPackageNames() recursively
            //    when on recursive call we return an empty collection, recursion collapses gracefully
            listOf()
    )

    protected val declaredMemberIndex: NotNullLazyValue<DeclaredMemberIndex> = c.storageManager.createLazyValue { computeMemberIndex() }

    protected abstract fun computeMemberIndex(): DeclaredMemberIndex

    // Fake overrides, SAM constructors/adapters, values()/valueOf(), etc.
    protected abstract fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name)

    protected abstract fun getDispatchReceiverParameter(): ReceiverParameterDescriptor?

    private val functions = c.storageManager.createMemoizedFunction<Name, Collection<SimpleFunctionDescriptor>> {
        name ->
        val result = LinkedHashSet<SimpleFunctionDescriptor>()

        for (method in declaredMemberIndex().findMethodsByName(name)) {
            val descriptor = resolveMethodToFunctionDescriptor(method)
            if (!descriptor.isVisibleAsFunction()) continue

            c.components.javaResolverCache.recordMethod(method, descriptor)
            result.add(descriptor)
            if (method.isStatic) {
                result.addIfNotNull(c.components.samConversionResolver.resolveSamAdapter(descriptor))
            }
        }

        computeNonDeclaredFunctions(result, name)

        enhanceSignatures(result).toReadOnlyList()
    }

    open protected fun JavaMethodDescriptor.isVisibleAsFunction() = true

    protected data class MethodSignatureData(
            val returnType: KotlinType,
            val receiverType: KotlinType?,
            val valueParameters: List<ValueParameterDescriptor>,
            val typeParameters: List<TypeParameterDescriptor>,
            val hasStableParameterNames: Boolean,
            val errors: List<String>
    )

    protected abstract fun resolveMethodSignature(
            method: JavaMethod,
            methodTypeParameters: List<TypeParameterDescriptor>,
            returnType: KotlinType,
            valueParameters: List<ValueParameterDescriptor>
    ): MethodSignatureData

    protected fun resolveMethodToFunctionDescriptor(method: JavaMethod): JavaMethodDescriptor {
        val annotations = c.resolveAnnotations(method)
        val functionDescriptorImpl = JavaMethodDescriptor.createJavaMethod(
                ownerDescriptor, annotations, method.name, c.components.sourceElementFactory.source(method)
        )

        val c = c.child(functionDescriptorImpl, method)

        val methodTypeParameters = method.typeParameters.map { p -> c.typeParameterResolver.resolveTypeParameter(p)!! }
        val valueParameters = resolveValueParameters(c, functionDescriptorImpl, method.valueParameters)

        val returnType = computeMethodReturnType(method, annotations, c)

        val effectiveSignature = resolveMethodSignature(method, methodTypeParameters, returnType, valueParameters.descriptors)

        functionDescriptorImpl.initialize(
                effectiveSignature.receiverType,
                getDispatchReceiverParameter(),
                effectiveSignature.typeParameters,
                effectiveSignature.valueParameters,
                effectiveSignature.returnType,
                Modality.convertFromFlags(method.isAbstract, !method.isFinal),
                method.visibility
        )

        functionDescriptorImpl.setParameterNamesStatus(effectiveSignature.hasStableParameterNames, valueParameters.hasSynthesizedNames)

        if (effectiveSignature.errors.isNotEmpty()) {
            c.components.signaturePropagator.reportSignatureErrors(functionDescriptorImpl, effectiveSignature.errors)
        }

        return functionDescriptorImpl
    }

    protected fun computeMethodReturnType(method: JavaMethod, annotations: Annotations, c: LazyJavaResolverContext): KotlinType {
        val annotationMethod = method.containingClass.isAnnotationType
        val returnTypeAttrs = LazyJavaTypeAttributes(
                TypeUsage.MEMBER_SIGNATURE_COVARIANT, annotations,
                allowFlexible = !annotationMethod,
                isForAnnotationParameter = annotationMethod
        )
        return c.typeResolver.transformJavaType(method.returnType, returnTypeAttrs).let {
            // Annotation arguments are never null in Java
            if (annotationMethod) TypeUtils.makeNotNullable(it) else it
        }
    }

    protected class ResolvedValueParameters(val descriptors: List<ValueParameterDescriptor>, val hasSynthesizedNames: Boolean)

    protected fun resolveValueParameters(
            c: LazyJavaResolverContext,
            function: FunctionDescriptor,
            jValueParameters: List<JavaValueParameter>
    ): ResolvedValueParameters {
        var synthesizedNames = false
        val descriptors = jValueParameters.withIndex().map { pair ->
            val (index, javaParameter) = pair

            val annotations = c.resolveAnnotations(javaParameter)
            val typeUsage = LazyJavaTypeAttributes(TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT, annotations)
            val (outType, varargElementType) =
                    if (javaParameter.isVararg) {
                        val paramType = javaParameter.type as? JavaArrayType
                                        ?: throw AssertionError("Vararg parameter should be an array: $javaParameter")
                        val outType = c.typeResolver.transformArrayType(paramType, typeUsage, true)
                        outType to c.module.builtIns.getArrayElementType(outType)
                    }
                    else {
                        c.typeResolver.transformJavaType(javaParameter.type, typeUsage) to null
                    }

            val name = if (function.name.asString() == "equals" &&
                           jValueParameters.size == 1 &&
                           c.module.builtIns.getNullableAnyType() == outType) {
                // This is a hack to prevent numerous warnings on Kotlin classes that inherit Java classes: if you override "equals" in such
                // class without this hack, you'll be warned that in the superclass the name is "p0" (regardless of the fact that it's
                // "other" in Any)
                // TODO: fix Java parameter name loading logic somehow (don't always load "p0", "p1", etc.)
                Name.identifier("other")
            }
            else {
                // TODO: parameter names may be drawn from attached sources, which is slow; it's better to make them lazy
                val javaName = javaParameter.name
                if (javaName == null) synthesizedNames = true
                javaName ?: Name.identifier("p$index")
            }

            ValueParameterDescriptorImpl(
                    function,
                    null,
                    index,
                    annotations,
                    name,
                    outType,
                    /* declaresDefaultValue = */ false,
                    /* isCrossinline = */ false,
                    /* isNoinline = */ false,
                    /* isCoroutine = */ false,
                    varargElementType,
                    c.components.sourceElementFactory.source(javaParameter)
            )
        }.toList()
        return ResolvedValueParameters(descriptors, synthesizedNames)
    }

    private val functionNamesLazy by c.storageManager.createLazyValue { computeFunctionNames(DescriptorKindFilter.FUNCTIONS, null) }
    private val propertyNamesLazy by c.storageManager.createLazyValue { computePropertyNames(DescriptorKindFilter.VARIABLES, null) }

    override fun getFunctionNames() = functionNamesLazy
    override fun getVariableNames() = propertyNamesLazy

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        if (name !in getFunctionNames()) return emptyList()
        return functions(name)
    }

    protected open fun computeFunctionNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?): Set<Name> =
            declaredMemberIndex().getMethodNames()

    protected abstract fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>)

    protected open fun computePropertyNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?): Set<Name> =
            declaredMemberIndex().getFieldNames()

    private val properties = c.storageManager.createMemoizedFunction {
        name: Name ->
        val properties = ArrayList<PropertyDescriptor>()

        val field = declaredMemberIndex().findFieldByName(name)
        if (field != null && !field.isEnumEntry) {
            properties.add(resolveProperty(field))
        }

        computeNonDeclaredProperties(name, properties)

        if (DescriptorUtils.isAnnotationClass(ownerDescriptor))
            properties.toReadOnlyList()
        else
            enhanceSignatures(properties).toReadOnlyList()
    }

    private fun resolveProperty(field: JavaField): PropertyDescriptor {
        val propertyDescriptor = createPropertyDescriptor(field)
        propertyDescriptor.initialize(null, null)

        val propertyType = getPropertyType(field, propertyDescriptor.annotations)

        propertyDescriptor.setType(propertyType, listOf(), getDispatchReceiverParameter(), null as KotlinType?)

        if (DescriptorUtils.shouldRecordInitializerForProperty(propertyDescriptor, propertyDescriptor.getType())) {
            propertyDescriptor.setCompileTimeInitializer(
                    c.storageManager.createNullableLazyValue {
                        c.components.javaPropertyInitializerEvaluator.getInitializerConstant(field, propertyDescriptor)
                    })
        }

        c.components.javaResolverCache.recordField(field, propertyDescriptor)

        return propertyDescriptor
    }

    private fun createPropertyDescriptor(field: JavaField): PropertyDescriptorImpl {
        val isVar = !field.isFinal
        val annotations = c.resolveAnnotations(field)

        return JavaPropertyDescriptor.create(
                ownerDescriptor, annotations, Modality.FINAL, field.visibility, isVar, field.name,
                c.components.sourceElementFactory.source(field), /* isConst = */ field.isFinalStatic
        )
    }

    private val JavaField.isFinalStatic: Boolean
        get() = isFinal && isStatic

    private fun getPropertyType(field: JavaField, annotations: Annotations): KotlinType {
        // Fields do not have their own generic parameters.
        // Simple static constants should not have flexible types.
        val allowFlexible = !(field.isFinalStatic && c.components.javaPropertyInitializerEvaluator.isNotNullCompileTimeConstant(field))
        val propertyType = c.typeResolver.transformJavaType(
                field.type,
                LazyJavaTypeAttributes(TypeUsage.MEMBER_SIGNATURE_INVARIANT, annotations, allowFlexible)
        )
        if (!allowFlexible) {
            return TypeUtils.makeNotNullable(propertyType)
        }

        return propertyType
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        if (name !in getVariableNames()) return emptyList()
        return properties(name)
    }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) = allDescriptors()

    protected fun computeDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
    ): List<DeclarationDescriptor> {
        val result = LinkedHashSet<DeclarationDescriptor>()

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            for (name in computeClassNames(kindFilter, nameFilter)) {
                if (nameFilter(name)) {
                    // Null signifies that a class found in Java is not present in Kotlin (e.g. package class)
                    result.addIfNotNull(getContributedClassifier(name, location))
                }
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK) && !kindFilter.excludes.contains(NonExtensions)) {
            for (name in computeFunctionNames(kindFilter, nameFilter)) {
                if (nameFilter(name)) {
                    result.addAll(getContributedFunctions(name, location))
                }
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK) && !kindFilter.excludes.contains(NonExtensions)) {
            for (name in computePropertyNames(kindFilter, nameFilter)) {
                if (nameFilter(name)) {
                    result.addAll(getContributedVariables(name, location))
                }
            }
        }

        return result.toReadOnlyList()
    }

    protected abstract fun computeClassNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?): Set<Name>

    override fun toString() = "Lazy scope for $ownerDescriptor"

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName, " {")
        p.pushIndent()

        p.println("containingDeclaration: $ownerDescriptor")

        p.popIndent()
        p.println("}")
    }
}
