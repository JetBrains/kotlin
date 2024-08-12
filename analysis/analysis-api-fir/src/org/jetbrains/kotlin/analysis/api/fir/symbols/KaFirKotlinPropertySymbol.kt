/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.callableId
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.kaSymbolModality
import org.jetbrains.kotlin.analysis.api.fir.location
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.*
import org.jetbrains.kotlin.analysis.api.fir.visibility
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaUnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirCallableSignature
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration

internal class KaFirKotlinPropertySymbol private constructor(
    override val backingPsi: KtProperty?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirPropertySymbol>,
) : KaKotlinPropertySymbol(), KaFirKtBasedSymbol<KtProperty, FirPropertySymbol> {
    constructor(declaration: KtProperty, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirPropertySymbol, session: KaFirSession) : this(
        backingPsi = symbol.fir.realPsi as? KtProperty,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    ) {
        assert(!firSymbol.isLocal)
        check(firSymbol !is FirSyntheticPropertySymbol)
        check(firSymbol.fir !is FirSyntheticProperty)
    }

    init {
        require(backingPsi?.isLocal != true)
    }

    override val isDelegatedProperty: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.hasDelegate()
            else
                firSymbol.delegateFieldSymbol != null
        }

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: firSymbol.findPsi() }

    override val isVal: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                !backingPsi.isVar
            else
                firSymbol.isVal
        }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { firSymbol.receiver(builder) }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { firSymbol.createContextReceivers(builder) }

    override val isExtension: Boolean
        get() = withValidityAssertion { backingPsi?.isExtensionDeclaration() ?: firSymbol.isExtension }

    override val initializer: KaInitializerValue?
        get() = withValidityAssertion { firSymbol.getKtConstantInitializer(builder) }

    override val location: KaSymbolLocation
        get() = withValidityAssertion {
            when {
                backingPsi != null -> backingPsi.location
                firSymbol.origin == FirDeclarationOrigin.DynamicScope -> KaSymbolLocation.CLASS
                firSymbol.containingClassLookupTag()?.classId == null -> KaSymbolLocation.TOP_LEVEL
                else -> KaSymbolLocation.CLASS
            }
        }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { backingPsi?.kaSymbolModality ?: firSymbol.kaSymbolModality }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { backingPsi?.visibility ?: firSymbol.visibility }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val callableId: CallableId?
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.callableId
            else
                firSymbol.getCallableId()
        }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }

    override val getter: KaPropertyGetterSymbol?
        get() = withValidityAssertion {
            firSymbol.getterSymbol?.let { builder.functionBuilder.buildPropertyAccessorSymbol(it) } as? KaPropertyGetterSymbol
        }

    override val setter: KaPropertySetterSymbol?
        get() = withValidityAssertion {
            firSymbol.setterSymbol?.let { builder.functionBuilder.buildPropertyAccessorSymbol(it) } as? KaPropertySetterSymbol
        }

    override val backingFieldSymbol: KaBackingFieldSymbol?
        get() = withValidityAssertion {
            firSymbol.backingFieldSymbol?.let { builder.variableBuilder.buildBackingFieldSymbol(it) }
        }

    // NB: `field` in accessors indicates the property should have a backing field. To see that, though, we need BODY_RESOLVE.
    override val hasBackingField: Boolean
        get() = withValidityAssertion {
            firSymbol.hasBackingField
        }

    override val isLateInit: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                backingPsi.hasModifier(KtTokens.LATEINIT_KEYWORD)
            else
                firSymbol.isLateInit
        }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion {
            if (backingPsi != null)
                false
            else
                firSymbol.fir.fromPrimaryConstructor == true
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

    override val hasGetter: Boolean
        get() = withValidityAssertion { firSymbol.getterSymbol != null }

    override val hasSetter: Boolean
        get() = withValidityAssertion { firSymbol.setterSymbol != null }

    override fun createPointer(): KaSymbolPointer<KaKotlinPropertySymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaKotlinPropertySymbol>()?.let { return it }

        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaVariableSymbol>(this)?.let { psiPointer ->
            return KaFirPsiBasedPropertySymbolPointer(psiPointer)
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
}

