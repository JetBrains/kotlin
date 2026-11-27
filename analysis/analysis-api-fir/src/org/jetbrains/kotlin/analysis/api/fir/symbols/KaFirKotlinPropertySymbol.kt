/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.KaNonConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.*
import org.jetbrains.kotlin.analysis.api.fir.utils.callableId
import org.jetbrains.kotlin.analysis.api.fir.utils.callableIdForName
import org.jetbrains.kotlin.analysis.api.fir.utils.inferReturnTypeByPsi
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirCallableSignature
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal sealed class KaFirKotlinPropertySymbol<P : KtCallableDeclaration>(
    final override val backingPsi: P?,
    final override val analysisSession: KaFirSession,
    final override val lazyFirSymbol: Lazy<FirPropertySymbol>,
) : KaKotlinPropertySymbol(), KaFirKtBasedSymbol<P, FirPropertySymbol> {
    override val name: Name
        get() = entryPointImplementation { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val returnType: KaType
        get() = entryPointImplementation { createReturnType() }

    /**
     * The type of the property inferred from its PSI without resolution, if possible.
     */
    val returnTypeByPsi: KaType?
        get() = entryPointImplementation { backingPsi?.inferReturnTypeByPsi(analysisSession = analysisSession) }

    override val contextReceivers: List<KaContextReceiver>
        get() = entryPointImplementation { createContextReceivers() }

    override val contextParameters: List<KaContextParameterSymbol>
        get() = entryPointImplementation { emptyList() }

    override val isExtension: Boolean
        get() = entryPointImplementation { backingPsi?.isExtensionDeclaration() ?: firSymbol.isExtension }

    val isInline: Boolean
        get() = entryPointImplementation { backingPsi?.hasModifier(KtTokens.INLINE_KEYWORD) ?: firSymbol.isInline }

    override val annotations: KaAnnotationList
        get() = entryPointImplementation { psiOrSymbolAnnotationList() }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = entryPointImplementation {
            createKaTypeParameters() ?: firSymbol.createKtTypeParameters()
        }

    open val compilerVisibilityByPsi: Visibility?
        get() = entryPointImplementation {
            backingPsi?.psiBasedVisibility(::isOverride)
        }

    override val visibility: KaSymbolVisibility
        get() = entryPointImplementation { (compilerVisibilityByPsi ?: firSymbol.visibility).asKaSymbolVisibility }

    abstract val modalityByPsi: KaSymbolModality?

    override val modality: KaSymbolModality
        get() = entryPointImplementation { modalityByPsi ?: firSymbol.kaSymbolModality }

    override val backingFieldSymbol: KaBackingFieldSymbol?
        get() = entryPointImplementation {
            val backingPsi = backingPsi
            if (backingPsi != null) {
                val backingFieldPsi = (backingPsi as? KtProperty)?.fieldDeclaration
                return if (backingFieldPsi != null) {
                    KaFirBackingFieldSymbol(backingFieldPsi, analysisSession, this)
                } else {
                    /**
                     * For consistency with the FIR compiler implementation. Even for computed properties without a real backing field,
                     * the [org.jetbrains.kotlin.fir.declarations.FirBackingField] is still generated.
                     */
                    KaFirDefaultBackingFieldSymbol(analysisSession, this)
                }
            }

            val backingFieldSymbol = firSymbol.backingFieldSymbol
            if (backingFieldSymbol != null) {
                return if (backingFieldSymbol.fir !is FirDefaultPropertyBackingField) {
                    KaFirBackingFieldSymbol(backingFieldSymbol, analysisSession, this)
                } else {
                    KaFirDefaultBackingFieldSymbol(analysisSession, this)
                }
            }

            return null
        }

    override val isLateInit: Boolean
        get() = entryPointImplementation {
            backingPsi?.hasModifier(KtTokens.LATEINIT_KEYWORD) ?: firSymbol.isLateInit
        }

    override val isOverride: Boolean
        get() = entryPointImplementation {
            isOverrideWithWorkaround
        }

    override val isConst: Boolean
        get() = entryPointImplementation {
            backingPsi?.hasModifier(KtTokens.CONST_KEYWORD) ?: firSymbol.isConst
        }

    override val isActual: Boolean
        get() = entryPointImplementation { backingPsi?.isActualDeclaration() ?: firSymbol.isActual }

    override val isExpect: Boolean
        get() = entryPointImplementation { backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect }

    override fun createPointer(): KaSymbolPointer<KaKotlinPropertySymbol> = entryPointImplementation {
        psiBasedSymbolPointerOfTypeIfSource<KaVariableSymbol>()?.let {
            return KaFirPsiBasedPropertySymbolPointer(it, this)
        }

        when (val kind = location) {
            KaSymbolLocation.TOP_LEVEL -> {
                if (firSymbol.fir.origin is FirDeclarationOrigin.ScriptCustomization.ResultProperty) {
                    KaFirResultPropertySymbolPointer(createOwnerPointer(), this)
                } else {
                    KaFirTopLevelPropertySymbolPointer(
                        firSymbol.callableId!!,
                        FirCallableSignature.createSignature(firSymbol),
                        this
                    )
                }
            }

            KaSymbolLocation.CLASS -> when (origin) {
                KaSymbolOrigin.JS_DYNAMIC -> KaFirDynamicPropertySymbolPointer(name, this)
                else -> KaFirMemberPropertySymbolPointer(
                    ownerPointer = createOwnerPointer(),
                    name = name,
                    signature = FirCallableSignature.createSignature(firSymbol),
                    isStatic = firSymbol.isStatic,
                    originalSymbol = this
                )
            }

            else -> throw KaUnsupportedSymbolLocation(this::class, kind)
        }
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()

    companion object {
        context(analysisSession: KaFirSession)
        fun create(declaration: KtProperty): KaKotlinPropertySymbol = KaFirKotlinPropertyKtPropertyBasedSymbol(declaration, analysisSession)

        context(analysisSession: KaFirSession)
        fun create(declaration: KtParameter): KaKotlinPropertySymbol = KaFirKotlinPropertyKtParameterBasedSymbol(declaration, analysisSession)

        context(analysisSession: KaFirSession)
        fun create(declaration: KtDestructuringDeclarationEntry): KaKotlinPropertySymbol = KaFirKotlinPropertyKtDestructuringDeclarationEntryBasedSymbol(declaration, analysisSession)

        context(analysisSession: KaFirSession)
        fun create(symbol: FirPropertySymbol): KaKotlinPropertySymbol {
            assert(symbol is FirRegularPropertySymbol)
            check(symbol !is FirSyntheticPropertySymbol)
            check(symbol.fir !is FirSyntheticProperty)

            return when (val psi = symbol.fir.getAllowedPsi()) {
                is KtProperty -> create(psi)
                is KtParameter -> create(psi)
                is KtDestructuringDeclarationEntry -> create(psi)
                else -> KaFirKotlinPropertyKtPropertyBasedSymbol(symbol, analysisSession)
            }
        }

        // These constants are used to reduce the chance of clash with the original symbol
        const val HASH_CODE_ADDITION_FOR_BACKING_FIELD: Int = 2
        const val HASH_CODE_ADDITION_FOR_GETTER: Int = 3
        const val HASH_CODE_ADDITION_FOR_SETTER: Int = 4
    }
}

/**
 * Represents [KtProperty]-based symbol or [FirPropertySymbol] without any PSI
 */
private class KaFirKotlinPropertyKtPropertyBasedSymbol : KaFirKotlinPropertySymbol<KtProperty> {
    constructor(symbol: FirPropertySymbol, session: KaFirSession) : super(
        backingPsi = null,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    constructor(declaration: KtProperty, session: KaFirSession) : super(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, analysisSession = session),
        analysisSession = session,
    ) {
        require(!declaration.isLocal)
    }

    override val location: KaSymbolLocation
        get() = entryPointImplementation {
            when {
                backingPsi != null -> backingPsi.location
                firSymbol.origin == FirDeclarationOrigin.DynamicScope -> KaSymbolLocation.CLASS
                firSymbol.containingClassLookupTag()?.classId == null -> KaSymbolLocation.TOP_LEVEL
                else -> KaSymbolLocation.CLASS
            }
        }

    override val isDelegated: Boolean
        get() = entryPointImplementation {
            backingPsi?.hasDelegate() ?: firSymbol.isDelegatedProperty
        }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = entryPointImplementation {
            KaFirReceiverParameterSymbol.create(backingPsi, this, analysisSession = analysisSession)
        }

    override val contextParameters: List<KaContextParameterSymbol>
        get() = entryPointImplementation {
            createKaContextParameters() ?: firSymbol.createKaContextParameters()
        }

    override val isVal: Boolean
        get() = entryPointImplementation {
            if (backingPsi != null)
                !backingPsi.isVar
            else
                firSymbol.isVal
        }

    override val isStatic: Boolean
        get() = entryPointImplementation {
            // Kotlin doesn't have static properties
            if (backingPsi != null || origin == KaSymbolOrigin.SOURCE)
                false
            else
                firSymbol.isStatic
        }

    @OptIn(KtExperimentalApi::class)
    override val isCompanion: Boolean
        get() = entryPointImplementation {
            backingPsi?.isCompanion ?: firSymbol.isStatic
        }

    override val hasGetter: Boolean
        get() = entryPointImplementation { backingPsi != null || firSymbol.getterSymbol != null }

    override val getter: KaPropertyGetterSymbol?
        get() = entryPointImplementation {
            when {
                backingPsi != null ->
                    if (backingPsi.hasRegularGetter)
                        KaFirPropertyGetterSymbol(this)
                    else
                        KaFirDefaultPropertyGetterSymbol(this)

                firSymbol.getterSymbol != null ->
                    if (firSymbol.getterSymbol?.fir is FirDefaultPropertyAccessor)
                        KaFirDefaultPropertyGetterSymbol(this)
                    else
                        KaFirPropertyGetterSymbol(this)

                else -> null
            }
        }

    override val hasSetter: Boolean
        get() = entryPointImplementation {
            if (backingPsi != null)
                backingPsi.isVar || backingPsi.setter != null
            else
                firSymbol.setterSymbol != null
        }

    override val setter: KaPropertySetterSymbol?
        get() = entryPointImplementation {
            when {
                backingPsi != null -> when {
                    backingPsi.hasRegularSetter -> KaFirPropertySetterSymbol(this)
                    backingPsi.isVar || backingPsi.setter != null -> KaFirDefaultPropertySetterSymbol(this)
                    else -> null
                }

                firSymbol.setterSymbol != null ->
                    if (firSymbol.setterSymbol?.fir is FirDefaultPropertyAccessor)
                        KaFirDefaultPropertySetterSymbol(this)
                    else
                        KaFirPropertySetterSymbol(this)

                else -> null
            }
        }

    override val modalityByPsi: KaSymbolModality?
        get() = entryPointImplementation {
            backingPsi?.run {
                val modalityByModifiers = kaSymbolModalityByModifiers
                when {
                    modalityByModifiers != null -> when {
                        // KT-80178: interface members with no body have implicit ABSTRACT modality
                        modalityByModifiers.isOpenFromInterface && !hasBody() -> KaSymbolModality.ABSTRACT
                        else -> modalityByModifiers
                    }

                    // Green code cannot have those modifiers with other modalities
                    hasModifier(KtTokens.CONST_KEYWORD) -> KaSymbolModality.FINAL

                    else -> psiBasedDefaultKaModality(::isOverride)
                }
            }
        }

    override val callableId: CallableId?
        get() = entryPointImplementation {
            if (backingPsi != null)
                backingPsi.callableId
            else
                firSymbol.getCallableId()
        }

    override val initializer: KaInitializerValue?
        get() = entryPointImplementation {
            if (backingPsi?.hasInitializer() == false) {
                return null
            }

            firSymbol.getKtConstantInitializer()
        }

    @Deprecated(
        "Use `KaKotlinProperty.primaryConstructorParameter` instead.",
        ReplaceWith("primaryConstructorParameter != null")
    )
    override val isFromPrimaryConstructor: Boolean
        get() = entryPointImplementation {
            if (backingPsi != null)
                false
            else
                firSymbol.fir.fromPrimaryConstructor == true
        }

    override val primaryConstructorParameter: KaValueParameterSymbol?
        get() = entryPointImplementation {
            if (backingPsi != null) {
                return null
            }
            val firValueParameter = firSymbol.correspondingValueParameterFromPrimaryConstructor ?: return@entryPointImplementation null
            return KaFirValueParameterSymbol(firValueParameter, analysisSession)
        }

    override val isExternal: Boolean
        get() = entryPointImplementation {
            backingPsi?.isExternalDeclaration ?: firSymbol.isEffectivelyExternal(analysisSession.firSession)
        }

    // NB: `field` in accessors indicates the property should have a backing field. To see that, though, we need BODY_RESOLVE.
    override val hasBackingField: Boolean
        get() = entryPointImplementation {
            if (backingPsi != null) {
                val fastAnswer = when {
                    backingPsi.isExpectDeclaration() -> false
                    backingPsi.hasModifier(KtTokens.ABSTRACT_KEYWORD) -> false

                    // Compiled properties might have both `hasBackingField = true` and `isDelegatedProperty = true` at the same time
                    backingPsi.hasDelegate() -> if (backingPsi.cameFromKotlinLibrary) null else false
                    backingPsi.fieldDeclaration != null -> true
                    !backingPsi.hasModifier(KtTokens.FINAL_KEYWORD) && (backingPsi.containingClassOrObject as? KtClass)?.isInterface() == true -> false
                    !backingPsi.hasRegularGetter -> true
                    backingPsi.isVar && !backingPsi.hasRegularSetter -> true
                    else -> null
                }

                fastAnswer?.let { return it }
            }

            firSymbol.hasBackingField
        }
}

/**
 * Represent a property symbol which is generated from [KtParameter]
 */
private class KaFirKotlinPropertyKtParameterBasedSymbol : KaFirKotlinPropertySymbol<KtParameter> {
    constructor(declaration: KtParameter, session: KaFirSession) : super(
        backingPsi = declaration,
        lazyFirSymbol = lazyPub<FirPropertySymbol> {
            val parameterSymbol = declaration.resolveToFirSymbolOfType<FirValueParameterSymbol>(session.resolutionFacade)
            val propertySymbol = parameterSymbol.fir.correspondingProperty?.symbol
            requireWithAttachment(
                propertySymbol != null,
                { "Corresponding property should not be null for parameter" },
            ) {
                withPsiEntry("parameterPsi", declaration)
                withFirSymbolEntry("parameterSymbol", parameterSymbol)
            }

            propertySymbol
        },
        analysisSession = session,
    ) {
        val ownerFunction = declaration.ownerFunction
        requireWithAttachment(
            ownerFunction is KtPrimaryConstructor,
            { "Unexpected owner function: ${ownerFunction?.let { it::class.simpleName }}" },
        ) {
            withPsiEntry("declaration", declaration)
        }

        require(declaration.hasValOrVar())
    }

    /**
     * [KtParameter] in hands isn't enough to decide whether the parameter or generated property should be obtained
     */
    override val realPsi: PsiElement?
        get() = entryPointImplementation { null }

    override val location: KaSymbolLocation
        get() = entryPointImplementation { KaSymbolLocation.CLASS }

    override val isDelegated: Boolean
        get() = entryPointImplementation { false }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = entryPointImplementation { null }

    override val isVal: Boolean
        get() = entryPointImplementation {
            if (backingPsi != null)
                !backingPsi.isMutable
            else
                firSymbol.isVal
        }

    override val hasGetter: Boolean
        get() = entryPointImplementation { true }

    override val isStatic: Boolean
        get() = entryPointImplementation { false }

    @KaExperimentalApi
    override val isCompanion: Boolean
        get() = entryPointImplementation { false }

    override val getter: KaPropertyGetterSymbol?
        get() = entryPointImplementation { KaFirDefaultPropertyGetterSymbol(this) }

    override val hasSetter: Boolean
        get() = entryPointImplementation { !isVal }

    override val setter: KaPropertySetterSymbol?
        get() = entryPointImplementation {
            if (hasSetter)
                KaFirDefaultPropertySetterSymbol(this)
            else
                null
        }

    override val modalityByPsi: KaSymbolModality?
        get() = entryPointImplementation {
            backingPsi?.run {
                kaSymbolModalityByModifiers ?: psiBasedDefaultKaModality(::isOverride)
            }
        }

    override val callableId: CallableId?
        get() = entryPointImplementation {
            if (backingPsi != null)
                backingPsi.callableIdForName(backingPsi.nameAsSafeName)
            else
                firSymbol.getCallableId()
        }

    /**
     * We treat the corresponding parameter as the initializer.
     *
     * Effectively, the code below
     * ```kotlin
     * class Foo(val param: Int)
     * ```
     * is syntax sugar for
     * ```kotlin
     * class Foo(param: Int) {
     *   val param: Int = param
     * }
     * ```
     */
    override val initializer: KaInitializerValue?
        get() = entryPointImplementation {
            backingPsi?.takeIf { it.containingClassOrObject?.isAnnotation() != true }
                ?.let(::KaNonConstantInitializerValue) ?: firSymbol.getKtConstantInitializer()
        }

    @Deprecated(
        "Use `correspondingPrimaryConstructorParameter` instead.",
        replaceWith = ReplaceWith("correspondingPrimaryConstructorParameter != null")
    )
    override val isFromPrimaryConstructor: Boolean
        get() = entryPointImplementation { true }

    override val primaryConstructorParameter: KaValueParameterSymbol?
        get() = entryPointImplementation {
            if (backingPsi != null) {
                return KaFirValueParameterSymbol(backingPsi, analysisSession)
            }
            val firValueParameter = firSymbol.correspondingValueParameterFromPrimaryConstructor ?: return@entryPointImplementation null
            return KaFirValueParameterSymbol(firValueParameter, analysisSession)
        }

    override val isExternal: Boolean
        get() = entryPointImplementation {
            // a generated primary constructor property is external if its containing class is external (it can't be external by itself)
            backingPsi?.containingClassOrObject?.isExternalDeclaration ?: firSymbol.isEffectivelyExternal(analysisSession.firSession)
        }

    override val hasBackingField: Boolean
        get() = entryPointImplementation {
            // A compiled annotation class declares its properties as constructor parameters, and at least on Kotlin/JVM
            // the backing field of such a property is not materialized
            val parameter = backingPsi
            if (parameter != null && parameter.cameFromKotlinLibrary && parameter.containingClassOrObject?.isAnnotation() == true) {
                firSymbol.hasBackingField
            } else {
                true
            }
        }
}

/**
 * Represent a property symbol which is generated from a non-local [org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry]:
 * on top-level of [org.jetbrains.kotlin.psi.KtScript], or on top-level of a file or in a class, where such declarations are not allowed
 */
private class KaFirKotlinPropertyKtDestructuringDeclarationEntryBasedSymbol : KaFirKotlinPropertySymbol<KtDestructuringDeclarationEntry> {
    constructor(declaration: KtDestructuringDeclarationEntry, session: KaFirSession) : super(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, analysisSession = session),
        analysisSession = session,
    )

    override val location: KaSymbolLocation
        get() = entryPointImplementation {
            if (backingPsi?.containingClassOrObject != null || firSymbol.dispatchReceiverType != null) {
                KaSymbolLocation.CLASS
            } else {
                KaSymbolLocation.TOP_LEVEL
            }
        }

    override val name: Name
        get() = entryPointImplementation { backingPsi?.entryName ?: firSymbol.name }

    override val isDelegated: Boolean
        get() = entryPointImplementation { false }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = entryPointImplementation { null }

    override val isVal: Boolean
        get() = entryPointImplementation {
            if (backingPsi != null)
                !backingPsi.isVar
            else
                firSymbol.isVal
        }

    override val isStatic: Boolean
        get() = entryPointImplementation { false }

    @KaExperimentalApi
    override val isCompanion: Boolean
        get() = entryPointImplementation { false }

    override val isOverride: Boolean
        get() = entryPointImplementation { false }

    override val hasGetter: Boolean
        get() = entryPointImplementation { true }

    override val getter: KaPropertyGetterSymbol?
        get() = entryPointImplementation { KaFirDefaultPropertyGetterSymbol(this) }

    override val hasSetter: Boolean
        get() = entryPointImplementation { !isVal }

    override val setter: KaPropertySetterSymbol?
        get() = entryPointImplementation {
            if (hasSetter)
                KaFirDefaultPropertySetterSymbol(this)
            else
                null
        }

    override val modalityByPsi: KaSymbolModality
        get() = entryPointImplementation { KaSymbolModality.FINAL }

    override val compilerVisibilityByPsi: Visibility
        get() = entryPointImplementation { Visibilities.Public }

    override val callableId: CallableId?
        get() = entryPointImplementation {
            if (backingPsi != null)
                backingPsi.callableIdForName(name)
            else
                firSymbol.getCallableId()
        }

    /**
     * Effectively, the code below
     * ```kotlin
     * val pair = 1 to 2
     * val (first, second) = pair
     * ```
     * is syntax sugar for
     * ```kotlin
     * val pair = 1 to 2
     * val first = pair.component1()
     * val second = pair.component2()
     * ```
     * so the initializer should be `pair.component1()`, but this expression doesn't exist in the code.
     * It is unclear which behavior we should have, so for now it aligned with [getKtConstantInitializer].
     */
    override val initializer: KaInitializerValue?
        get() = entryPointImplementation {
            backingPsi?.let(::KaNonConstantInitializerValue) ?: firSymbol.getKtConstantInitializer()
        }

    @Deprecated(
        "Use `correspondingPrimaryConstructorParameter` instead.",
        replaceWith = ReplaceWith("correspondingPrimaryConstructorParameter != null")
    )
    override val isFromPrimaryConstructor: Boolean
        get() = entryPointImplementation { false }

    override val primaryConstructorParameter: KaValueParameterSymbol?
        get() = entryPointImplementation { null }

    override val isExternal: Boolean
        get() = entryPointImplementation { false }

    /** KT-70766 */
    override val hasBackingField: Boolean
        get() = entryPointImplementation { true }

    /** KT-70766 */
    override val backingFieldSymbol: KaBackingFieldSymbol?
        get() = entryPointImplementation { null }
}
