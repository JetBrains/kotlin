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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.descriptors.JavaConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.child
import org.jetbrains.kotlin.load.java.lazy.resolveAnnotations
import org.jetbrains.kotlin.load.java.lazy.types.RawSubstitution
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.typeEnhacement.enhanceSignatures
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.util.*

public class LazyJavaClassMemberScope(
        c: LazyJavaResolverContext,
        containingDeclaration: ClassDescriptor,
        private val jClass: JavaClass
) : LazyJavaScope(c, containingDeclaration) {

    override fun computeMemberIndex(): MemberIndex {
        return object : ClassMemberIndex(jClass, { !it.isStatic() }) {
            // For SAM-constructors
            override fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name>
                    = super.getMethodNames(nameFilter) + getClassNames(DescriptorKindFilter.CLASSIFIERS, nameFilter)
        }
    }

    internal val constructors = c.storageManager.createLazyValue {
        val constructors = jClass.getConstructors()
        val result = ArrayList<JavaConstructorDescriptor>(constructors.size())
        for (constructor in constructors) {
            val descriptor = resolveConstructor(constructor)
            result.add(descriptor)
            result.addIfNotNull(c.components.samConversionResolver.resolveSamAdapter(descriptor))
        }
        
        enhanceSignatures(
                result ifEmpty { emptyOrSingletonList(createDefaultConstructor()) }
        ).toReadOnlyList()
    }

    override fun JavaMethodDescriptor.isVisibleAsFunction(): Boolean {
        val nameAsString = name.asString()

        if (JvmAbi.isGetterName(nameAsString)) {
            val propertyName = propertyNameByGetMethodName(name) ?: return true
            return !doesClassOverrideAnyProperty(getPropertiesFromSupertypes(propertyName))
        }

        if (JvmAbi.isSetterName(nameAsString)) {
            return propertyNamesBySetMethodName(name).none {
                propertyName ->
                getPropertiesFromSupertypes(propertyName).any {
                    property -> property.isVar && doesClassOverridesProperty(property)
                }
            }
        }

        if (name.isBuiltinSpecialPropertyName) {
            return !doesClassOverrideAnyProperty(getPropertiesFromSupertypes(name))
        }

        return true
    }

    private fun doesClassOverrideAnyProperty(properties: Collection<PropertyDescriptor>)
            = properties.any { property -> doesClassOverridesProperty(property) }

    private fun PropertyDescriptor.findGetterOverride(): JavaMethodDescriptor? {
        val getterName = getter?.builtinSpecialOverridden?.builtinSpecialPropertyAccessorName ?: JvmAbi.getterName(name.asString())
        return memberIndex().findMethodsByName(Name.identifier(getterName)).firstNotNullResult factory@{
            javaMethod ->
            val descriptor = resolveMethodToFunctionDescriptor(javaMethod)
            if (descriptor.valueParameters.size != 0) return@factory null

            descriptor.check { JetTypeChecker.DEFAULT.isSubtypeOf(descriptor.returnType ?: return@check false, type) }
        }
    }

    private fun PropertyDescriptor.findSetterOverride(): JavaMethodDescriptor? {
        return memberIndex().findMethodsByName(Name.identifier(JvmAbi.setterName(name.asString()))).firstNotNullResult factory@{
            javaMethod ->
            val descriptor = resolveMethodToFunctionDescriptor(javaMethod)
            if (descriptor.valueParameters.size != 1) return@factory null

            if (!KotlinBuiltIns.isUnit(descriptor.returnType ?: return@factory null)) return@factory null
            descriptor.check { JetTypeChecker.DEFAULT.equalTypes(descriptor.valueParameters.single().type, type) }
        }
    }

    private fun doesClassOverridesProperty(property: PropertyDescriptor): Boolean {
        if (property.isJavaField) return false
        val getter = property.findGetterOverride()
        val setter = property.findSetterOverride()

        if (getter == null) return false
        if (!property.isVar) return true

        return setter != null && setter.modality == getter.modality
    }

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        val functionsFromSupertypes = getFunctionsFromSupertypes(name)

        result.addAll(DescriptorResolverUtils.resolveOverrides(name, functionsFromSupertypes, result, getContainingDeclaration(), c.components.errorReporter))
    }

    private fun getFunctionsFromSupertypes(name: Name): Set<SimpleFunctionDescriptor> {
          return getContainingDeclaration().typeConstructor.supertypes.flatMap {
              it.memberScope.getFunctions(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS).map { f -> f as SimpleFunctionDescriptor }
          }.toSet()
      }

    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        if (jClass.isAnnotationType()) {
            computeAnnotationProperties(name, result)
        }

        val propertiesFromSupertypes = getPropertiesFromSupertypes(name)
        addPropertyOverrideByMethod(propertiesFromSupertypes, result)

        result.addAll(DescriptorResolverUtils.resolveOverrides(name, propertiesFromSupertypes, result, getContainingDeclaration(),
                                                                   c.components.errorReporter))
    }

    private fun addPropertyOverrideByMethod(
            propertiesFromSupertypes: Set<PropertyDescriptor>,
            result: MutableCollection<PropertyDescriptor>
    ) {
        for (property in propertiesFromSupertypes) {
            val newProperty = createPropertyDescriptorByMethods(property)
            if (newProperty != null) {
                result.add(newProperty)
                break
            }
        }
    }

    private fun computeAnnotationProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        val method = memberIndex().findMethodsByName(name).singleOrNull() ?: return
        result.add(createPropertyDescriptorWithDefaultGetter(method, modality = Modality.FINAL))
    }

    private fun createPropertyDescriptorWithDefaultGetter(
            method: JavaMethod, givenType: JetType? = null, modality: Modality
    ): JavaPropertyDescriptor {
        val annotations = c.resolveAnnotations(method)

        val propertyDescriptor = JavaPropertyDescriptor(
                getContainingDeclaration(), annotations, modality, method.getVisibility(),
                /* isVar = */ false, method.name, c.components.sourceElementFactory.source(method), /* original */ null,
                /* isStaticFinal = */ false
        )

        val getter = DescriptorFactory.createDefaultGetter(propertyDescriptor, Annotations.EMPTY)
        propertyDescriptor.initialize(getter, null)

        val returnType = givenType ?: computeMethodReturnType(method, annotations, c.child(propertyDescriptor, method))
        propertyDescriptor.setType(returnType, listOf(), getDispatchReceiverParameter(), null as JetType?)
        getter.initialize(returnType)

        return propertyDescriptor
    }

    private fun createPropertyDescriptorByMethods(
            overriddenProperty: PropertyDescriptor
    ): JavaPropertyDescriptor? {
        if (!doesClassOverridesProperty(overriddenProperty)) return null

        val getterMethod = overriddenProperty.findGetterOverride()!!
        val setterMethod =
                if (overriddenProperty.isVar)
                    overriddenProperty.findSetterOverride()!!
                else
                    null

        val propertyDescriptor = JavaPropertyDescriptor(
                getContainingDeclaration(), Annotations.EMPTY, getterMethod.modality, getterMethod.visibility,
                /* isVar = */ setterMethod != null, overriddenProperty.name, getterMethod.source,
                /* original */ null,
                /* isStaticFinal = */ false
        )

        propertyDescriptor.setType(getterMethod.returnType!!, listOf(), getDispatchReceiverParameter(), null as JetType?)

        val getter = DescriptorFactory.createGetter(
                propertyDescriptor, getterMethod.annotations, /* isDefault = */false, getterMethod.source
        ).apply {
            initialize(propertyDescriptor.type)
        }

        val setter = setterMethod?.let { setterMethod ->
            DescriptorFactory.createSetter(propertyDescriptor, setterMethod.annotations, /* isDefault = */false, setterMethod.visibility)
        }

        return propertyDescriptor.apply { initialize(getter, setter) }
    }

    private fun getPropertiesFromSupertypes(name: Name): Set<PropertyDescriptor> {
        return getContainingDeclaration().typeConstructor.supertypes.flatMap {
            it.memberScope.getProperties(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS).map { p -> p as PropertyDescriptor }
        }.toSet()
    }

    override fun resolveMethodSignature(
            method: JavaMethod, methodTypeParameters: List<TypeParameterDescriptor>, returnType: JetType,
            valueParameters: LazyJavaScope.ResolvedValueParameters
    ): LazyJavaScope.MethodSignatureData {
        val propagated = c.components.externalSignatureResolver.resolvePropagatedSignature(
                method, getContainingDeclaration(), returnType, null, valueParameters.descriptors, methodTypeParameters
        )
        val effectiveSignature = c.components.externalSignatureResolver.resolveAlternativeMethodSignature(
                method, !propagated.getSuperMethods().isEmpty(), propagated.getReturnType(),
                propagated.getReceiverType(), propagated.getValueParameters(), propagated.getTypeParameters(),
                propagated.hasStableParameterNames()
        )

        return LazyJavaScope.MethodSignatureData(effectiveSignature, propagated.getErrors() + effectiveSignature.getErrors())
    }

    override fun resolveMethodToFunctionDescriptor(method: JavaMethod): JavaMethodDescriptor {
        val commonResult = super.resolveMethodToFunctionDescriptor(method)

        if (commonResult.name.sameAsBuiltinMethodWithErasedValueParameters) {
            val erasedCopy = createOverrideForErasedIfNeeded(commonResult)
            if (erasedCopy != null) return erasedCopy
        }

        return commonResult
    }

    private fun createOverrideForErasedIfNeeded(
            methodDescriptor: JavaMethodDescriptor
    ): JavaMethodDescriptor? {

        val overriddenCandidates =
                getFunctionsFromSupertypes(methodDescriptor.name).map { it.overriddenBuiltinFunctionWithErasedValueParametersInJava }.filterNotNull()

        if (overriddenCandidates.isEmpty()) return null

        val overridden = firstOverriddenBuiltinFunctionWithErasedValueParameters(methodDescriptor, overriddenCandidates) ?: return null

        return methodDescriptor
                .enhance(
                        methodDescriptor.extensionReceiverParameter?.type,
                        overridden.valueParameters.map { it.type },
                        methodDescriptor.returnType!!)
                .copy(
                        methodDescriptor.containingDeclaration,
                        methodDescriptor.modality, methodDescriptor.visibility,
                        CallableMemberDescriptor.Kind.SYNTHESIZED, false) as? JavaMethodDescriptor
    }

    private fun firstOverriddenBuiltinFunctionWithErasedValueParameters(
            javaMethodDescriptor: JavaMethodDescriptor,
            candidatesToOverride: Collection<FunctionDescriptor>
    ): FunctionDescriptor? {
        return candidatesToOverride.firstNotNullResult overrides@{
            candidate ->
            candidate.check { javaMethodDescriptor.doesOverrideBuiltinFunctionWithErasedValueParameters(candidate) }
        }
    }

    private fun JavaMethodDescriptor.doesOverrideBuiltinFunctionWithErasedValueParameters(
            builtinWithErasedParameters: FunctionDescriptor
    ): Boolean {
        if (this.valueParameters.size() != builtinWithErasedParameters.valueParameters.size()) return false
        if (!this.typeParameters.isEmpty() || !builtinWithErasedParameters.typeParameters.isEmpty()) return false
        if (this.extensionReceiverParameter != null || builtinWithErasedParameters.extensionReceiverParameter != null) return false

        return this.valueParameters.indices.all {
            index ->
            val currentType = valueParameters[index].type
            val overriddenCandidate = RawSubstitution.eraseType(
                    builtinWithErasedParameters.original.valueParameters[index].type)
            JetTypeChecker.DEFAULT.equalTypes(currentType, overriddenCandidate)
        } && returnType.isSubtypeOf(builtinWithErasedParameters.returnType)
    }

    private fun JetType?.isSubtypeOf(other: JetType?): Boolean {
        return JetTypeChecker.DEFAULT.isSubtypeOf(this ?: return false, other ?: return false)
    }

    private fun resolveConstructor(constructor: JavaConstructor): JavaConstructorDescriptor {
        val classDescriptor = getContainingDeclaration()

        val constructorDescriptor = JavaConstructorDescriptor.createJavaConstructor(
                classDescriptor, c.resolveAnnotations(constructor), /* isPrimary = */ false, c.components.sourceElementFactory.source(constructor)
        )

        val valueParameters = resolveValueParameters(c, constructorDescriptor, constructor.getValueParameters())
        val effectiveSignature = c.components.externalSignatureResolver.resolveAlternativeMethodSignature(
                constructor, false, null, null, valueParameters.descriptors, Collections.emptyList(), false)

        constructorDescriptor.initialize(
                classDescriptor.getTypeConstructor().getParameters(),
                effectiveSignature.getValueParameters(),
                constructor.getVisibility()
        )
        constructorDescriptor.setHasStableParameterNames(effectiveSignature.hasStableParameterNames())
        constructorDescriptor.setHasSynthesizedParameterNames(valueParameters.hasSynthesizedNames)

        constructorDescriptor.setReturnType(classDescriptor.getDefaultType())

        val signatureErrors = effectiveSignature.getErrors()
        if (!signatureErrors.isEmpty()) {
            c.components.externalSignatureResolver.reportSignatureErrors(constructorDescriptor, signatureErrors)
        }

        c.components.javaResolverCache.recordConstructor(constructor, constructorDescriptor)

        return constructorDescriptor
    }

    private fun createDefaultConstructor(): ConstructorDescriptor? {
        val isAnnotation: Boolean = jClass.isAnnotationType()
        if (jClass.isInterface() && !isAnnotation)
            return null

        val classDescriptor = getContainingDeclaration()
        val constructorDescriptor = JavaConstructorDescriptor.createJavaConstructor(
                classDescriptor, Annotations.EMPTY, /* isPrimary = */ true, c.components.sourceElementFactory.source(jClass)
        )
        val typeParameters = classDescriptor.getTypeConstructor().getParameters()
        val valueParameters = if (isAnnotation) createAnnotationConstructorParameters(constructorDescriptor)
                              else Collections.emptyList<ValueParameterDescriptor>()
        constructorDescriptor.setHasSynthesizedParameterNames(false)

        constructorDescriptor.initialize(typeParameters, valueParameters, getConstructorVisibility(classDescriptor))
        constructorDescriptor.setHasStableParameterNames(true)
        constructorDescriptor.setReturnType(classDescriptor.getDefaultType())
        c.components.javaResolverCache.recordConstructor(jClass, constructorDescriptor);
        return constructorDescriptor
    }

    private fun getConstructorVisibility(classDescriptor: ClassDescriptor): Visibility {
        val visibility = classDescriptor.getVisibility()
        if (visibility == JavaVisibilities.PROTECTED_STATIC_VISIBILITY) {
            return JavaVisibilities.PROTECTED_AND_PACKAGE
        }
        return visibility
    }

    private fun createAnnotationConstructorParameters(constructor: ConstructorDescriptorImpl): List<ValueParameterDescriptor> {
        val methods = jClass.getMethods()
        val result = ArrayList<ValueParameterDescriptor>(methods.size())

        val attr = TypeUsage.MEMBER_SIGNATURE_INVARIANT.toAttributes(allowFlexible = false, isForAnnotationParameter = true)

        val (methodsNamedValue, otherMethods) = methods.
                partition { it.getName() == JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME }

        assert(methodsNamedValue.size() <= 1) { "There can't be more than one method named 'value' in annotation class: $jClass" }
        val methodNamedValue = methodsNamedValue.firstOrNull()
        if (methodNamedValue != null) {
            val parameterNamedValueJavaType = methodNamedValue.getReturnType()
            val (parameterType, varargType) =
                    if (parameterNamedValueJavaType is JavaArrayType)
                        Pair(c.typeResolver.transformArrayType(parameterNamedValueJavaType, attr, isVararg = true),
                             c.typeResolver.transformJavaType(parameterNamedValueJavaType.getComponentType(), attr))
                    else
                        Pair(c.typeResolver.transformJavaType(parameterNamedValueJavaType, attr), null)

            result.addAnnotationValueParameter(constructor, 0, methodNamedValue, parameterType, varargType)
        }

        val startIndex = if (methodNamedValue != null) 1 else 0
        for ((index, method) in otherMethods.withIndex()) {
            val parameterType = c.typeResolver.transformJavaType(method.getReturnType(), attr)
            result.addAnnotationValueParameter(constructor, index + startIndex, method, parameterType, null)
        }

        return result
    }

    private fun MutableList<ValueParameterDescriptor>.addAnnotationValueParameter(
            constructor: ConstructorDescriptor,
            index: Int,
            method: JavaMethod,
            returnType: JetType,
            varargElementType: JetType?
    ) {
        add(ValueParameterDescriptorImpl(
                constructor,
                null,
                index,
                Annotations.EMPTY,
                method.getName(),
                // Parameters of annotation constructors in Java are never nullable
                TypeUtils.makeNotNullable(returnType),
                method.hasAnnotationParameterDefaultValue(),
                // Nulls are not allowed in annotation arguments in Java
                varargElementType?.let { TypeUtils.makeNotNullable(it) },
                c.components.sourceElementFactory.source(method)
        ))
    }

    private val nestedClassIndex = c.storageManager.createLazyValue {
        jClass.getInnerClasses().valuesToMap { c -> c.getName() }
    }

    private val enumEntryIndex = c.storageManager.createLazyValue {
        jClass.getFields().filter { it.isEnumEntry() }.valuesToMap { f -> f.getName() }
    }

    private val nestedClasses = c.storageManager.createMemoizedFunctionWithNullableValues {
        name: Name ->
        val jNestedClass = nestedClassIndex()[name]
        if (jNestedClass == null) {
            val field = enumEntryIndex()[name]
            if (field != null) {
                EnumEntrySyntheticClassDescriptor.create(c.storageManager, getContainingDeclaration(), name,
                                                         c.storageManager.createLazyValue {
                                                             memberIndex().getAllFieldNames() + memberIndex().getMethodNames({true})
                                                         }, c.components.sourceElementFactory.source(field))
            }
            else null
        }
        else {
            LazyJavaClassDescriptor(
                    c, getContainingDeclaration(), DescriptorUtils.getFqName(getContainingDeclaration()).child(name).toSafe(), jNestedClass
            )
        }
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? =
            DescriptorUtils.getDispatchReceiverParameterIfNeeded(getContainingDeclaration())

    override fun getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = nestedClasses(name)

    override fun getClassNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name>
            = nestedClassIndex().keySet() + enumEntryIndex().keySet()

    override fun getPropertyNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name> {
        if (jClass.isAnnotationType()) return memberIndex().getMethodNames(nameFilter)

        return memberIndex().getAllFieldNames() +
        getContainingDeclaration().getTypeConstructor().getSupertypes().flatMapTo(LinkedHashSet<Name>()) { supertype ->
            supertype.getMemberScope().getDescriptors(kindFilter, nameFilter).map { variable ->
                variable.getName()
            }
        }
    }

    // TODO
    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = listOf()


    override fun getContainingDeclaration() = super.getContainingDeclaration() as ClassDescriptor

    // namespaces should be resolved elsewhere
    override fun getPackage(name: Name) = null

    override fun toString() = "Lazy java member scope for " + jClass.getFqName()
}
