/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.KaNonConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.*
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaUnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.impl.base.util.callableId
import org.jetbrains.kotlin.analysis.api.impl.base.util.callableIdForName
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
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
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal sealed class KaFirKotlinPropertySymbol<P : KtCallableDeclaration>(
    final override val backingPsi: P?,
    final override val analysisSession: KaFirSession,
    final override val lazyFirSymbol: Lazy<FirPropertySymbol>,
) : KaKotlinPropertySymbol(), KaFirKtBasedSymbol<P, FirPropertySymbol> {
    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { createContextReceivers() }

    override val contextParameters: List<KaContextParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    override val isExtension: Boolean
        get() = withValidityAssertion { backingPsi?.isExtensionDeclaration() ?: firSymbol.isExtension }

    val isInline: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.INLINE_KEYWORD) ?: firSymbol.isInline }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion {
            createKaTypeParameters() ?: firSymbol.createKtTypeParameters(builder)
        }

    abstract val compilerVisibilityByPsi: Visibility?

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { compilerVisibilityByPsi ?: firSymbol.visibility }

    abstract val modalityByPsi: KaSymbolModality?

    override val modality: KaSymbolModality
        get() = withValidityAssertion { modalityByPsi ?: firSymbol.kaSymbolModality }

    override val backingFieldSymbol: KaBackingFieldSymbol?
        get() = withValidityAssertion { KaFirBackingFieldSymbol.create(this) }

    override val isLateInit: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.hasModifier(KtTokens.LATEINIT_KEYWORD)
            else
                firSymbol.isLateInit
        }

    override val isOverride: Boolean
        get() = withValidityAssertion {
            ifSource { backingPsi }?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: firSymbol.isOverride
        }

    override val isConst: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.hasModifier(KtTokens.CONST_KEYWORD)
            else
                firSymbol.isConst
        }

    override val isStatic: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                false
            else
                firSymbol.isStatic
        }

    override val isActual: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.ACTUAL_KEYWORD) ?: firSymbol.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect }

    override fun createPointer(): KaSymbolPointer<KaKotlinPropertySymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaVariableSymbol>()?.let {
            return KaFirPsiBasedPropertySymbolPointer(it, this)
        }

        when (val kind = location) {
            KaSymbolLocation.TOP_LEVEL -> {
                if (firSymbol.fir.origin is FirDeclarationOrigin.ScriptCustomization.ResultProperty) {
                    KaFirResultPropertySymbolPointer(analysisSession.createOwnerPointer(this), this)
                } else {
                    KaFirTopLevelPropertySymbolPointer(
                        firSymbol.callableId,
                        FirCallableSignature.createSignature(firSymbol),
                        this
                    )
                }
            }

            KaSymbolLocation.CLASS -> when (origin) {
                KaSymbolOrigin.JS_DYNAMIC -> KaFirDynamicPropertySymbolPointer(name, this)
                else -> KaFirMemberPropertySymbolPointer(
                    ownerPointer = analysisSession.createOwnerPointer(this),
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
        fun create(
            declaration: KtProperty,
            session: KaFirSession,
        ): KaKotlinPropertySymbol = KaFirKotlinPropertyKtPropertyBasedSymbol(declaration, session)

        fun create(
            declaration: KtParameter,
            session: KaFirSession,
        ): KaKotlinPropertySymbol = KaFirKotlinPropertyKtParameterBasedSymbol(declaration, session)

        fun create(
            declaration: KtDestructuringDeclarationEntry,
            session: KaFirSession,
        ): KaKotlinPropertySymbol = KaFirKotlinPropertyKtDestructuringDeclarationEntryBasedSymbol(declaration, session)

        fun create(symbol: FirPropertySymbol, session: KaFirSession): KaKotlinPropertySymbol {
            assert(!symbol.isLocal)
            check(symbol !is FirSyntheticPropertySymbol)
            check(symbol.fir !is FirSyntheticProperty)

            return when (val psi = symbol.fir.getAllowedPsi()) {
                is KtProperty -> create(psi, session)
                is KtParameter -> create(psi, session)
                is KtDestructuringDeclarationEntry -> create(psi, session)
                else -> KaFirKotlinPropertyKtPropertyBasedSymbol(symbol, session)
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
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    ) {
        require(!declaration.isLocal)
    }

    override val location: KaSymbolLocation
        get() = withValidityAssertion {
            when {
                backingPsi != null -> backingPsi.location
                firSymbol.origin == FirDeclarationOrigin.DynamicScope -> KaSymbolLocation.CLASS
                firSymbol.containingClassLookupTag()?.classId == null -> KaSymbolLocation.TOP_LEVEL
                else -> KaSymbolLocation.CLASS
            }
        }

    override val isDelegatedProperty: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.hasDelegate()
            else
                firSymbol.delegateFieldSymbol != null
        }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion {
            KaFirReceiverParameterSymbol.create(backingPsi, analysisSession, this)
        }

    override val contextParameters: List<KaContextParameterSymbol>
        get() = withValidityAssertion {
            createKaContextParameters() ?: firSymbol.createKaContextParameters(builder)
        }

    override val isVal: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                !backingPsi.isVar
            else
                firSymbol.isVal
        }

    override val hasGetter: Boolean
        get() = withValidityAssertion { backingPsi != null || firSymbol.getterSymbol != null }

    override val getter: KaPropertyGetterSymbol?
        get() = withValidityAssertion {
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
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.isVar || backingPsi.setter != null
            else
                firSymbol.setterSymbol != null
        }

    override val setter: KaPropertySetterSymbol?
        get() = withValidityAssertion {
            when {
                backingPsi != null -> when {
                    backingPsi.hasRegularSetter -> KaFirPropertySetterSymbol(this)
                    backingPsi.isVar -> KaFirDefaultPropertySetterSymbol(this)
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
        get() = withValidityAssertion { backingPsi?.kaSymbolModality }

    override val compilerVisibilityByPsi: Visibility?
        get() = withValidityAssertion { backingPsi?.visibility }

    override val callableId: CallableId?
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.callableId
            else
                firSymbol.getCallableId()
        }

    override val initializer: KaInitializerValue?
        get() = withValidityAssertion {
            if (backingPsi?.hasInitializer() == false) {
                return null
            }

            firSymbol.getKtConstantInitializer(builder)
        }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                false
            else
                firSymbol.fir.fromPrimaryConstructor == true
        }

    override val isExternal: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.EXTERNAL_KEYWORD) ?: firSymbol.isExternal }

    // NB: `field` in accessors indicates the property should have a backing field. To see that, though, we need BODY_RESOLVE.
    override val hasBackingField: Boolean
        get() = withValidityAssertion { firSymbol.hasBackingField }
}

