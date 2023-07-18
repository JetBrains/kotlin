/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtInitializerValue
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.*
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.UnsupportedSymbolKind
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KtFirKotlinPropertySymbol(
    override val firSymbol: FirPropertySymbol,
    override val analysisSession: KtFirAnalysisSession,
) : KtKotlinPropertySymbol(), KtFirSymbol<FirPropertySymbol> {
    init {
        assert(!firSymbol.isLocal)
        check(firSymbol !is FirSyntheticPropertySymbol)
        check(firSymbol.fir !is FirSyntheticProperty)
    }

    override val isDelegatedProperty: Boolean
        get() = withValidityAssertion { firSymbol.delegateFieldSymbol != null }

    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val isVal: Boolean get() = withValidityAssertion { firSymbol.isVal }
    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverParameter: KtReceiverParameterSymbol? get() = withValidityAssertion { firSymbol.receiver(builder) }

    override val contextReceivers: List<KtContextReceiver> by cached { firSymbol.createContextReceivers(builder) }

    override val isExtension: Boolean get() = withValidityAssertion { firSymbol.isExtension }
    override val initializer: KtInitializerValue? by cached { firSymbol.getKtConstantInitializer(analysisSession.firResolveSession) }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion {
            when (firSymbol.containingClassLookupTag()?.classId) {
                null -> KtSymbolKind.TOP_LEVEL
                else -> KtSymbolKind.CLASS_MEMBER
            }
        }

    override val modality: Modality get() = withValidityAssertion { firSymbol.modalityOrFinal }
    override val visibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override val annotationsList by cached {
        KtFirAnnotationListForDeclaration.create(
            firSymbol,
            analysisSession.useSiteSession,
            token
        )
    }

    override val callableIdIfNonLocal: CallableId? get() = withValidityAssertion { firSymbol.getCallableIdIfNonLocal() }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { firSymbol.createKtTypeParameters(builder) }

    override val getter: KtPropertyGetterSymbol?
        get() = withValidityAssertion {
            firSymbol.getterSymbol?.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as? KtPropertyGetterSymbol
        }

    override val setter: KtPropertySetterSymbol?
        get() = withValidityAssertion {
            firSymbol.setterSymbol?.let { builder.callableBuilder.buildPropertyAccessorSymbol(it) } as? KtPropertySetterSymbol
        }
    override val backingFieldSymbol: KtBackingFieldSymbol?
        get() = withValidityAssertion {
            firSymbol.backingFieldSymbol?.let { builder.callableBuilder.buildBackingFieldSymbol(it) }
        }

    // NB: `field` in accessors indicates the property should have a backing field. To see that, though, we need BODY_RESOLVE.
    override val hasBackingField: Boolean
        get() = withValidityAssertion {
            firSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            firSymbol.fir.hasBackingField
        }

    override val isLateInit: Boolean get() = withValidityAssertion { firSymbol.isLateInit }


    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion {
            firSymbol.fir.fromPrimaryConstructor == true || firSymbol.source?.kind == KtFakeSourceElementKind.PropertyFromParameter
        }

    override val isOverride: Boolean get() = withValidityAssertion { firSymbol.isOverride }
    override val isConst: Boolean get() = withValidityAssertion { firSymbol.isConst }
    override val isStatic: Boolean get() = withValidityAssertion { firSymbol.isStatic }
    override val isActual: Boolean get() = withValidityAssertion { firSymbol.isActual }
    override val isExpect: Boolean get() = withValidityAssertion { firSymbol.isExpect }

    override val hasGetter: Boolean get() = withValidityAssertion { firSymbol.getterSymbol != null }
    override val hasSetter: Boolean get() = withValidityAssertion { firSymbol.setterSymbol != null }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtKotlinPropertySymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtVariableLikeSymbol>(this)?.let { psiPointer ->
            return KtFirPsiBasedPropertySymbolPointer(psiPointer)
        }

        return when (val kind = symbolKind) {
            KtSymbolKind.TOP_LEVEL -> KtFirTopLevelPropertySymbolPointer(
                firSymbol.callableId,
                FirCallableSignature.createSignature(firSymbol),
            )

            KtSymbolKind.CLASS_MEMBER ->
                KtFirMemberPropertySymbolPointer(
                    ownerPointer = requireOwnerPointer(),
                    name = firSymbol.name,
                    signature = FirCallableSignature.createSignature(firSymbol),
                    isStatic = firSymbol.isStatic,
                )

            else -> throw UnsupportedSymbolKind(this::class, kind)
        }
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}

