/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.StandardNames.CONTINUATION_INTERFACE_FQ_NAME
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithDifferentJvmName.isRemoveAtByIndex
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.sameAsBuiltinMethodWithErasedValueParameters
import org.jetbrains.kotlin.load.java.ClassicBuiltinSpecialProperties.getBuiltinSpecialPropertyGetterName
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.sameAsRenamedInJvmBuiltin
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils.resolveOverridesForNonStaticMembers
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.load.java.descriptors.*
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.childForMethod
import org.jetbrains.kotlin.load.java.lazy.resolveAnnotations
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeRefinement
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.*

class LazyJavaClassMemberScope(
    c: LazyJavaResolverContext,
    override val ownerDescriptor: ClassDescriptor,
    private val jClass: JavaClass,
    private val skipRefinement: Boolean,
    mainScope: LazyJavaClassMemberScope? = null
) : LazyJavaScope(c, mainScope) {

    override fun computeMemberIndex() = ClassDeclaredMemberIndex(jClass) { !it.isStatic }

    override fun computeFunctionNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?) =
        ownerDescriptor.typeConstructor.supertypes.flatMapTo(linkedSetOf()) {
            it.memberScope.getFunctionNames()
        }.apply {
            addAll(declaredMemberIndex().getMethodNames())
            addAll(declaredMemberIndex().getRecordComponentNames())
            addAll(computeClassNames(kindFilter, nameFilter))
            addAll(c.components.syntheticPartsProvider.getMethodNames(ownerDescriptor))
        }

    internal val constructors = c.storageManager.createLazyValue {
        val constructors = jClass.constructors
        val result = ArrayList<ClassConstructorDescriptor>(constructors.size)
        for (constructor in constructors) {
            val descriptor = resolveConstructor(constructor)
            result.add(descriptor)
        }

        if (jClass.isRecord) {
            val defaultConstructor = createDefaultRecordConstructor()
            val jvmDescriptor = defaultConstructor.computeJvmDescriptor(withReturnType = false)

            if (result.none { it.computeJvmDescriptor(withReturnType = false) == jvmDescriptor }) {
                result.add(defaultConstructor)
                c.components.javaResolverCache.recordConstructor(jClass, defaultConstructor)
            }
        }

        c.components.syntheticPartsProvider.generateConstructors(ownerDescriptor, result)

        c.components.signatureEnhancement.enhanceSignatures(
            c,
            result.ifEmpty { listOfNotNull(createDefaultConstructor()) }
        ).toList()
    }

    private fun createDefaultRecordConstructor(): ClassConstructorDescriptor {
        val classDescriptor = ownerDescriptor
        val constructorDescriptor = JavaClassConstructorDescriptor.createJavaConstructor(
            classDescriptor, Annotations.EMPTY, /* isPrimary = */ true, c.components.sourceElementFactory.source(jClass)
        )
        val valueParameters = createRecordConstructorParameters(constructorDescriptor)
        constructorDescriptor.setHasSynthesizedParameterNames(false)

        constructorDescriptor.initialize(valueParameters, getConstructorVisibility(classDescriptor))
        constructorDescriptor.setHasStableParameterNames(false)
        constructorDescriptor.returnType = classDescriptor.defaultType
        return constructorDescriptor
    }

    private fun createRecordConstructorParameters(constructor: ClassConstructorDescriptorImpl): List<ValueParameterDescriptor> {
        val components = jClass.recordComponents
        val result = ArrayList<ValueParameterDescriptor>(components.size)

        val attr = TypeUsage.COMMON.toAttributes(isForAnnotationParameter = false)

        for ((index, component) in components.withIndex()) {
            val parameterType = c.typeResolver.transformJavaType(component.type, attr)
            val varargElementType =
                if (component.isVararg) c.components.module.builtIns.getArrayElementType(parameterType) else null

            result.add(
                ValueParameterDescriptorImpl(
                    constructor,
                    null,
                    index,
                    Annotations.EMPTY,
                    component.name,
                    parameterType,
                    /* deeclaresDefaultValue = */false,
                    /* isCrossinline = */ false,
                    /* isNoinline = */ false,
                    varargElementType,
                    c.components.sourceElementFactory.source(component)
                )
            )
        }

        return result
    }

    override fun JavaMethodDescriptor.isVisibleAsFunction(): Boolean {
        if (jClass.isAnnotationType) return false
        return isVisibleAsFunctionInCurrentClass(this)
    }

    private fun isVisibleAsFunctionInCurrentClass(function: SimpleFunctionDescriptor): Boolean {
        if (getPropertyNamesCandidatesByAccessorName(function.name).any { propertyName ->
                getPropertiesFromSupertypes(propertyName).any { property ->
                    doesClassOverridesProperty(property) { accessorName ->
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

        return !function.doesOverrideRenamedBuiltins() &&
                !function.shouldBeVisibleAsOverrideOfBuiltInWithErasedValueParameters() &&
                !function.doesOverrideSuspendFunction()
    }

    /**
     * Checks if function is a valid override of JDK analogue of built-in method with erased value parameters (e.g. Map.containsKey(k: K))
     *
     * Examples:
     * - boolean containsKey(Object key) -> true
     * - boolean containsKey(K key) -> false // Wrong JDK method override, while it's a valid Kotlin built-in override
     */
    private fun SimpleFunctionDescriptor.shouldBeVisibleAsOverrideOfBuiltInWithErasedValueParameters(): Boolean {
        if (!name.sameAsBuiltinMethodWithErasedValueParameters) return false
        val candidatesToOverride =
            getFunctionsFromSupertypes(name).mapNotNull {
                BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(it)
            }

        return candidatesToOverride.any { candidate ->
            hasSameJvmDescriptorButDoesNotOverride(candidate)
        }
    }

    private fun searchMethodsByNameWithoutBuiltinMagic(name: Name): Collection<SimpleFunctionDescriptor> =
        declaredMemberIndex().findMethodsByName(name).map { resolveMethodToFunctionDescriptor(it) }

    private fun searchMethodsInSupertypesWithoutBuiltinMagic(name: Name): Collection<SimpleFunctionDescriptor> =
        getFunctionsFromSupertypes(name).filterNot {
            it.doesOverrideBuiltinWithDifferentJvmName()
                    || BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(it) != null
        }

    private fun SimpleFunctionDescriptor.doesOverrideRenamedBuiltins(): Boolean {
        return SpecialGenericSignatures.getBuiltinFunctionNamesByJvmName(name).any {
            // e.g. 'removeAt' or 'toInt'
                builtinName ->
            val builtinSpecialFromSuperTypes =
                getFunctionsFromSupertypes(builtinName).filter { it.doesOverrideBuiltinWithDifferentJvmName() }
            if (builtinSpecialFromSuperTypes.isEmpty()) return@any false

            val methodDescriptor = this.createRenamedCopy(builtinName)

            builtinSpecialFromSuperTypes.any { doesOverrideRenamedDescriptor(it, methodDescriptor) }
        }
    }

    private fun SimpleFunctionDescriptor.doesOverrideSuspendFunction(): Boolean {
        val suspendView = this.createSuspendView() ?: return false

        return getFunctionsFromSupertypes(name).any { overriddenCandidate ->
            overriddenCandidate.isSuspend && suspendView.doesOverride(overriddenCandidate)
        }
    }

    private fun SimpleFunctionDescriptor.createSuspendView(): SimpleFunctionDescriptor? {
        val continuationParameter = valueParameters.lastOrNull()?.takeIf {
            it.type.constructor.declarationDescriptor?.fqNameUnsafe?.takeIf(FqNameUnsafe::isSafe)
                ?.toSafe() == CONTINUATION_INTERFACE_FQ_NAME
        } ?: return null

        val functionDescriptor = newCopyBuilder()
            .setValueParameters(valueParameters.dropLast(1))
            .setReturnType(continuationParameter.type.arguments[0].type)
            .build()
        (functionDescriptor as SimpleFunctionDescriptorImpl?)?.isSuspend = true
        return functionDescriptor
    }

    private fun SimpleFunctionDescriptor.createRenamedCopy(builtinName: Name): SimpleFunctionDescriptor =
        this.newCopyBuilder().apply {
            setName(builtinName)
            setSignatureChange()
            setPreserveSourceElement()
        }.build()!!

    private fun doesOverrideRenamedDescriptor(
        superDescriptor: SimpleFunctionDescriptor,
        subDescriptor: FunctionDescriptor
    ): Boolean {
        // if we check 'removeAt', get original sub-descriptor to distinct `remove(int)` and `remove(E)` in Java
        val subDescriptorToCheck = if (superDescriptor.isRemoveAtByIndex) subDescriptor.original else subDescriptor

        return subDescriptorToCheck.doesOverride(superDescriptor)
    }

    private fun CallableDescriptor.doesOverride(superDescriptor: CallableDescriptor): Boolean {
        val commonOverridabilityResult =
            OverridingUtil.DEFAULT.isOverridableByWithoutExternalConditions(superDescriptor, this, true).result

        return commonOverridabilityResult == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE &&
                !JavaIncompatibilityRulesOverridabilityCondition.doesJavaOverrideHaveIncompatibleValueParameterKinds(
                    superDescriptor, this
                )
    }

    private fun PropertyDescriptor.findGetterOverride(
        functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        val overriddenBuiltinProperty = getter?.getOverriddenBuiltinWithDifferentJvmName()
        val specialGetterName = overriddenBuiltinProperty?.getBuiltinSpecialPropertyGetterName()
        if (specialGetterName != null
            && !this@LazyJavaClassMemberScope.ownerDescriptor.hasRealKotlinSuperClassWithOverrideOf(overriddenBuiltinProperty)
        ) {
            return findGetterByName(specialGetterName, functions)
        }

        return findGetterByName(JvmAbi.getterName(name.asString()), functions)
    }

    private fun PropertyDescriptor.findGetterByName(
        getterName: String,
        functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        return functions(Name.identifier(getterName)).firstNotNullOfOrNull factory@{ descriptor ->
            if (descriptor.valueParameters.size != 0) return@factory null

            descriptor.takeIf { KotlinTypeChecker.DEFAULT.isSubtypeOf(descriptor.returnType ?: return@takeIf false, type) }
        }
    }

    private fun PropertyDescriptor.findSetterOverride(
        functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        return functions(Name.identifier(JvmAbi.setterName(name.asString()))).firstNotNullOfOrNull factory@{ descriptor ->
            if (descriptor.valueParameters.size != 1) return@factory null

            if (!KotlinBuiltIns.isUnit(descriptor.returnType ?: return@factory null)) return@factory null
            descriptor.takeIf { KotlinTypeChecker.DEFAULT.equalTypes(descriptor.valueParameters.single().type, type) }
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

        if (!name.sameAsRenamedInJvmBuiltin && !name.sameAsBuiltinMethodWithErasedValueParameters
            && functionsFromSupertypes.none(FunctionDescriptor::isSuspend)
        ) {
            // Simple fast path in case of name is not suspicious (i.e. name is not one of builtins that have different signature in Java)
            addFunctionFromSupertypes(
                result, name,
                functionsFromSupertypes.filter { isVisibleAsFunctionInCurrentClass(it) },
                isSpecialBuiltinName = false
            )
            return
        }

        val specialBuiltinsFromSuperTypes = SmartSet.create<SimpleFunctionDescriptor>()

        // Merge functions with same signatures
        val mergedFunctionFromSuperTypes = resolveOverridesForNonStaticMembers(
            name, functionsFromSupertypes, emptyList(), ownerDescriptor, ErrorReporter.DO_NOTHING,
            c.components.kotlinTypeChecker.overridingUtil
        )

        // add declarations
        addOverriddenSpecialMethods(
            name, result, mergedFunctionFromSuperTypes, result,
            this::searchMethodsByNameWithoutBuiltinMagic
        )

        // add from super types
        addOverriddenSpecialMethods(
            name, result, mergedFunctionFromSuperTypes, specialBuiltinsFromSuperTypes,
            this::searchMethodsInSupertypesWithoutBuiltinMagic
        )

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
            name, functionsFromSupertypes, result, ownerDescriptor, c.components.errorReporter,
            c.components.kotlinTypeChecker.overridingUtil
        )

        if (!isSpecialBuiltinName) {
            result.addAll(additionalOverrides)
        } else {
            val allDescriptors = result + additionalOverrides
            result.addAll(
                additionalOverrides.map { resolvedOverride ->
                    val overriddenBuiltin = resolvedOverride.getOverriddenSpecialBuiltin()
                        ?: return@map resolvedOverride

                    resolvedOverride.createHiddenCopyIfBuiltinAlreadyAccidentallyOverridden(overriddenBuiltin, allDescriptors)
                })
        }
    }

    // - Built-in (collections) methods with different signature in JDK
    // - Suspend functions
    private fun addOverriddenSpecialMethods(
        name: Name,
        alreadyDeclaredFunctions: Collection<SimpleFunctionDescriptor>,
        candidatesForOverride: Collection<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>,
        functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ) {
        for (descriptor in candidatesForOverride) {
            result.addIfNotNull(
                obtainOverrideForBuiltinWithDifferentJvmName(descriptor, functions, name, alreadyDeclaredFunctions)
            )
            result.addIfNotNull(
                obtainOverrideForBuiltInWithErasedValueParametersInJava(descriptor, functions, alreadyDeclaredFunctions)
            )

            result.addIfNotNull(obtainOverrideForSuspend(descriptor, functions))
        }
    }

    private fun obtainOverrideForBuiltInWithErasedValueParametersInJava(
        descriptor: SimpleFunctionDescriptor,
        functions: (Name) -> Collection<SimpleFunctionDescriptor>,
        alreadyDeclaredFunctions: Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        val overriddenBuiltin =
            BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(descriptor)
                ?: return null

        return createOverrideForBuiltinFunctionWithErasedParameterIfNeeded(overriddenBuiltin, functions)
            ?.takeIf(this::isVisibleAsFunctionInCurrentClass)
            ?.createHiddenCopyIfBuiltinAlreadyAccidentallyOverridden(overriddenBuiltin, alreadyDeclaredFunctions)
    }

    private fun obtainOverrideForBuiltinWithDifferentJvmName(
        descriptor: SimpleFunctionDescriptor,
        functions: (Name) -> Collection<SimpleFunctionDescriptor>,
        name: Name,
        alreadyDeclaredFunctions: Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        val overriddenBuiltin = descriptor.getOverriddenBuiltinWithDifferentJvmName() ?: return null

        val nameInJava = getJvmMethodNameIfSpecial(overriddenBuiltin)!!
        for (method in functions(Name.identifier(nameInJava))) {
            val renamedCopy = method.createRenamedCopy(name)

            if (doesOverrideRenamedDescriptor(overriddenBuiltin, renamedCopy)) {
                return renamedCopy.createHiddenCopyIfBuiltinAlreadyAccidentallyOverridden(overriddenBuiltin, alreadyDeclaredFunctions)
            }
        }

        return null
    }

    private fun obtainOverrideForSuspend(
        descriptor: SimpleFunctionDescriptor,
        functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        if (!descriptor.isSuspend) return null

        return functions(descriptor.name).firstNotNullOfOrNull { overrideCandidate ->
            overrideCandidate.createSuspendView()?.takeIf { suspendView -> suspendView.doesOverride(descriptor) }
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
    ): SimpleFunctionDescriptor =
        if (alreadyDeclaredFunctions.none { this != it && it.initialSignatureDescriptor == null && it.doesOverride(specialBuiltin) })
            this
        else
            newCopyBuilder().setHiddenToOvercomeSignatureClash().build()!!

    private fun createOverrideForBuiltinFunctionWithErasedParameterIfNeeded(
        overridden: FunctionDescriptor,
        functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ): SimpleFunctionDescriptor? {
        return functions(overridden.name).firstOrNull {
            it.hasSameJvmDescriptorButDoesNotOverride(overridden)
        }?.let { override ->
            override.newCopyBuilder().apply {
                setValueParameters(
                    copyValueParameters(
                        overridden.valueParameters.map(ValueParameterDescriptor::getType),
                        override.valueParameters, overridden
                    )
                )
                setSignatureChange()
                setPreserveSourceElement()
                putUserData(JavaMethodDescriptor.HAS_ERASED_VALUE_PARAMETERS, true)
            }.build()
        }
    }

    private fun getFunctionsFromSupertypes(name: Name): Set<SimpleFunctionDescriptor> {
        return computeSupertypes()
            .flatMapTo(LinkedHashSet()) {
                it.memberScope.getContributedFunctions(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS)
            }
    }

    override fun computeImplicitlyDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        if (jClass.isRecord && declaredMemberIndex().findRecordComponentByName(name) != null && result.none { it.valueParameters.isEmpty() }) {
            result.add(resolveRecordComponentToFunctionDescriptor(declaredMemberIndex().findRecordComponentByName(name)!!))
        }

        c.components.syntheticPartsProvider.generateMethods(ownerDescriptor, name, result)
    }

    private fun resolveRecordComponentToFunctionDescriptor(recordComponent: JavaRecordComponent): JavaMethodDescriptor {
        val annotations = c.resolveAnnotations(recordComponent)
        val functionDescriptorImpl = JavaMethodDescriptor.createJavaMethod(
            ownerDescriptor, annotations, recordComponent.name, c.components.sourceElementFactory.source(recordComponent), true
        )

        val returnTypeAttrs = TypeUsage.COMMON.toAttributes(isForAnnotationParameter = false)
        val returnType = c.typeResolver.transformJavaType(recordComponent.type, returnTypeAttrs)

        functionDescriptorImpl.initialize(
            null,
            getDispatchReceiverParameter(),
            emptyList(),
            emptyList(),
            emptyList(),
            returnType,
            // Those functions are generated as open in bytecode
            // Actually, it should not be important because the class is final anyway, but leaving them open is convenient for consistency
            Modality.convertFromFlags(sealed = false, abstract = false, open = true),
            DescriptorVisibilities.PUBLIC,
            null,
        )

        functionDescriptorImpl.setParameterNamesStatus(false, false)

        c.components.javaResolverCache.recordMethod(recordComponent, functionDescriptorImpl)

        return functionDescriptorImpl
    }


    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        if (jClass.isAnnotationType) {
            computeAnnotationProperties(name, result)
        }

        val propertiesFromSupertypes = getPropertiesFromSupertypes(name)
        if (propertiesFromSupertypes.isEmpty()) return

        val handledProperties = SmartSet.create<PropertyDescriptor>()

        val propertiesOverridesFromSuperTypes = SmartSet.create<PropertyDescriptor>()

        addPropertyOverrideByMethod(propertiesFromSupertypes, result, handledProperties) { searchMethodsByNameWithoutBuiltinMagic(it) }

        addPropertyOverrideByMethod(propertiesFromSupertypes - handledProperties, propertiesOverridesFromSuperTypes, null) {
            searchMethodsInSupertypesWithoutBuiltinMagic(it)
        }

        result.addAll(
            resolveOverridesForNonStaticMembers(
                name,
                propertiesFromSupertypes + propertiesOverridesFromSuperTypes,
                result,
                ownerDescriptor,
                c.components.errorReporter,
                c.components.kotlinTypeChecker.overridingUtil
            )
        )
    }

    private fun addPropertyOverrideByMethod(
        propertiesFromSupertypes: Set<PropertyDescriptor>,
        result: MutableCollection<PropertyDescriptor>,
        handledProperties: MutableSet<PropertyDescriptor>?,
        functions: (Name) -> Collection<SimpleFunctionDescriptor>
    ) {
        for (property in propertiesFromSupertypes) {
            val newProperty = createPropertyDescriptorByMethods(property, functions)
            if (newProperty != null) {
                result.add(newProperty)
                handledProperties?.add(property)
                break
            }
        }
    }

    private fun computeAnnotationProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        val method = declaredMemberIndex().findMethodsByName(name).singleOrNull() ?: return
        result.add(createPropertyDescriptorWithDefaultGetter(method, modality = Modality.FINAL))
    }

    private fun createPropertyDescriptorWithDefaultGetter(
        method: JavaMethod, givenType: KotlinType? = null, modality: Modality
    ): JavaPropertyDescriptor {
        val annotations = c.resolveAnnotations(method)

        val propertyDescriptor = JavaPropertyDescriptor.create(
            ownerDescriptor, annotations, modality, method.visibility.toDescriptorVisibility(),
            /* isVar = */ false, method.name, c.components.sourceElementFactory.source(method),
            /* isStaticFinal = */ false
        )

        val getter = DescriptorFactory.createDefaultGetter(propertyDescriptor, Annotations.EMPTY)
        propertyDescriptor.initialize(getter, null)

        val returnType = givenType ?: computeMethodReturnType(method, c.childForMethod(propertyDescriptor, method))
        propertyDescriptor.setType(returnType, listOf(), getDispatchReceiverParameter(), null, listOf())
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
            "Different accessors modalities when creating overrides for $overriddenProperty in $ownerDescriptor" +
                    "for getter is ${getterMethod.modality}, but for setter is ${setterMethod?.modality}"
        }

        val propertyDescriptor = JavaForKotlinOverridePropertyDescriptor(ownerDescriptor, getterMethod, setterMethod, overriddenProperty)

        propertyDescriptor.setType(getterMethod.returnType!!, listOf(), getDispatchReceiverParameter(), null, listOf())

        val getter = DescriptorFactory.createGetter(
            propertyDescriptor, getterMethod.annotations, /* isDefault = */false,
            /* isExternal = */ false, /* isInline = */ false, getterMethod.source
        ).apply {
            initialSignatureDescriptor = getterMethod
            initialize(propertyDescriptor.type)
        }

        val setter = if (setterMethod != null) {
            val parameter = setterMethod.valueParameters.firstOrNull() ?: throw AssertionError("No parameter found for $setterMethod")
            DescriptorFactory.createSetter(
                propertyDescriptor, setterMethod.annotations, parameter.annotations, /* isDefault = */false,
                /* isExternal = */ false, /* isInline = */ false, setterMethod.visibility, setterMethod.source
            ).apply {
                initialSignatureDescriptor = setterMethod
            }
        } else null

        return propertyDescriptor.apply { initialize(getter, setter) }
    }

    private fun getPropertiesFromSupertypes(name: Name): Set<PropertyDescriptor> {
        return computeSupertypes().flatMap {
            it.memberScope.getContributedVariables(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS).map { p -> p }
        }.toSet()
    }

    private fun computeSupertypes(): Collection<KotlinType> {
        if (skipRefinement) return ownerDescriptor.typeConstructor.supertypes

        @OptIn(TypeRefinement::class)
        return c.components.kotlinTypeChecker.kotlinTypeRefiner.refineSupertypes(ownerDescriptor)
    }

    override fun resolveMethodSignature(
        method: JavaMethod, methodTypeParameters: List<TypeParameterDescriptor>, returnType: KotlinType,
        valueParameters: List<ValueParameterDescriptor>
    ): MethodSignatureData {
        val propagated = c.components.signaturePropagator.resolvePropagatedSignature(
            method, ownerDescriptor, returnType, null, valueParameters, methodTypeParameters
        )
        return MethodSignatureData(
            propagated.returnType, propagated.receiverType, propagated.valueParameters, propagated.typeParameters,
            propagated.hasStableParameterNames(), propagated.errors
        )
    }

    private fun SimpleFunctionDescriptor.hasSameJvmDescriptorButDoesNotOverride(
        builtinWithErasedParameters: FunctionDescriptor
    ): Boolean {
        return computeJvmDescriptor(withReturnType = false) ==
                builtinWithErasedParameters.original.computeJvmDescriptor(withReturnType = false)
                && !doesOverride(builtinWithErasedParameters)
    }

    private fun resolveConstructor(constructor: JavaConstructor): JavaClassConstructorDescriptor {
        val classDescriptor = ownerDescriptor

        val constructorDescriptor = JavaClassConstructorDescriptor.createJavaConstructor(
            classDescriptor,
            c.resolveAnnotations(constructor), /* isPrimary = */
            false,
            c.components.sourceElementFactory.source(constructor)
        )


        val c =
            c.childForMethod(constructorDescriptor, constructor, typeParametersIndexOffset = classDescriptor.declaredTypeParameters.size)
        val valueParameters = resolveValueParameters(c, constructorDescriptor, constructor.valueParameters)
        val constructorTypeParameters =
            classDescriptor.declaredTypeParameters +
                    constructor.typeParameters.map { p -> c.typeParameterResolver.resolveTypeParameter(p)!! }

        constructorDescriptor.initialize(
            valueParameters.descriptors,
            constructor.visibility.toDescriptorVisibility(),
            constructorTypeParameters
        )
        constructorDescriptor.setHasStableParameterNames(false)
        constructorDescriptor.setHasSynthesizedParameterNames(valueParameters.hasSynthesizedNames)

        constructorDescriptor.returnType = classDescriptor.defaultType

        c.components.javaResolverCache.recordConstructor(constructor, constructorDescriptor)

        return constructorDescriptor
    }

    private fun createDefaultConstructor(): ClassConstructorDescriptor? {
        val isAnnotation: Boolean = jClass.isAnnotationType
        if ((jClass.isInterface || !jClass.hasDefaultConstructor()) && !isAnnotation)
            return null

        val classDescriptor = ownerDescriptor
        val constructorDescriptor = JavaClassConstructorDescriptor.createJavaConstructor(
            classDescriptor, Annotations.EMPTY, /* isPrimary = */ true, c.components.sourceElementFactory.source(jClass)
        )
        val valueParameters = if (isAnnotation) createAnnotationConstructorParameters(constructorDescriptor)
        else Collections.emptyList<ValueParameterDescriptor>()
        constructorDescriptor.setHasSynthesizedParameterNames(false)

        constructorDescriptor.initialize(valueParameters, getConstructorVisibility(classDescriptor))
        constructorDescriptor.setHasStableParameterNames(true)
        constructorDescriptor.returnType = classDescriptor.defaultType
        c.components.javaResolverCache.recordConstructor(jClass, constructorDescriptor)
        return constructorDescriptor
    }

    private fun getConstructorVisibility(classDescriptor: ClassDescriptor): DescriptorVisibility {
        val visibility = classDescriptor.visibility
        if (visibility == JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY) {
            return JavaDescriptorVisibilities.PROTECTED_AND_PACKAGE
        }
        return visibility
    }

    private fun createAnnotationConstructorParameters(constructor: ClassConstructorDescriptorImpl): List<ValueParameterDescriptor> {
        val methods = jClass.methods
        val result = ArrayList<ValueParameterDescriptor>(methods.size)

        val attr = TypeUsage.COMMON.toAttributes(isForAnnotationParameter = true)

        val (methodsNamedValue, otherMethods) = methods.partition { it.name == JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME }

        assert(methodsNamedValue.size <= 1) { "There can't be more than one method named 'value' in annotation class: $jClass" }
        val methodNamedValue = methodsNamedValue.firstOrNull()
        if (methodNamedValue != null) {
            val parameterNamedValueJavaType = methodNamedValue.returnType
            val (parameterType, varargType) =
                if (parameterNamedValueJavaType is JavaArrayType)
                    Pair(
                        c.typeResolver.transformArrayType(parameterNamedValueJavaType, attr, isVararg = true),
                        c.typeResolver.transformJavaType(parameterNamedValueJavaType.componentType, attr)
                    )
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
        add(
            ValueParameterDescriptorImpl(
                constructor,
                null,
                index,
                Annotations.EMPTY,
                method.name,
                // Parameters of annotation constructors in Java are never nullable
                TypeUtils.makeNotNullable(returnType),
                method.hasAnnotationParameterDefaultValue,
                /* isCrossinline = */ false,
                /* isNoinline = */ false,
                // Nulls are not allowed in annotation arguments in Java
                varargElementType?.let { TypeUtils.makeNotNullable(it) },
                c.components.sourceElementFactory.source(method)
            )
        )
    }

    private val nestedClassIndex = c.storageManager.createLazyValue {
        jClass.innerClassNames.toSet()
    }

    private val enumEntryIndex = c.storageManager.createLazyValue {
        jClass.fields.filter { it.isEnumEntry }.associateBy { f -> f.name }
    }

    private val nestedClasses = c.storageManager.createMemoizedFunctionWithNullableValues { name: Name ->
        if (name !in nestedClassIndex()) {
            val field = enumEntryIndex()[name]
            if (field != null) {
                val enumMemberNames: NotNullLazyValue<Set<Name>> = c.storageManager.createLazyValue {
                    getFunctionNames() + getVariableNames()
                }
                EnumEntrySyntheticClassDescriptor.create(
                    c.storageManager, ownerDescriptor, name, enumMemberNames, c.resolveAnnotations(field),
                    c.components.sourceElementFactory.source(field)
                )
            } else null
        } else {
            c.components.finder.findClass(
                JavaClassFinder.Request(
                    ownerDescriptor.classId!!.createNestedClassId(name),
                    outerClass = jClass
                )
            )?.let {
                LazyJavaClassDescriptor(c, ownerDescriptor, it)
                    .also(c.components.javaClassesTracker::reportClass)
            }
        }
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? =
        DescriptorUtils.getDispatchReceiverParameterIfNeeded(ownerDescriptor)

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        recordLookup(name, location)

        return (mainScope as LazyJavaClassMemberScope?)?.nestedClasses?.invoke(name) ?: nestedClasses(name)
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        recordLookup(name, location)
        return super.getContributedFunctions(name, location)
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        recordLookup(name, location)
        return super.getContributedVariables(name, location)
    }

    override fun computeClassNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?): Set<Name> =
        nestedClassIndex() + enumEntryIndex().keys

    override fun computePropertyNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?): Set<Name> {
        if (jClass.isAnnotationType) return getFunctionNames()
        val result = LinkedHashSet(declaredMemberIndex().getFieldNames())
        return ownerDescriptor.typeConstructor.supertypes.flatMapTo(result) { supertype ->
            supertype.memberScope.getVariableNames()
        }
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.components.lookupTracker.record(location, ownerDescriptor, name)
    }

    override fun toString() = "Lazy Java member scope for " + jClass.fqName
}
