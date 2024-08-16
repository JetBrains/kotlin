/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.*
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaUnsupportedSymbolLocation
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
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal sealed class KaFirKotlinPropertySymbol<P : KtTypeParameterListOwner>(
    final override val backingPsi: P?,
    final override val analysisSession: KaFirSession,
    final override val lazyFirSymbol: Lazy<FirPropertySymbol>,
) : KaKotlinPropertySymbol(), KaFirKtBasedSymbol<P, FirPropertySymbol> {
    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: firSymbol.findPsi() }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { firSymbol.createContextReceivers(builder) }

    override val isExtension: Boolean
        get() = withValidityAssertion { backingPsi?.isExtensionDeclaration() ?: firSymbol.isExtension }

    override val initializer: KaInitializerValue?
        get() = withValidityAssertion { firSymbol.getKtConstantInitializer(builder) }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion {
            createKaTypeParameters() ?: firSymbol.createKtTypeParameters(builder)
        }

    override val getter: KaPropertyGetterSymbol?
        get() = withValidityAssertion {
            if (backingPsi == null || hasGetter)
                firSymbol.getterSymbol?.let { builder.functionBuilder.buildGetterSymbol(it) }
            else
                null
        }

    override val setter: KaPropertySetterSymbol?
        get() = withValidityAssertion {
            if (backingPsi == null || hasSetter)
                firSymbol.setterSymbol?.let { builder.functionBuilder.buildSetterSymbol(it) }
            else
                null
        }

    override val backingFieldSymbol: KaBackingFieldSymbol?
        get() = withValidityAssertion {
            firSymbol.backingFieldSymbol?.let { builder.variableBuilder.buildBackingFieldSymbol(it) }
        }

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
            return KaFirPsiBasedPropertySymbolPointer(it)
        }

        when (val kind = location) {
            KaSymbolLocation.TOP_LEVEL -> {
                if (firSymbol.fir.origin is FirDeclarationOrigin.ScriptCustomization.ResultProperty) {
                    KaFirResultPropertySymbolPointer(analysisSession.createOwnerPointer(this))
                } else {
                    KaFirTopLevelPropertySymbolPointer(
                        firSymbol.callableId,
                        FirCallableSignature.createSignature(firSymbol),
                    )
                }
            }

            KaSymbolLocation.CLASS ->
                KaFirMemberPropertySymbolPointer(
                    ownerPointer = analysisSession.createOwnerPointer(this),
                    name = firSymbol.name,
                    signature = FirCallableSignature.createSignature(firSymbol),
                    isStatic = firSymbol.isStatic,
                )

            else -> throw KaUnsupportedSymbolLocation(this::class, kind)
        }
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()

    companion object {
        operator fun invoke(
            declaration: KtProperty,
            session: KaFirSession,
        ): KaKotlinPropertySymbol = KaFirKotlinPropertyKtPropertyBasedSymbol(declaration, session)

        operator fun invoke(
            declaration: KtParameter,
            session: KaFirSession,
        ): KaKotlinPropertySymbol = KaFirKotlinPropertyKtParameterBasedSymbol(declaration, session)

        operator fun invoke(
            declaration: KtDestructuringDeclarationEntry,
            session: KaFirSession,
        ): KaKotlinPropertySymbol = KaFirKotlinPropertyKtDestructuringDeclarationEntryBasedSymbol(declaration, session)

        operator fun invoke(symbol: FirPropertySymbol, session: KaFirSession): KaKotlinPropertySymbol {
            assert(!symbol.isLocal)
            check(symbol !is FirSyntheticPropertySymbol)
            check(symbol.fir !is FirSyntheticProperty)

            return when (val psi = symbol.fir.getAllowedPsi()) {
                is KtProperty -> this(psi, session)
                is KtParameter -> this(psi, session)
                is KtDestructuringDeclarationEntry -> this(psi, session)
                else -> KaFirKotlinPropertyKtPropertyBasedSymbol(symbol, session)
            }
        }
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
            KaFirReceiverParameterSymbol(backingPsi, analysisSession, this)
        }

    override val isVal: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                !backingPsi.isVar
            else
                firSymbol.isVal
        }

    override val hasGetter: Boolean
        get() = withValidityAssertion { backingPsi != null || getter != null }

    override val hasSetter: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.isVar || backingPsi.setter != null
            else
                setter != null
        }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { backingPsi?.kaSymbolModality ?: firSymbol.kaSymbolModality }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { backingPsi?.visibility ?: firSymbol.visibility }

    override val callableId: CallableId?
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.callableId
            else
                firSymbol.getCallableId()
        }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                false
            else
                firSymbol.fir.fromPrimaryConstructor == true
        }

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

    override val hasSetter: Boolean
        get() = withValidityAssertion { !isVal }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { backingPsi?.kaSymbolModalityByModifiers ?: firSymbol.kaSymbolModality }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { backingPsi?.visibilityByModifiers ?: firSymbol.visibility }

    override val callableId: CallableId?
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.callableIdForName(backingPsi.nameAsSafeName)
            else
                firSymbol.getCallableId()
        }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion { true }

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

    override val hasSetter: Boolean
        get() = withValidityAssertion { !isVal }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { KaSymbolModality.FINAL }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { Visibilities.Public }

    override val callableId: CallableId?
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.callableIdForName(name)
            else
                firSymbol.getCallableId()
        }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion { false }

    /** KT-70766 */
    override val hasBackingField: Boolean
        get() = withValidityAssertion { true }

    /** KT-70766 */
    override val backingFieldSymbol: KaBackingFieldSymbol?
        get() = withValidityAssertion { null }
}