/**
 * Represent a property symbol which is generated from [KtParameter]
 */
private class KaFirKotlinPropertyKtParameterBasedSymbol : KaFirKotlinPropertySymbol<KtParameter> {
    constructor(declaration: KtParameter, session: KaFirSession) : super(
        backingPsi = declaration,
        lazyFirSymbol = lazyPub<FirPropertySymbol> {
            val parameterSymbol = declaration.resolveToFirSymbolOfType<FirValueParameterSymbol>(session.firResolveSession)
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

    override val location: KaSymbolLocation
        get() = withValidityAssertion { KaSymbolLocation.CLASS }

    override val isDelegatedProperty: Boolean
        get() = withValidityAssertion { false }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { null }

    override val isVal: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                !backingPsi.isMutable
            else
                firSymbol.isVal
        }

    override val hasGetter: Boolean
        get() = withValidityAssertion { true }

    override val getter: KaPropertyGetterSymbol?
        get() = withValidityAssertion { KaFirDefaultPropertyGetterSymbol(this) }

    override val hasSetter: Boolean
        get() = withValidityAssertion { !isVal }

    override val setter: KaPropertySetterSymbol?
        get() = withValidityAssertion {
            if (hasSetter)
                KaFirDefaultPropertySetterSymbol(this)
            else
                null
        }

    override val modalityByPsi: KaSymbolModality?
        get() = withValidityAssertion { backingPsi?.kaSymbolModalityByModifiers }

    override val compilerVisibilityByPsi: Visibility?
        get() = withValidityAssertion { backingPsi?.visibilityByModifiers }

    override val callableId: CallableId?
        get() = withValidityAssertion {
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
        get() = withValidityAssertion {
            backingPsi?.takeIf { it.containingClassOrObject?.isAnnotation() != true }
                ?.let(::KaNonConstantInitializerValue) ?: firSymbol.getKtConstantInitializer(builder)
        }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion { true }

    override val isExternal: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.EXTERNAL_KEYWORD) ?: firSymbol.isExternal }

    override val hasBackingField: Boolean
        get() = withValidityAssertion { true }
}

/**
 * Represent a property symbol which is generated from [org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry]
 * on top-level of [org.jetbrains.kotlin.psi.KtScript]
 */
private class KaFirKotlinPropertyKtDestructuringDeclarationEntryBasedSymbol : KaFirKotlinPropertySymbol<KtDestructuringDeclarationEntry> {
    constructor(declaration: KtDestructuringDeclarationEntry, session: KaFirSession) : super(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    ) {
        require(declaration.containingKtFile.isScript())
    }

    override val location: KaSymbolLocation
        get() = withValidityAssertion { KaSymbolLocation.TOP_LEVEL }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.entryName ?: firSymbol.name }

    override val isDelegatedProperty: Boolean
        get() = withValidityAssertion { false }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { null }

    override val isVal: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                !backingPsi.isVar
            else
                firSymbol.isVal
        }

    override val hasGetter: Boolean
        get() = withValidityAssertion { true }

    override val getter: KaPropertyGetterSymbol?
        get() = withValidityAssertion { KaFirDefaultPropertyGetterSymbol(this) }

    override val hasSetter: Boolean
        get() = withValidityAssertion { !isVal }

    override val setter: KaPropertySetterSymbol?
        get() = withValidityAssertion {
            if (hasSetter)
                KaFirDefaultPropertySetterSymbol(this)
            else
                null
        }

    override val modalityByPsi: KaSymbolModality?
        get() = withValidityAssertion { KaSymbolModality.FINAL }

    override val compilerVisibilityByPsi: Visibility?
        get() = withValidityAssertion { Visibilities.Public }

    override val callableId: CallableId?
        get() = withValidityAssertion {
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
        get() = withValidityAssertion {
            backingPsi?.let(::KaNonConstantInitializerValue) ?: firSymbol.getKtConstantInitializer(builder)
        }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion { false }

    override val isExternal: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.EXTERNAL_KEYWORD) ?: firSymbol.isExternal }

    /** KT-70766 */
    override val hasBackingField: Boolean
        get() = withValidityAssertion { true }

    /** KT-70766 */
    override val backingFieldSymbol: KaBackingFieldSymbol?
        get() = withValidityAssertion { null }
}
