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
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithDifferentJvmName.isRemoveAtByIndex
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithDifferentJvmName.sameAsRenamedInJvmBuiltin
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.sameAsBuiltinMethodWithErasedValueParameters
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties.getBuiltinSpecialPropertyGetterName
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils.resolveOverridesForNonStaticMembers
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.descriptors.JavaConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.load.java.descriptors.copyValueParameters
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.child
import org.jetbrains.kotlin.load.java.lazy.resolveAnnotations
import org.jetbrains.kotlin.load.java.lazy.types.RawSubstitution
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.load.java.structure.JavaArrayType
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaConstructor
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.load.java.typeEnhancement.enhanceSignatures
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.util.*

class LazyJavaClassMemberScope(
        c: LazyJavaResolverContext,
        override val ownerDescriptor: ClassDescriptor,
        private val jClass: JavaClass
) : LazyJavaScope(c) {

    override fun computeMemberIndex(): MemberIndex {
        return object : ClassMemberIndex(jClass, { !it.isStatic }) {
            // For SAM-constructors
            override fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name>
                    = super.getMethodNames(nameFilter) + getClassNames(DescriptorKindFilter.CLASSIFIERS, nameFilter)
        }
    }

    internal val constructors = c.storageManager.createLazyValue {
        val constructors = jClass.getConstructors()
        val result = ArrayList<JavaConstructorDescriptor>(constructors.size)
        for (constructor in constructors) {
            val descriptor = resolveConstructor(constructor)
            result.add(descriptor)
            result.addIfNotNull(c.components.samConversionResolver.resolveSamAdapter(descriptor))
        }
        
        enhanceSignatures(
                result.ifEmpty { emptyOrSingletonList(createDefaultConstructor()) }
        ).toReadOnlyList()
    }

    override fun JavaMethodDescriptor.isVisibleAsFunction(): Boolean {
        if (jClass.isAnnotationType) return false
        return isVisibleAsFunctionInCurrentClass(this)
    }

    private fun isVisibleAsFunctionInCurrentClass(function: SimpleFunctionDescriptor): Boolean {
        if (getPropertyNamesCandidatesByAccessorName(function.name).any {
            propertyName ->
            getPropertiesFromSupertypes(propertyName).any {
                property ->
                doesClassOverridesProperty(property) {
                    accessorName ->
                    // This lambda should return property accessors available in this class by their name
                    // If 'accessorName' is current function we return only it just because we check exactly
                    // that current method is override of accessor
                    if (function.name == accessorName)
                        listOf(function)
                    else
                        searchMethodsByNameWithoutBuiltinMagic(accessorName) + searchMethodsInSupertypesWithoutBuiltinMagic(accessorName)
                } && (property.isVar || !JvmAbi.isSetterName(function.name.asString()))
            }
        }) return false

        if (function.doesOverrideRenamedBuiltins()) {
            return false
        }

        if (function.name.sameAsBuiltinMethodWithErasedValueParameters && hasOverriddenBuiltinFunctionWithErasedValueParameters(function)) {
            return false
        }

        return true
    }

    private fun searchMethodsByNameWithoutBuiltinMagic(name: Name): Collection<SimpleFunctionDescriptor> =
            memberIndex().findMethodsByName(name).map { resolveMethodToFunctionDescriptor(it) }

    private fun searchMethodsInSupertypesWithoutBuiltinMagic(name: Name): Collection<SimpleFunctionDescriptor> =
            getFunctionsFromSupertypes(name).filterNot {
                it.doesOverrideBuiltinWithDifferentJvmName()
                        || BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(it) != null
            }

    private fun SimpleFunctionDescriptor.doesOverrideRenamedBuiltins(): Boolean {
        return BuiltinMethodsWithDifferentJvmName.getBuiltinFunctionNamesByJvmName(name).any {
            builtinName ->
            val builtinSpecialFromSuperTypes =
                    getFunctionsFromSupertypes(builtinName).filter { it.doesOverrideBuiltinWithDifferentJvmName() }
            if (builtinSpecialFromSuperTypes.isEmpty()) return@any false

            val methodDescriptor = this.createRenamedCopy(builtinName)

            builtinSpecialFromSuperTypes.any { isOverridableRenamedDescriptor(it, methodDescriptor) }
        }
    }

    private fun isOverridableRenamedDescriptor(superDescriptor: FunctionDescriptor, subDescriptor: FunctionDescriptor): Boolean {
        // if we check 'removeAt', get original sub-descriptor to distinct `remove(int)` and `remove(E)` in Java
        val subDescriptorToCheck = if (superDescriptor.isRemoveAtByIndex) subDescriptor.original else subDescriptor

        return subDescriptorToCheck.doesOverride(superDescriptor)
    }

    private fun CallableDescriptor.doesOverride(superDescriptor: CallableDescriptor): Boolean {
        return OverridingUtil.DEFAULT.isOverridableByWithoutExternalConditions(
                superDescriptor, this, /* checkReturnType = */ true
        ).result == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
    }

    private fun PropertyDescriptor.findGetterOverride(
            functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        val overriddenBuiltinProperty = getter?.getOverriddenBuiltinWithDifferentJvmName()
        val specialGetterName = overriddenBuiltinProperty?.getBuiltinSpecialPropertyGetterName()
        if (specialGetterName != null
                && !this@LazyJavaClassMemberScope.ownerDescriptor.hasRealKotlinSuperClassWithOverrideOf(
                overriddenBuiltinProperty!!)
        ) {
            return findGetterByName(specialGetterName, functions)
        }

        return findGetterByName(JvmAbi.getterName(name.asString()), functions)
    }

    private fun PropertyDescriptor.findGetterByName(
            getterName: String,
            functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        return functions(Name.identifier(getterName)).firstNotNullResult factory@{
            descriptor ->
            if (descriptor.valueParameters.size != 0) return@factory null

            descriptor.check { KotlinTypeChecker.DEFAULT.isSubtypeOf(descriptor.returnType ?: return@check false, type) }
        }
    }

    private fun PropertyDescriptor.findSetterOverride(
            functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        return functions(Name.identifier(JvmAbi.setterName(name.asString()))).firstNotNullResult factory@{
            descriptor ->
            if (descriptor.valueParameters.size != 1) return@factory null

            if (!KotlinBuiltIns.isUnit(descriptor.returnType ?: return@factory null)) return@factory null
            descriptor.check { KotlinTypeChecker.DEFAULT.equalTypes(descriptor.valueParameters.single().type, type) }
        }
    }

    private fun doesClassOverridesProperty(
            property: PropertyDescriptor,
            functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): Boolean {
        if (property.isJavaField) return false
        val getter = property.findGetterOverride(functions)
        val setter = property.findSetterOverride(functions)

        if (getter == null) return false
        if (!property.isVar) return true

        return setter != null && setter.modality == getter.modality
    }

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        val functionsFromSupertypes = getFunctionsFromSupertypes(name)

        if (!name.sameAsRenamedInJvmBuiltin && !name.sameAsBuiltinMethodWithErasedValueParameters) {
            // Simple fast path in case of name is not suspicious (i.e. name is not one of builtins that have different signature in Java)
            addFunctionFromSupertypes(
                    result, name,
                    functionsFromSupertypes.filter { isVisibleAsFunctionInCurrentClass(it) },
                    isSpecialBuiltinName = false)
            return
        }

        var specialBuiltinsFromSuperTypes = SmartSet.create<SimpleFunctionDescriptor>()

        // Merge functions with same signatures
        val mergedFunctionFromSuperTypes = resolveOverridesForNonStaticMembers(
                name, functionsFromSupertypes, emptyList(), ownerDescriptor, ErrorReporter.DO_NOTHING)

        // add declarations
        addOverriddenBuiltinMethods(name, result, mergedFunctionFromSuperTypes, result) {
            searchMethodsByNameWithoutBuiltinMagic(it)
        }

        // add from super types
        addOverriddenBuiltinMethods(name, result, mergedFunctionFromSuperTypes, specialBuiltinsFromSuperTypes) {
            searchMethodsInSupertypesWithoutBuiltinMagic(it)
        }

        val visibleFunctionsFromSupertypes =
                functionsFromSupertypes.filter { isVisibleAsFunctionInCurrentClass(it) } + specialBuiltinsFromSuperTypes

        addFunctionFromSupertypes(result, name, visibleFunctionsFromSupertypes, isSpecialBuiltinName = true)
    }

    private fun addFunctionFromSupertypes(
            result: MutableCollection<SimpleFunctionDescriptor>,
            name: Name,
            functionsFromSupertypes: Collection<SimpleFunctionDescriptor>,
            isSpecialBuiltinName: Boolean
    ) {

        val additionalOverrides = resolveOverridesForNonStaticMembers(
                name, functionsFromSupertypes, result, ownerDescriptor, c.components.errorReporter
        )

        if (!isSpecialBuiltinName) {
            result.addAll(additionalOverrides)
        }
        else {
            val allDescriptors = result + additionalOverrides
            result.addAll(
                    additionalOverrides.map {
                        resolvedOverride ->
                        val overriddenBuiltin = resolvedOverride.getOverriddenSpecialBuiltin()
                                                ?: return@map resolvedOverride

                        resolvedOverride.createHiddenCopyIfBuiltinAlreadyAccidentallyOverridden(overriddenBuiltin, allDescriptors)
                    })
        }
    }

    private fun addOverriddenBuiltinMethods(
            name: Name,
            alreadyDeclaredFunctions: Collection<SimpleFunctionDescriptor>,
            candidatesForOverride: Collection<SimpleFunctionDescriptor>,
            result: MutableCollection<SimpleFunctionDescriptor>,
            functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ) {
        for (descriptor in candidatesForOverride) {
            val overriddenBuiltin = descriptor.getOverriddenBuiltinWithDifferentJvmName() ?: continue

            val nameInJava = getJvmMethodNameIfSpecial(overriddenBuiltin)!!
            for (method in functions(Name.identifier(nameInJava))) {
                val renamedCopy = method.createRenamedCopy(name)

                if (isOverridableRenamedDescriptor(overriddenBuiltin, renamedCopy)) {
                    result.add(
                            renamedCopy.createHiddenCopyIfBuiltinAlreadyAccidentallyOverridden(overriddenBuiltin, alreadyDeclaredFunctions))
                    break
                }
            }
        }

        for (descriptor in candidatesForOverride) {
            val overriddenBuiltin =
                    BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(descriptor)
                    ?: continue

            createOverrideForBuiltinFunctionWithErasedParameterIfNeeded(overriddenBuiltin, functions)?.let {
                override ->
                if (isVisibleAsFunctionInCurrentClass(override)) {
                    result.add(override.createHiddenCopyIfBuiltinAlreadyAccidentallyOverridden(overriddenBuiltin, alreadyDeclaredFunctions))
                }
            }
        }
    }

    // In case when Java has declaration with signature reflecting one of special builtin we load override of builtin as hidden function
    // Unless we do it then signature clash happens.
    // For example see java.nio.CharBuffer implementing CharSequence and defining irrelevant 'get' method having the same signature as in kotlin.CharSequence
    // We load java.nio.CharBuffer as having both 'get' functions, but one that is override of kotlin.CharSequence is hidden,
    // so when someone calls CharBuffer.get it results in invoking java method CharBuffer.get
    // But we still have the way to call 'charAt' java method by upcasting CharBuffer to kotlin.CharSequence
    private fun SimpleFunctionDescriptor.createHiddenCopyIfBuiltinAlreadyAccidentallyOverridden(
            specialBuiltin: CallableDescriptor,
            alreadyDeclaredFunctions: Collection<SimpleFunctionDescriptor>
    ) = if (alreadyDeclaredFunctions.none { this != it && it.doesOverride(specialBuiltin) })
            this
        else
            createHiddenCopyToOvercomeSignatureClash()

    private fun createOverrideForBuiltinFunctionWithErasedParameterIfNeeded(
            overridden: FunctionDescriptor,
            functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        return functions(overridden.name).firstOrNull {
            it.doesOverrideBuiltinFunctionWithErasedValueParameters(overridden)
        }?.let {
            override ->
            override.createCopyWithNewValueParameters(
                    copyValueParameters(overridden.valueParameters.map { it.type }, override.valueParameters, overridden))

        }
    }

    private fun getFunctionsFromSupertypes(name: Name): Set<SimpleFunctionDescriptor> {
          return ownerDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
              it.memberScope.getContributedFunctions(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS)
          }
      }

    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        if (jClass.isAnnotationType) {
            computeAnnotationProperties(name, result)
        }

        val propertiesFromSupertypes = getPropertiesFromSupertypes(name)
        if (propertiesFromSupertypes.isEmpty()) return

        val propertiesOverridesFromSuperTypes = SmartSet.create<PropertyDescriptor>()

        addPropertyOverrideByMethod(propertiesFromSupertypes, result) { searchMethodsByNameWithoutBuiltinMagic(it) }

        addPropertyOverrideByMethod(propertiesFromSupertypes, propertiesOverridesFromSuperTypes) {
            searchMethodsInSupertypesWithoutBuiltinMagic(it)
        }

        result.addAll(resolveOverridesForNonStaticMembers(
                name, propertiesFromSupertypes + propertiesOverridesFromSuperTypes, result, ownerDescriptor, c.components.errorReporter))
    }

    private fun addPropertyOverrideByMethod(
            propertiesFromSupertypes: Set<PropertyDescriptor>,
            result: MutableCollection<PropertyDescriptor>,
            functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ) {
        for (property in propertiesFromSupertypes) {
            val newProperty = createPropertyDescriptorByMethods(property, functions)
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
            method: JavaMethod, givenType: KotlinType? = null, modality: Modality
    ): JavaPropertyDescriptor {
        val annotations = c.resolveAnnotations(method)

        val propertyDescriptor = JavaPropertyDescriptor.create(
                ownerDescriptor, annotations, modality, method.visibility,
                /* isVar = */ false, method.name, c.components.sourceElementFactory.source(method),
                /* isStaticFinal = */ false
        )

        val getter = DescriptorFactory.createDefaultGetter(propertyDescriptor, Annotations.EMPTY)
        propertyDescriptor.initialize(getter, null)

        val returnType = givenType ?: computeMethodReturnType(method, annotations, c.child(propertyDescriptor, method))
        propertyDescriptor.setType(returnType, listOf(), getDispatchReceiverParameter(), null as KotlinType?)
        getter.initialize(returnType)

        return propertyDescriptor
    }

    private fun createPropertyDescriptorByMethods(
            overriddenProperty: PropertyDescriptor,
            functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): JavaPropertyDescriptor? {
        if (!doesClassOverridesProperty(overriddenProperty, functions)) return null

        val getterMethod = overriddenProperty.findGetterOverride(functions)!!
        val setterMethod =
                if (overriddenProperty.isVar)
                    overriddenProperty.findSetterOverride(functions)!!
                else
                    null

        assert(setterMethod?.let { it.modality == getterMethod.modality } ?: true) {
            "Different accessors modalities when creating overrides for $overriddenProperty in ${ownerDescriptor}" +
            "for getter is ${getterMethod.modality}, but for setter is ${setterMethod?.modality}"
        }

        val propertyDescriptor = JavaPropertyDescriptor.create(
                ownerDescriptor, Annotations.EMPTY, getterMethod.modality, getterMethod.visibility,
                /* isVar = */ setterMethod != null, overriddenProperty.name, getterMethod.source,
                /* isStaticFinal = */ false
        )

        propertyDescriptor.setType(getterMethod.returnType!!, listOf(), getDispatchReceiverParameter(), null as KotlinType?)

        val getter = DescriptorFactory.createGetter(
                propertyDescriptor, getterMethod.annotations, /* isDefault = */false,
                /* isExternal = */ false, getterMethod.source
        ).apply {
            initialSignatureDescriptor = getterMethod
            initialize(propertyDescriptor.type)
        }

        val setter = setterMethod?.let { setterMethod ->
            DescriptorFactory.createSetter(propertyDescriptor, setterMethod.annotations, /* isDefault = */false,
            /* isExternal = */ false, setterMethod.visibility, setterMethod.source
            ).apply {
                initialSignatureDescriptor = setterMethod
            }
        }

        return propertyDescriptor.apply { initialize(getter, setter) }
    }

    private fun getPropertiesFromSupertypes(name: Name): Set<PropertyDescriptor> {
        return ownerDescriptor.typeConstructor.supertypes.flatMap {
            it.memberScope.getContributedVariables(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS).map { p -> p }
        }.toSet()
    }

    override fun resolveMethodSignature(
            method: JavaMethod, methodTypeParameters: List<TypeParameterDescriptor>, returnType: KotlinType,
            valueParameters: List<ValueParameterDescriptor>
    ): LazyJavaScope.MethodSignatureData {
        val propagated = c.components.signaturePropagator.resolvePropagatedSignature(
                method, ownerDescriptor, returnType, null, valueParameters, methodTypeParameters
        )
        return LazyJavaScope.MethodSignatureData(
                propagated.returnType, propagated.receiverType, propagated.valueParameters, propagated.typeParameters,
                propagated.hasStableParameterNames(), propagated.errors
        )
    }

    private fun hasOverriddenBuiltinFunctionWithErasedValueParameters(
            simpleFunctionDescriptor: SimpleFunctionDescriptor
    ): Boolean {
        val candidatesToOverride =
                getFunctionsFromSupertypes(simpleFunctionDescriptor.name).mapNotNull {
                    BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(it)
                }

        return candidatesToOverride.any {
            candidate ->
            simpleFunctionDescriptor.doesOverrideBuiltinFunctionWithErasedValueParameters(candidate)
        }
    }

    private fun SimpleFunctionDescriptor.doesOverrideBuiltinFunctionWithErasedValueParameters(
            builtinWithErasedParameters: FunctionDescriptor
    ): Boolean {
        if (this.valueParameters.size != builtinWithErasedParameters.valueParameters.size) return false
        if (!this.typeParameters.isEmpty() || !builtinWithErasedParameters.typeParameters.isEmpty()) return false
        if (this.extensionReceiverParameter != null || builtinWithErasedParameters.extensionReceiverParameter != null) return false

        return this.valueParameters.indices.all {
            index ->
            val currentType = valueParameters[index].type
            val overriddenCandidate = RawSubstitution.eraseType(
                    builtinWithErasedParameters.original.valueParameters[index].type)
            KotlinTypeChecker.DEFAULT.equalTypes(currentType, overriddenCandidate)
        } && returnType.isSubtypeOf(builtinWithErasedParameters.returnType)
    }

    private fun KotlinType?.isSubtypeOf(other: KotlinType?): Boolean {
        return KotlinTypeChecker.DEFAULT.isSubtypeOf(this ?: return false, other ?: return false)
    }

    private fun resolveConstructor(constructor: JavaConstructor): JavaConstructorDescriptor {
        val classDescriptor = ownerDescriptor

        val constructorDescriptor = JavaConstructorDescriptor.createJavaConstructor(
                classDescriptor, c.resolveAnnotations(constructor), /* isPrimary = */ false, c.components.sourceElementFactory.source(constructor)
        )


        val c = c.child(constructorDescriptor, constructor, typeParametersIndexOffset = classDescriptor.declaredTypeParameters.size)
        val valueParameters = resolveValueParameters(c, constructorDescriptor, constructor.valueParameters)
        val constructorTypeParameters =
                classDescriptor.declaredTypeParameters +
                constructor.typeParameters.map { p -> c.typeParameterResolver.resolveTypeParameter(p)!! }

        constructorDescriptor.initialize(valueParameters.descriptors, constructor.visibility, constructorTypeParameters)
        constructorDescriptor.setHasStableParameterNames(false)
        constructorDescriptor.setHasSynthesizedParameterNames(valueParameters.hasSynthesizedNames)

        constructorDescriptor.returnType = classDescriptor.defaultType

        c.components.javaResolverCache.recordConstructor(constructor, constructorDescriptor)

        return constructorDescriptor
    }

    private fun createDefaultConstructor(): ConstructorDescriptor? {
        val isAnnotation: Boolean = jClass.isAnnotationType
        if (jClass.isInterface && !isAnnotation)
            return null

        val classDescriptor = ownerDescriptor
        val constructorDescriptor = JavaConstructorDescriptor.createJavaConstructor(
                classDescriptor, Annotations.EMPTY, /* isPrimary = */ true, c.components.sourceElementFactory.source(jClass)
        )
        val valueParameters = if (isAnnotation) createAnnotationConstructorParameters(constructorDescriptor)
                              else Collections.emptyList<ValueParameterDescriptor>()
        constructorDescriptor.setHasSynthesizedParameterNames(false)

        constructorDescriptor.initialize(valueParameters, getConstructorVisibility(classDescriptor))
        constructorDescriptor.setHasStableParameterNames(true)
        constructorDescriptor.returnType = classDescriptor.defaultType
        c.components.javaResolverCache.recordConstructor(jClass, constructorDescriptor);
        return constructorDescriptor
    }

    private fun getConstructorVisibility(classDescriptor: ClassDescriptor): Visibility {
        val visibility = classDescriptor.visibility
        if (visibility == JavaVisibilities.PROTECTED_STATIC_VISIBILITY) {
            return JavaVisibilities.PROTECTED_AND_PACKAGE
        }
        return visibility
    }

    private fun createAnnotationConstructorParameters(constructor: ConstructorDescriptorImpl): List<ValueParameterDescriptor> {
        val methods = jClass.methods
        val result = ArrayList<ValueParameterDescriptor>(methods.size)

        val attr = TypeUsage.MEMBER_SIGNATURE_INVARIANT.toAttributes(allowFlexible = false, isForAnnotationParameter = true)

        val (methodsNamedValue, otherMethods) = methods.
                partition { it.name == JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME }

        assert(methodsNamedValue.size <= 1) { "There can't be more than one method named 'value' in annotation class: $jClass" }
        val methodNamedValue = methodsNamedValue.firstOrNull()
        if (methodNamedValue != null) {
            val parameterNamedValueJavaType = methodNamedValue.returnType
            val (parameterType, varargType) =
                    if (parameterNamedValueJavaType is JavaArrayType)
                        Pair(c.typeResolver.transformArrayType(parameterNamedValueJavaType, attr, isVararg = true),
                             c.typeResolver.transformJavaType(parameterNamedValueJavaType.componentType, attr))
                    else
                        Pair(c.typeResolver.transformJavaType(parameterNamedValueJavaType, attr), null)

            result.addAnnotationValueParameter(constructor, 0, methodNamedValue, parameterType, varargType)
        }

        val startIndex = if (methodNamedValue != null) 1 else 0
        for ((index, method) in otherMethods.withIndex()) {
            val parameterType = c.typeResolver.transformJavaType(method.returnType, attr)
            result.addAnnotationValueParameter(constructor, index + startIndex, method, parameterType, null)
        }

        return result
    }

    private fun MutableList<ValueParameterDescriptor>.addAnnotationValueParameter(
            constructor: ConstructorDescriptor,
            index: Int,
            method: JavaMethod,
            returnType: KotlinType,
            varargElementType: KotlinType?
    ) {
        add(ValueParameterDescriptorImpl(
                constructor,
                null,
                index,
                Annotations.EMPTY,
                method.name,
                // Parameters of annotation constructors in Java are never nullable
                TypeUtils.makeNotNullable(returnType),
                method.hasAnnotationParameterDefaultValue(),
                /* isCrossinline = */ false,
                /* isNoinline = */ false,
                // Nulls are not allowed in annotation arguments in Java
                varargElementType?.let { TypeUtils.makeNotNullable(it) },
                c.components.sourceElementFactory.source(method)
        ))
    }

    private val nestedClassIndex = c.storageManager.createLazyValue {
        jClass.innerClasses.associateBy { c -> c.name }
    }

    private val enumEntryIndex = c.storageManager.createLazyValue {
        jClass.fields.filter { it.isEnumEntry }.associateBy { f -> f.name }
    }

    private val nestedClasses = c.storageManager.createMemoizedFunctionWithNullableValues {
        name: Name ->
        val jNestedClass = nestedClassIndex()[name]
        if (jNestedClass == null) {
            val field = enumEntryIndex()[name]
            if (field != null) {
                val createLazyValue: NotNullLazyValue<Collection<Name>> = c.storageManager.createLazyValue {
                    memberIndex().getAllFieldNames() + memberIndex().getMethodNames({ true })
                }
                EnumEntrySyntheticClassDescriptor.create(c.storageManager, ownerDescriptor, name,
                                                         createLazyValue, c.resolveAnnotations(field), c.components.sourceElementFactory.source(field))
            }
            else null
        }
        else {
            LazyJavaClassDescriptor(
                    c, ownerDescriptor, DescriptorUtils.getFqName(ownerDescriptor).child(name).toSafe(), jNestedClass
            )
        }
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? =
            DescriptorUtils.getDispatchReceiverParameterIfNeeded(ownerDescriptor)

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        recordLookup(name, location)
        return nestedClasses(name)
    }

    override fun getClassNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name>
            = nestedClassIndex().keys + enumEntryIndex().keys

    override fun getPropertyNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name> {
        if (jClass.isAnnotationType()) return memberIndex().getMethodNames(nameFilter)

        return memberIndex().getAllFieldNames() +
               ownerDescriptor.getTypeConstructor().getSupertypes().flatMapTo(LinkedHashSet<Name>()) { supertype ->
            supertype.getMemberScope().getContributedDescriptors(kindFilter, nameFilter).map { variable ->
                variable.getName()
            }
        }
    }

    override fun toString() = "Lazy java member scope for " + jClass.fqName
}
