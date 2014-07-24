/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.storage.NotNullLazyValue
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.resolve.java.structure.JavaField
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContextWithTypes
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaMethodDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.java.lazy.child
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.jet.lang.resolve.java.lazy.resolveAnnotations
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType
import org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.java.lazy.hasNotNullAnnotation
import org.jetbrains.jet.lang.resolve.java.lazy.types.LazyJavaTypeAttributes
import org.jetbrains.jet.lang.resolve.java.lazy.hasMutableAnnotation
import org.jetbrains.jet.lang.resolve.java.lazy.hasReadOnlyAnnotation
import org.jetbrains.jet.lang.resolve.java.structure.JavaValueParameter
import java.util.ArrayList
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import java.util.LinkedHashSet
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaPropertyDescriptor
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl
import java.util.Collections
import org.jetbrains.jet.lang.resolve.java.resolver.ExternalSignatureResolver
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils
import org.jetbrains.jet.utils.*

public abstract class LazyJavaMemberScope(
        protected val c: LazyJavaResolverContextWithTypes,
        private val _containingDeclaration: DeclarationDescriptor
) : JetScope {
    private val _allDescriptors = c.storageManager.createRecursionTolerantLazyValue<Collection<DeclarationDescriptor>>(
            {computeAllDescriptors()},
            // This is to avoid the following recursive case:
            //    when computing getAllPackageNames() we ask the JavaPsiFacade for all subpackages of foo
            //    it, in turn, asks JavaElementFinder for subpackages of Kotlin package foo, which calls getAllPackageNames() recursively
            //    when on recursive call we return an empty collection, recursion collapses gracefully
            Collections.emptyList()
    )

    override fun getContainingDeclaration() = _containingDeclaration

    protected val memberIndex: NotNullLazyValue<MemberIndex> = c.storageManager.createLazyValue {
        computeMemberIndex()
    }

    protected abstract fun computeMemberIndex(): MemberIndex

    protected abstract fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name)

    private val _functions = c.storageManager.createMemoizedFunction {
        (name: Name): Collection<FunctionDescriptor>
        ->
        val methods = memberIndex().findMethodsByName(name)
        val functions = LinkedHashSet<SimpleFunctionDescriptor>(
                methods.stream()
                      // values() and valueOf() are added manually, see LazyJavaClassDescriptor::getClassObjectDescriptor()
                      .filter{ m -> !DescriptorResolverUtils.shouldBeInEnumClassObject(m) }
                      .flatMap {
                              m ->
                              val function = resolveMethodToFunctionDescriptor(m, true)
                              val samAdapter = resolveSamAdapter(function)
                              if (samAdapter != null)
                                  listOf(function, samAdapter).stream()
                              else
                                  listOf(function).stream()
                      }.toList())

        computeNonDeclaredFunctions(functions, name)

        // Make sure that lazy things are computed before we release the lock
        for (f in functions) {
            for (p in f.getValueParameters()) {
                p.hasDefaultValue()
            }
        }

        functions
    }

    data class MethodSignatureData(
            val effectiveSignature: ExternalSignatureResolver.AlternativeMethodSignature,
            val superFunctions: List<FunctionDescriptor>,
            val errors: List<String>
    )

    abstract fun resolveMethodSignature(method: JavaMethod, methodTypeParameters: List<TypeParameterDescriptor>,
                                        returnType: JetType, valueParameters: ResolvedValueParameters): MethodSignatureData

    fun resolveMethodToFunctionDescriptor(method: JavaMethod, record: Boolean = true): JavaMethodDescriptor {

        val functionDescriptorImpl = JavaMethodDescriptor.createJavaMethod(
                _containingDeclaration, c.resolveAnnotations(method), method.getName(), c.sourceElementFactory.source(method)
        )

        val c = c.child(functionDescriptorImpl, method.getTypeParameters().toSet())

        val methodTypeParameters = method.getTypeParameters().map { p -> c.typeParameterResolver.resolveTypeParameter(p)!! }
        val valueParameters = resolveValueParameters(c, functionDescriptorImpl, method.getValueParameters())

        val returnTypeAttrs = LazyJavaTypeAttributes(c, method, TypeUsage.MEMBER_SIGNATURE_COVARIANT) {
            if (c.hasReadOnlyAnnotation(method) && !c.hasMutableAnnotation(method))
                TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
            else
                TypeUsage.MEMBER_SIGNATURE_COVARIANT
        }

        val returnJavaType = method.getReturnType() ?: throw IllegalStateException("Constructor passed as method: $method")
        val returnType = c.typeResolver.transformJavaType(returnJavaType, returnTypeAttrs).let {
            // Annotation arguments are never null in Java
            if (method.getContainingClass().isAnnotationType()) TypeUtils.makeNotNullable(it) else it
        }

        val (effectiveSignature, superFunctions, signatureErrors) = resolveMethodSignature(method, methodTypeParameters, returnType, valueParameters)

        functionDescriptorImpl.initialize(
                effectiveSignature.getReceiverType(),
                DescriptorUtils.getExpectedThisObjectIfNeeded(_containingDeclaration),
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

        c.methodSignatureChecker.checkSignature(method, record, functionDescriptorImpl, signatureErrors, superFunctions)

        return functionDescriptorImpl
    }

    protected class ResolvedValueParameters(val descriptors: List<ValueParameterDescriptor>, val hasSynthesizedNames: Boolean)
    protected fun resolveValueParameters(
            c: LazyJavaResolverContextWithTypes,
            function: FunctionDescriptor,
            jValueParameters: List<JavaValueParameter>
    ): ResolvedValueParameters {
        var synthesizedNames = false
        val descriptors = jValueParameters.withIndices().map {
            pair ->
            val (index, javaParameter) = pair

            val typeUsage = LazyJavaTypeAttributes(c, javaParameter, TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT) {
                    if (c.hasMutableAnnotation(javaParameter)) TypeUsage.MEMBER_SIGNATURE_COVARIANT else TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT
            }

            val (outType, varargElementType) =
                if (javaParameter.isVararg()) {
                    val paramType = javaParameter.getType()
                    assert (paramType is JavaArrayType, "Vararg parameter should be an array: $paramType")
                    val arrayType = c.typeResolver.transformArrayType(paramType as JavaArrayType, typeUsage, true)
                    val outType = TypeUtils.makeNotNullable(arrayType)
                    Pair(outType, KotlinBuiltIns.getInstance().getArrayElementType(outType))
                }
                else {
                    val jetType = c.typeResolver.transformJavaType(javaParameter.getType(), typeUsage)
                    if (jetType.isNullable() && c.hasNotNullAnnotation(javaParameter))
                        Pair(TypeUtils.makeNotNullable(jetType), null)
                    else Pair(jetType, null)
                }

            val name = if (function.getName().asString() == "equals" &&
                           jValueParameters.size() == 1 &&
                           KotlinBuiltIns.getInstance().getNullableAnyType() == outType) {
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
                    c.resolveAnnotations(javaParameter),
                    name,
                    outType,
                    false,
                    varargElementType,
                    c.sourceElementFactory.source(javaParameter)
            )
        }.toList()
        return ResolvedValueParameters(descriptors, synthesizedNames)
    }

    private fun resolveSamAdapter(original: JavaMethodDescriptor): JavaMethodDescriptor? {
        return if (SingleAbstractMethodUtils.isSamAdapterNecessary(original))
                    SingleAbstractMethodUtils.createSamAdapterFunction(original) as JavaMethodDescriptor
               else null
    }

    override fun getFunctions(name: Name) = _functions(name)
    protected open fun getAllFunctionNames(): Collection<Name> = memberIndex().getAllMethodNames()

    protected abstract fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>)

    val _properties = c.storageManager.createMemoizedFunction {
        (name: Name) ->
        val properties = ArrayList<PropertyDescriptor>()

        val field = memberIndex().findFieldByName(name)
        if (field != null && !field.isEnumEntry()) {
            properties.add(resolveProperty(field))
        }

        computeNonDeclaredProperties(name, properties)

        properties
    }

    private fun resolveProperty(field: JavaField): PropertyDescriptor {
        val isVar = !field.isFinal()
        val propertyDescriptor = createPropertyDescriptor(field)
        propertyDescriptor.initialize(null, null)

        val propertyType = getPropertyType(field)
        val effectiveSignature = c.externalSignatureResolver.resolveAlternativeFieldSignature(field, propertyType, isVar)
        val signatureErrors = effectiveSignature.getErrors()
        if (!signatureErrors.isEmpty()) {
            c.externalSignatureResolver.reportSignatureErrors(propertyDescriptor, signatureErrors)
        }

        propertyDescriptor.setType(effectiveSignature.getReturnType(), Collections.emptyList(), DescriptorUtils.getExpectedThisObjectIfNeeded(getContainingDeclaration()), null : JetType?)

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

        return JavaPropertyDescriptor(_containingDeclaration, annotations, visibility, isVar, propertyName,
                                      c.sourceElementFactory.source(field))
    }

    private fun getPropertyType(field: JavaField): JetType {
        // Fields do not have their own generic parameters
        val propertyType = c.typeResolver.transformJavaType(field.getType(), LazyJavaTypeAttributes(c, field, TypeUsage.MEMBER_SIGNATURE_INVARIANT))
        if (field.isFinal() && field.isStatic()) {
            return TypeUtils.makeNotNullable(propertyType)
        }
        return propertyType
    }

    override fun getProperties(name: Name): Collection<VariableDescriptor> = _properties(name)
    protected open fun getAllPropertyNames(): Collection<Name> = memberIndex().getAllFieldNames()

    override fun getLocalVariable(name: Name): VariableDescriptor? = null
    override fun getDeclarationsByLabel(labelName: Name) = listOf<DeclarationDescriptor>()

    override fun getOwnDeclaredDescriptors() = getAllDescriptors()
    override fun getAllDescriptors() = _allDescriptors()

    private fun computeAllDescriptors(): MutableCollection<DeclarationDescriptor> {
        val result = LinkedHashSet<DeclarationDescriptor>()

        for (name in getAllClassNames()) {
            val descriptor = getClassifier(name)
            if (descriptor != null) {
                // Null signifies that a class found in Java is not present in Kotlin (e.g. package class)
                result.add(descriptor)
            }
        }

        for (name in getAllFunctionNames()) {
            result.addAll(getFunctions(name))
        }

        for (name in getAllPropertyNames()) {
            result.addAll(getProperties(name))
        }

        addExtraDescriptors(result)

        return result
    }

    protected open fun addExtraDescriptors(result: MutableSet<DeclarationDescriptor>) {
        // Do nothing
    }

    protected abstract fun getAllClassNames(): Collection<Name>

    override fun toString() = "Lazy scope for ${getContainingDeclaration()}"
    
    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.println("containigDeclaration: ${getContainingDeclaration()}")

        p.popIndent()
        p.println("}")
    }
}
