/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.load.java.components.ExternalSignatureResolver
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
import org.jetbrains.kotlin.load.java.typeEnhacement.enhanceSignatures
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.PLATFORM_TYPES
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude.NonExtensions
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.JetScopeImpl
import org.jetbrains.kotlin.resolve.scopes.UsageLocation
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.ArrayList
import java.util.LinkedHashSet

public abstract class LazyJavaMemberScope(
        protected val c: LazyJavaResolverContext,
        private val containingDeclaration: DeclarationDescriptor
) : JetScopeImpl() {
    // this lazy value is not used at all in LazyPackageFragmentScopeForJavaPackage because we do not use caching there
    // but is placed in the base class to not duplicate code
    private val allDescriptors = c.storageManager.createRecursionTolerantLazyValue<Collection<DeclarationDescriptor>>(
            { computeDescriptors(DescriptorKindFilter.ALL, JetScope.ALL_NAME_FILTER) },
            // This is to avoid the following recursive case:
            //    when computing getAllPackageNames() we ask the JavaPsiFacade for all subpackages of foo
            //    it, in turn, asks JavaElementFinder for subpackages of Kotlin package foo, which calls getAllPackageNames() recursively
            //    when on recursive call we return an empty collection, recursion collapses gracefully
            listOf()
    )

    override fun getContainingDeclaration() = containingDeclaration

    protected val memberIndex: NotNullLazyValue<MemberIndex> = c.storageManager.createLazyValue { computeMemberIndex() }

    protected abstract fun computeMemberIndex(): MemberIndex

    // Fake overrides, SAM constructors/adapters, values()/valueOf(), etc.
    protected abstract fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name)

    protected abstract fun getDispatchReceiverParameter(): ReceiverParameterDescriptor?

    private val functions = c.storageManager.createMemoizedFunction<Name, Collection<FunctionDescriptor>> {
        name ->
        val result = LinkedHashSet<SimpleFunctionDescriptor>()

        for (method in memberIndex().findMethodsByName(name)) {
            val descriptor = resolveMethodToFunctionDescriptor(method, true)
            result.add(descriptor)
            if (method.isStatic) {
                result.addIfNotNull(c.samConversionResolver.resolveSamAdapter(descriptor))
            }
        }

        computeNonDeclaredFunctions(result, name)

        val enhancedResult = enhanceSignatures(result)

        // Make sure that lazy things are computed before we release the lock
        for (f in enhancedResult) {
            for (p in f.getValueParameters()) {
                p.hasDefaultValue()
            }
        }

        enhancedResult.toReadOnlyList()
    }

    protected data class MethodSignatureData(
            val effectiveSignature: ExternalSignatureResolver.AlternativeMethodSignature,
            val errors: List<String>
    )

    protected abstract fun resolveMethodSignature(
            method: JavaMethod,
            methodTypeParameters: List<TypeParameterDescriptor>,
            returnType: JetType,
            valueParameters: ResolvedValueParameters): MethodSignatureData

    fun resolveMethodToFunctionDescriptor(method: JavaMethod, record: Boolean = true): JavaMethodDescriptor {
        val annotations = c.resolveAnnotations(method)
        val functionDescriptorImpl = JavaMethodDescriptor.createJavaMethod(
                containingDeclaration, annotations, method.getName(), c.sourceElementFactory.source(method)
        )

        val c = c.child(functionDescriptorImpl, method)

        val methodTypeParameters = method.getTypeParameters().map { p -> c.typeParameterResolver.resolveTypeParameter(p)!! }
        val valueParameters = resolveValueParameters(c, functionDescriptorImpl, method.getValueParameters())

        val returnType = computeMethodReturnType(method, annotations, c)

        val (effectiveSignature, signatureErrors) = resolveMethodSignature(method, methodTypeParameters, returnType, valueParameters)

        functionDescriptorImpl.initialize(
                effectiveSignature.getReceiverType(),
                getDispatchReceiverParameter(),
                effectiveSignature.getTypeParameters(),
                effectiveSignature.getValueParameters(),
                effectiveSignature.getReturnType(),
                Modality.convertFromFlags(method.isAbstract(), !method.isFinal()),
                method.getVisibility()
        )

        functionDescriptorImpl.setHasStableParameterNames(effectiveSignature.hasStableParameterNames())
        functionDescriptorImpl.setHasSynthesizedParameterNames(valueParameters.hasSynthesizedNames)

        if (record) {
            c.javaResolverCache.recordMethod(method, functionDescriptorImpl)
        }

        if (signatureErrors.isNotEmpty()) {
            c.externalSignatureResolver.reportSignatureErrors(functionDescriptorImpl, signatureErrors)
        }

        return functionDescriptorImpl
    }

    protected fun computeMethodReturnType(method: JavaMethod, annotations: Annotations, c: LazyJavaResolverContext): JetType {
        val annotationMethod = method.getContainingClass().isAnnotationType()
        val returnTypeAttrs = LazyJavaTypeAttributes(
                TypeUsage.MEMBER_SIGNATURE_COVARIANT, annotations,
                allowFlexible = !annotationMethod,
                isForAnnotationParameter = annotationMethod
        )
        return c.typeResolver.transformJavaType(method.getReturnType(), returnTypeAttrs).let {
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
                    if (javaParameter.isVararg()) {
                        val paramType = javaParameter.getType() as? JavaArrayType
                                        ?: throw AssertionError("Vararg parameter should be an array: $javaParameter")
                        val outType = c.typeResolver.transformArrayType(paramType, typeUsage, true)
                        outType to c.module.builtIns.getArrayElementType(outType)
                    }
                    else {
                        c.typeResolver.transformJavaType(javaParameter.getType(), typeUsage) to null
                    }

            val name = if (function.getName().asString() == "equals" &&
                           jValueParameters.size() == 1 &&
                           c.module.builtIns.getNullableAnyType() == outType) {
                // This is a hack to prevent numerous warnings on Kotlin classes that inherit Java classes: if you override "equals" in such
                // class without this hack, you'll be warned that in the superclass the name is "p0" (regardless of the fact that it's
                // "other" in Any)
                // TODO: fix Java parameter name loading logic somehow (don't always load "p0", "p1", etc.)
                Name.identifier("other")
            }
            else {
                // TODO: parameter names may be drawn from attached sources, which is slow; it's better to make them lazy
                val javaName = javaParameter.getName()
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
                    false,
                    varargElementType,
                    c.sourceElementFactory.source(javaParameter)
            )
        }.toList()
        return ResolvedValueParameters(descriptors, synthesizedNames)
    }

    override fun getFunctions(name: Name, location: UsageLocation) = functions(name)

    protected open fun getFunctionNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name>
            = memberIndex().getMethodNames(nameFilter)

    protected abstract fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>)

    protected abstract fun getPropertyNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name>

    private val properties = c.storageManager.createMemoizedFunction {
        name: Name ->
        val properties = ArrayList<PropertyDescriptor>()

        val field = memberIndex().findFieldByName(name)
        if (field != null && !field.isEnumEntry()) {
            properties.add(resolveProperty(field))
        }

        computeNonDeclaredProperties(name, properties)

        if (DescriptorUtils.isAnnotationClass(containingDeclaration))
            properties.toReadOnlyList()
        else
            enhanceSignatures(properties).toReadOnlyList()
    }

    private fun resolveProperty(field: JavaField): PropertyDescriptor {
        val isVar = !field.isFinal()
        val propertyDescriptor = createPropertyDescriptor(field)
        propertyDescriptor.initialize(null, null)

        val propertyType = getPropertyType(field, propertyDescriptor.getAnnotations())
        val effectiveSignature = c.externalSignatureResolver.resolveAlternativeFieldSignature(field, propertyType, isVar)
        val signatureErrors = effectiveSignature.getErrors()
        if (!signatureErrors.isEmpty()) {
            c.externalSignatureResolver.reportSignatureErrors(propertyDescriptor, signatureErrors)
        }

        propertyDescriptor.setType(effectiveSignature.getReturnType(), listOf(), getDispatchReceiverParameter(), null : JetType?)

        if (DescriptorUtils.shouldRecordInitializerForProperty(propertyDescriptor, propertyDescriptor.getType())) {
            propertyDescriptor.setCompileTimeInitializer(
                    c.storageManager.createNullableLazyValue {
                        c.javaPropertyInitializerEvaluator.getInitializerConstant(field, propertyDescriptor)
                    })
        }

        c.javaResolverCache.recordField(field, propertyDescriptor);

        return propertyDescriptor
    }

    private fun createPropertyDescriptor(field: JavaField): PropertyDescriptorImpl {
        val isVar = !field.isFinal()
        val visibility = field.getVisibility()
        val annotations = c.resolveAnnotations(field)
        val propertyName = field.getName()

        return JavaPropertyDescriptor(containingDeclaration, annotations, visibility, isVar, propertyName,
                                      c.sourceElementFactory.source(field), /* original = */ null)
    }

    private fun getPropertyType(field: JavaField, annotations: Annotations): JetType {
        // Fields do not have their own generic parameters
        val finalStatic = field.isFinal() && field.isStatic()

        // simple static constants should not have flexible types:
        val allowFlexible = PLATFORM_TYPES && !(finalStatic && c.javaPropertyInitializerEvaluator.isNotNullCompileTimeConstant(field))
        val propertyType = c.typeResolver.transformJavaType(
                field.getType(),
                LazyJavaTypeAttributes(TypeUsage.MEMBER_SIGNATURE_INVARIANT, annotations, allowFlexible)
        )
        if ((!allowFlexible || !PLATFORM_TYPES) && finalStatic) {
            return TypeUtils.makeNotNullable(propertyType)
        }

        return propertyType
    }

    override fun getProperties(name: Name, location: UsageLocation): Collection<VariableDescriptor> = properties(name)

    override fun getOwnDeclaredDescriptors() = getDescriptors()

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean) = allDescriptors()

    protected fun computeDescriptors(kindFilter: DescriptorKindFilter,
                                     nameFilter: (Name) -> Boolean): List<DeclarationDescriptor> {
        val result = LinkedHashSet<DeclarationDescriptor>()

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            for (name in getClassNames(kindFilter, nameFilter)) {
                if (nameFilter(name)) {
                    // Null signifies that a class found in Java is not present in Kotlin (e.g. package class)
                    result.addIfNotNull(getClassifier(name))
                }
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK) && !kindFilter.excludes.contains(NonExtensions)) {
            for (name in getFunctionNames(kindFilter, nameFilter)) {
                if (nameFilter(name)) {
                    result.addAll(getFunctions(name))
                }
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK) && !kindFilter.excludes.contains(NonExtensions)) {
            for (name in getPropertyNames(kindFilter, nameFilter)) {
                if (nameFilter(name)) {
                    result.addAll(getProperties(name))
                }
            }
        }

        addExtraDescriptors(result, kindFilter, nameFilter)

        return result.toReadOnlyList()
    }

    protected open fun addExtraDescriptors(result: MutableSet<DeclarationDescriptor>,
                                           kindFilter: DescriptorKindFilter,
                                           nameFilter: (Name) -> Boolean) {
        // Do nothing
    }

    protected abstract fun getClassNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name>

    override fun toString() = "Lazy scope for ${getContainingDeclaration()}"
    
    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.println("containingDeclaration: ${getContainingDeclaration()}")

        p.popIndent()
        p.println("}")
    }
}
