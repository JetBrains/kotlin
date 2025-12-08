/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightParameterListBuilder
import com.intellij.psi.impl.light.LightReferenceListBuilder
import org.jetbrains.kotlin.analysis.api.KaConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.KaConstantValueForAnnotation
import org.jetbrains.kotlin.analysis.api.KaNonConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_GETTER
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_FOR_SETTER
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.classes.*
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterForDefaultImplsReceiver
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightSetterParameter
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.load.java.JvmAbi.getterName
import org.jetbrains.kotlin.load.java.JvmAbi.setterName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal class SymbolLightAccessorMethod private constructor(
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    private val isGetter: Boolean,
    private val propertyAccessorDeclaration: KtPropertyAccessor?,
    private val propertyAccessorSymbolPointer: KaSymbolPointer<KaPropertyAccessorSymbol>,
    private val containingPropertyDeclaration: KtCallableDeclaration?,
    private val containingPropertySymbolPointer: KaSymbolPointer<KaPropertySymbol>,
    private val isTopLevel: Boolean,
    private val suppressStatic: Boolean,
    isJvmExposedBoxed: Boolean,
) : SymbolLightMethodBase(
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = methodIndex,
    isJvmExposedBoxed = isJvmExposedBoxed,
) {
    private constructor(
        propertyAccessorSymbol: KaPropertyAccessorSymbol,
        containingPropertySymbol: KaPropertySymbol,
        lightMemberOrigin: LightMemberOrigin?,
        containingClass: SymbolLightClassBase,
        isTopLevel: Boolean,
        suppressStatic: Boolean,
        isJvmExposedBoxed: Boolean,
    ) : this(
        lightMemberOrigin,
        containingClass,
        methodIndex = if (propertyAccessorSymbol is KaPropertyGetterSymbol) METHOD_INDEX_FOR_GETTER else METHOD_INDEX_FOR_SETTER,
        isGetter = propertyAccessorSymbol is KaPropertyGetterSymbol,
        propertyAccessorDeclaration = propertyAccessorSymbol.sourcePsiSafe(),
        propertyAccessorSymbolPointer = propertyAccessorSymbol.createPointer(),
        containingPropertyDeclaration = containingPropertySymbol.sourcePsiSafe(),
        containingPropertySymbolPointer = containingPropertySymbol.createPointer(),
        isTopLevel = isTopLevel,
        suppressStatic = suppressStatic,
        isJvmExposedBoxed = isJvmExposedBoxed,
    )

    private val KaPropertySymbol.accessorSymbol: KaPropertyAccessorSymbol
        get() = if (isGetter) getter!! else setter!!

    private inline fun <T> withPropertySymbol(crossinline action: KaSession.(KaPropertySymbol) -> T): T =
        containingPropertySymbolPointer.withSymbol(ktModule, action)

    private inline fun <T> withAccessorSymbol(crossinline action: KaSession.(KaPropertyAccessorSymbol) -> T): T =
        propertyAccessorSymbolPointer.withSymbol(ktModule, action)

    private fun String.abiName() = if (isGetter) getterName(this) else setterName(this)

    private val _name: String by lazyPub {
        withPropertySymbol { propertySymbol ->
            val accessorSymbol = propertySymbol.accessorSymbol
            val outerClass = this@SymbolLightAccessorMethod.containingClass
            val defaultName = propertySymbol.name.identifier.let {
                if (outerClass.isAnnotationType || outerClass.isRecord)
                    it
                else
                    it.abiName()
            }

            if (isJvmExposedBoxed) {
                computeJvmExposeBoxedMethodName(accessorSymbol, defaultName)
            } else {
                computeJvmMethodName(accessorSymbol, defaultName)
            }
        }
    }

    override fun getName(): String = _name

    private val _typeParameterList: PsiTypeParameterList? by lazyPub {
        hasTypeParameters().ifTrue {
            SymbolLightTypeParameterList(
                owner = this,
                symbolWithTypeParameterPointer = containingPropertySymbolPointer,
                ktModule = ktModule,
                ktDeclaration = containingPropertyDeclaration,
            )
        }
    }

    override fun hasTypeParameters(): Boolean {
        return withPropertySymbol { it.typeParameters.isNotEmpty() } || containingClass.isDefaultImplsForInterfaceWithTypeParameters
    }

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList
    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    override fun isVarArgs(): Boolean = false

    //TODO Fix it when SymbolConstructorValueParameter be ready
    private val isParameter: Boolean get() = containingPropertyDeclaration == null || containingPropertyDeclaration is KtParameter

    override fun computeThrowsList(builder: LightReferenceListBuilder) {
        withAccessorSymbol { accessorSymbol ->
            computeThrowsList(
                accessorSymbol,
                builder,
                this@SymbolLightAccessorMethod,
                containingClass,
            )
        }
    }

    private fun computeModifiers(modifier: String): Map<String, Boolean>? = when (modifier) {
        in GranularModifiersBox.VISIBILITY_MODIFIERS -> GranularModifiersBox.computeVisibilityForMember(
            ktModule,
            propertyAccessorSymbolPointer,
        )

        in GranularModifiersBox.MODALITY_MODIFIERS -> {
            val modality = if (containingClass.isInterface) {
                PsiModifier.ABSTRACT
            } else {
                withPropertySymbol { propertySymbol ->
                    propertySymbol.computeSimpleModality()?.takeUnless { isSuppressedFinalModifier(it, containingClass, propertySymbol) }
                }
            }

            GranularModifiersBox.MODALITY_MODIFIERS_MAP.with(modality)
        }

        PsiModifier.STATIC -> {
            val isStatic = if (suppressStatic) {
                false
            } else {
                isTopLevel || containingClass is SymbolLightClassForInterfaceDefaultImpls || isStatic()
            }

            mapOf(modifier to isStatic)
        }

        else -> null
    }

    private fun isStatic(): Boolean = withPropertySymbol { propertySymbol ->
        if (propertySymbol.isStatic) {
            return@withPropertySymbol true
        }

        propertySymbol.hasJvmStaticAnnotation() || propertySymbol.accessorSymbol.hasJvmStaticAnnotation()
    }

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(computer = ::computeModifiers),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = propertyAccessorSymbolPointer,
                ),
                additionalAnnotationsProvider = CompositeAdditionalAnnotationsProvider(
                    NullabilityAnnotationsProvider {
                        val nullabilityApplicable = isGetter &&
                                !(isParameter && this.containingClass.isAnnotationType) &&
                                !modifierList.hasModifierProperty(PsiModifier.PRIVATE)

                        if (nullabilityApplicable) {
                            withPropertySymbol { propertySymbol ->
                                when {
                                    propertySymbol.isLateInit || shouldEnforceBoxedReturnType(propertySymbol) -> NullabilityAnnotation.NON_NULLABLE
                                    else -> getRequiredNullabilityAnnotation(propertySymbol.returnType)
                                }
                            }
                        } else {
                            NullabilityAnnotation.NOT_REQUIRED
                        }
                    },
                    MethodAdditionalAnnotationsProvider,
                    JvmExposeBoxedAdditionalAnnotationsProvider,
                ),
                annotationFilter = jvmExposeBoxedAwareAnnotationFilter,
            ),
        )
    }

    override fun isConstructor(): Boolean = false

    private val _isDeprecated: Boolean by lazyPub {
        withPropertySymbol { propertySymbol ->
            propertySymbol.hasDeprecatedAnnotation() || propertySymbol.accessorSymbol.hasDeprecatedAnnotation()
        }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(this, containingPropertyDeclaration)

    private fun KaSession.shouldEnforceBoxedReturnType(propertySymbol: KaPropertySymbol): Boolean {
        return isJvmExposedBoxed && typeForValueClass(propertySymbol.returnType) ||
                propertySymbol.returnType.isPrimitiveBacked &&
                propertySymbol.allOverriddenSymbols.any { overriddenSymbol ->
                    !overriddenSymbol.returnType.isPrimitiveBacked
                }
    }

    private val _returnedType: PsiType by lazyPub {
        if (!isGetter) return@lazyPub PsiTypes.voidType()

        withPropertySymbol { propertySymbol ->
            val ktType = propertySymbol.returnType

            val typeMappingMode = if (shouldEnforceBoxedReturnType(propertySymbol))
                KaTypeMappingMode.RETURN_TYPE_BOXED
            else
                KaTypeMappingMode.RETURN_TYPE

            ktType.asPsiType(
                this@SymbolLightAccessorMethod,
                allowErrorTypes = true,
                typeMappingMode,
                containingClass.isAnnotationType,
                suppressWildcards(),
                allowNonJvmPlatforms = true,
            )
        } ?: nonExistentType()
    }

    override fun getReturnType(): PsiType = _returnedType

    override fun suppressWildcards(): Boolean? =
        withAccessorSymbol { accessorSymbol ->
            suppressWildcardMode(accessorSymbol) { parent ->
                parent !is KaPropertySymbol
            }
        }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return super.isEquivalentTo(another) || basicIsEquivalentTo(this, another as? PsiField)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightAccessorMethod ||
            other.isGetter != isGetter ||
            other.isTopLevel != isTopLevel ||
            other.suppressStatic != suppressStatic ||
            other.isJvmExposedBoxed != isJvmExposedBoxed ||
            other.ktModule != ktModule
        ) return false

        if (propertyAccessorDeclaration != null || other.propertyAccessorDeclaration != null) {
            return propertyAccessorDeclaration == other.propertyAccessorDeclaration
        }

        if (containingPropertyDeclaration != null || other.containingPropertyDeclaration != null) {
            return containingPropertyDeclaration == other.containingPropertyDeclaration
        }

        return compareSymbolPointers(propertyAccessorSymbolPointer, other.propertyAccessorSymbolPointer)
    }

    override fun hashCode(): Int = propertyAccessorDeclaration?.hashCode() ?: containingPropertyDeclaration.hashCode()

    private val _parametersList by lazyPub {
        val baseParameterPopulator: (LightParameterListBuilder) -> Unit = if (!isGetter) {
            { builder ->
                withAccessorSymbol { accessorSymbol ->
                    val setterParameter = (accessorSymbol as? KaPropertySetterSymbol)?.parameter ?: return@withAccessorSymbol
                    builder.addParameter(
                        SymbolLightSetterParameter(
                            containingPropertySymbolPointer = containingPropertySymbolPointer,
                            parameterSymbol = setterParameter,
                            containingMethod = this@SymbolLightAccessorMethod,
                        )
                    )
                }
            }
        } else {
            { }
        }

        val parameterPopulator: (LightParameterListBuilder) -> Unit = { builder ->
            if (containingClass is SymbolLightClassForInterfaceDefaultImpls) {
                builder.addParameter(SymbolLightParameterForDefaultImplsReceiver(this@SymbolLightAccessorMethod))
            }
            baseParameterPopulator(builder)
        }

        SymbolLightParameterList(
            parent = this@SymbolLightAccessorMethod,
            correspondingCallablePointer = containingPropertySymbolPointer,
            parameterPopulator = parameterPopulator,
        )
    }

    override fun getParameterList(): PsiParameterList = _parametersList

    override fun isValid(): Boolean =
        super.isValid() && propertyAccessorDeclaration?.isValid
                ?: containingPropertyDeclaration?.isValid
                ?: propertyAccessorSymbolPointer.isValid(ktModule)

    private val _isOverride: Boolean by lazyPub {
        if (isTopLevel) {
            false
        } else {
            withAccessorSymbol { accessorSymbol ->
                accessorSymbol.isOverride
            }
        }
    }

    override fun isOverride(): Boolean = _isOverride

    private val _defaultValue: PsiAnnotationMemberValue? by lazyPub {
        if (!containingClass.isAnnotationType) return@lazyPub null

        withPropertySymbol { propertySymbol ->
            when (val initializer = propertySymbol.initializer) {
                is KaConstantInitializerValue -> {
                    initializer.constant.createPsiExpression(this@SymbolLightAccessorMethod)
                }
                is KaConstantValueForAnnotation -> {
                    initializer.annotationValue.toLightClassAnnotationValue().toAnnotationMemberValue(this@SymbolLightAccessorMethod)
                }
                is KaNonConstantInitializerValue -> null
                null -> null
            }
        }
    }

    override fun getDefaultValue(): PsiAnnotationMemberValue? = _defaultValue

    override fun getText(): String {
        return lightMemberOrigin?.auxiliaryOriginalElement?.text ?: super.getText()
    }

    override fun getTextOffset(): Int {
        return lightMemberOrigin?.auxiliaryOriginalElement?.textOffset ?: super.getTextOffset()
    }

    override fun getTextRange(): TextRange? {
        return lightMemberOrigin?.auxiliaryOriginalElement?.textRange ?: super.getTextRange()
    }

    companion object {
        /**
         * Represents the context during accessors creation.
         */
        private class Context private constructor(
            val property: KaPropertySymbol,
            val destinationLightClass: SymbolLightClassBase,
            /** Whether the static modifier should be suppressed for the accessors. */
            val suppressStatic: Boolean,
            val isTopLevel: Boolean,
            /** Whether the accessors should be created only if they are marked with [JvmStatic] annotation. */
            val staticsFromCompanion: Boolean,
            private val hasValueClassInParameterType: Boolean,
            private val hasValueClassInReturnType: Boolean,
            private val jvmExposeBoxedMode: JvmExposeBoxedMode,
        ) {
            fun jvmExposeBoxedMode(accessor: KaPropertyAccessorSymbol): JvmExposeBoxedMode =
                if (accessor.hasJvmExposeBoxedAnnotation()) JvmExposeBoxedMode.EXPLICIT else jvmExposeBoxedMode

            fun hasValueClassInParameterType(accessor: KaPropertyAccessorSymbol): Boolean =
                if (accessor is KaPropertySetterSymbol) {
                    // Setter uses the return type as a value parameter
                    hasValueClassInParameterType || hasValueClassInReturnType
                } else {
                    hasValueClassInParameterType
                }

            fun hasValueClassInReturnType(accessor: KaPropertyAccessorSymbol): Boolean =
                if (accessor is KaPropertySetterSymbol) {
                    // Setter has a Unit return type
                    false
                } else {
                    hasValueClassInReturnType
                }

            companion object {
                context(session: KaSession)
                fun create(
                    property: KaPropertySymbol,
                    destinationLightClass: SymbolLightClassBase,
                    suppressStatic: Boolean,
                    isTopLevel: Boolean,
                    staticsFromCompanion: Boolean,
                ): Context = with(session) {
                    Context(
                        property = property,
                        destinationLightClass = destinationLightClass,
                        suppressStatic = suppressStatic,
                        isTopLevel = isTopLevel,
                        staticsFromCompanion = staticsFromCompanion,
                        hasValueClassInParameterType = hasValueClassInSignature(property, skipReturnTypeCheck = true),
                        hasValueClassInReturnType = hasValueClassInReturnType(property),
                        jvmExposeBoxedMode = jvmExposeBoxedMode(property),
                    )
                }
            }
        }

        context(context: Context)
        private val property: KaPropertySymbol get() = context.property

        internal fun KaSession.createPropertyAccessors(
            lightClass: SymbolLightClassBase,
            result: MutableList<PsiMethod>,
            declaration: KaPropertySymbol,
            isTopLevel: Boolean,
            isMutable: Boolean = !declaration.isVal,
            staticsFromCompanion: Boolean = false,
            suppressStatic: Boolean = false,
        ) {
            ProgressManager.checkCanceled()

            when {
                declaration.name.isSpecial -> return

                declaration is KaKotlinPropertySymbol && declaration.isConst -> return
                declaration.isJvmField -> return
                declaration.hasReifiedParameters -> return
            }

            val context = Context.create(
                property = declaration,
                destinationLightClass = lightClass,
                suppressStatic = suppressStatic,
                isTopLevel = isTopLevel,
                staticsFromCompanion = staticsFromCompanion,
            )

            with(context) {
                val getter = declaration.getter
                if (getter != null) {
                    produceSymbolLightAccessorMethodIfNeeded(
                        accessor = getter,
                        result = result,
                    )
                }

                val setter = declaration.takeIf { isMutable }?.setter
                if (setter != null && !lightClass.isAnnotationType) {
                    produceSymbolLightAccessorMethodIfNeeded(
                        accessor = setter,
                        result = result,
                    )
                }
            }
        }

        context(context: Context)
        private fun KaSession.produceSymbolLightAccessorMethodIfNeeded(
            accessor: KaPropertyAccessorSymbol,
            result: MutableList<PsiMethod>,
        ) {
            val accessorCanExist = lightAccessorCanExist(
                accessorSymbol = accessor,
                siteTarget = if (accessor is KaPropertyGetterSymbol)
                    AnnotationUseSiteTarget.PROPERTY_GETTER
                else
                    AnnotationUseSiteTarget.PROPERTY_SETTER,
            )

            if (!accessorCanExist) return

            val exposeBoxedMode = context.jvmExposeBoxedMode(accessor)
            val hasJvmNameAnnotation = accessor.hasJvmNameAnnotation()

            val hasValueClassInParameterType = context.hasValueClassInParameterType(accessor)
            val hasValueClassInReturnType = context.hasValueClassInReturnType(accessor)

            val hasMangledNameDueValueClassesInSignature = hasMangledNameDueValueClassesInSignature(
                hasValueClassInParameterType = hasValueClassInParameterType,
                hasValueClassInReturnType = hasValueClassInReturnType,
                isTopLevel = context.isTopLevel,
            )

            val isNonMaterializableValueClassProperty =
                // Assessors with JvmStatic should be materialized inside the containing value class
                !context.staticsFromCompanion &&
                        context.destinationLightClass.isValueClass &&
                        // Constructor properties are materialized by default
                        !property.isFromPrimaryConstructor &&
                        // Overrides are materialized by default
                        !property.isOverride

            val generationResult = methodGeneration(
                exposeBoxedMode = exposeBoxedMode,
                hasValueClassInParameterType = hasValueClassInParameterType,
                hasValueClassInReturnType = hasValueClassInReturnType,
                isAffectedByValueClass = hasMangledNameDueValueClassesInSignature || isNonMaterializableValueClassProperty,
                hasJvmNameAnnotation = hasJvmNameAnnotation,
                isSuspend = false,
                isOverridable = accessor.isOverridable()
            )

            if (!generationResult.isAnyMethodRequired) return

            val lightMemberOrigin = getLightMemberOriginForAccessor(accessor)

            if (generationResult.isBoxedMethodRequired) {
                result += SymbolLightAccessorMethod(
                    propertyAccessorSymbol = accessor,
                    containingPropertySymbol = property,
                    lightMemberOrigin = lightMemberOrigin,
                    containingClass = context.destinationLightClass,
                    isTopLevel = context.isTopLevel,
                    suppressStatic = context.suppressStatic,
                    isJvmExposedBoxed = true,
                )
            }

            if (generationResult.isRegularMethodRequired) {
                result += SymbolLightAccessorMethod(
                    propertyAccessorSymbol = accessor,
                    containingPropertySymbol = property,
                    lightMemberOrigin = lightMemberOrigin,
                    containingClass = context.destinationLightClass,
                    isTopLevel = context.isTopLevel,
                    suppressStatic = context.suppressStatic,
                    isJvmExposedBoxed = false,
                )
            }
        }

        context(context: Context)
        private fun getLightMemberOriginForAccessor(accessor: KaPropertyAccessorSymbol): LightMemberOriginForDeclaration? {
            val originalElement = property.psiSafe<KtDeclaration>() ?: return null

            return when (property.origin) {
                KaSymbolOrigin.SOURCE -> {
                    /**
                     * [org.jetbrains.kotlin.KtFakeSourceElementKind.DelegatedPropertyAccessor] is not allowed as source PSI, e.g.,
                     * ```
                     * val p by delegate(...)
                     * ```
                     *
                     * However, we also lose the source PSI of a custom property accessor, e.g.,
                     * ```
                     * val p by delegate(...)
                     *   get() = ...
                     * ```
                     *
                     * We go upward to the property's source PSI and attempt to find/bind accessor's source PSI.
                     */
                    fun sourcePsiFromProperty(): KtPropertyAccessor? {
                        if (accessor.origin != KaSymbolOrigin.SOURCE) return null
                        if (originalElement !is KtProperty) return null
                        return if (accessor is KaPropertyGetterSymbol) originalElement.getter else originalElement.setter
                    }

                    LightMemberOriginForDeclaration(
                        originalElement = originalElement,
                        originKind = JvmDeclarationOriginKind.OTHER,
                        auxiliaryOriginalElement = accessor.sourcePsiSafe<KtDeclaration>() ?: sourcePsiFromProperty()
                    )
                }

                KaSymbolOrigin.SOURCE_MEMBER_GENERATED ->
                    LightMemberOriginForDeclaration(originalElement, originKind = JvmDeclarationOriginKind.OTHER)

                KaSymbolOrigin.DELEGATED ->
                    LightMemberOriginForDeclaration(originalElement, originKind = JvmDeclarationOriginKind.DELEGATION)

                else -> null
            }
        }

        /**
         * Whether a light class potentially can be generated for the given accessor symbol
         */
        context(context: Context)
        private fun KaSession.lightAccessorCanExist(
            accessorSymbol: KaPropertyAccessorSymbol,
            siteTarget: AnnotationUseSiteTarget,
        ): Boolean = when {
            context.staticsFromCompanion && !accessorSymbol.hasJvmStaticAnnotation() && !property.hasJvmStaticAnnotation() -> false
            isHiddenByDeprecation(property) -> false
            isHiddenOrSynthetic(accessorSymbol, siteTarget) -> false
            !accessorSymbol.isNotDefault && accessorSymbol.visibility == KaSymbolVisibility.PRIVATE -> false
            // Value classes have special logic
            context.destinationLightClass.isValueClass -> when {
                // Overrides are generated for value classes
                property.isOverride -> true

                // Only public properties from the constructor can be exposed as regular accessors
                else -> !property.isFromPrimaryConstructor || property.visibility == KaSymbolVisibility.PUBLIC
            }

            else -> true
        }
    }
}
