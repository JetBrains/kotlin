/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaPropertySetterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.name.CallableId

internal class KaFirPropertySetterSymbol(
    override val firSymbol: FirPropertyAccessorSymbol,
    override val analysisSession: KaFirSession,
) : KaPropertySetterSymbol(), KaFirSymbol<FirPropertyAccessorSymbol> {

    init {
        require(firSymbol.isSetter)
    }

    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val isDefault: Boolean get() = withValidityAssertion { firSymbol.fir is FirDefaultPropertyAccessor }
    override val isInline: Boolean get() = withValidityAssertion { firSymbol.isInline }
    override val isOverride: Boolean
        get() = withValidityAssertion {
            if (firSymbol.isOverride) return true
            val propertySymbol = firSymbol.fir.propertySymbol
            if (!propertySymbol.isOverride) return false
            val session = analysisSession.useSiteSession
            val containingClassScope = firSymbol.dispatchReceiverType?.scope(
                session,
                analysisSession.getScopeSessionFor(session),
                CallableCopyTypeCalculator.DoNothing,
                requiredMembersPhase = FirResolvePhase.STATUS,
            ) ?: return false

            val overriddenProperties = containingClassScope.getDirectOverriddenProperties(propertySymbol)
            overriddenProperties.any { it.isVar }
        }

    override val hasBody: Boolean get() = withValidityAssertion { firSymbol.fir.hasBody }

    override val modality: Modality get() = withValidityAssertion { firSymbol.modality }
    override val visibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override val annotationsList by cached {
        KaFirAnnotationListForDeclaration.create(firSymbol, builder)
    }

    /**
     * Returns [CallableId] of the delegated Java method if the corresponding property of this setter is a synthetic Java property.
     * Otherwise, returns `null`
     */
    override val callableId: CallableId? by cached {
        val fir = firSymbol.fir
        if (fir is FirSyntheticPropertyAccessor) {
            fir.delegate.symbol.callableId
        } else null
    }

    override val parameter: KaValueParameterSymbol by cached {
        firSymbol.createKtValueParameters(builder).single()
    }

    override val valueParameters: List<KaValueParameterSymbol> by cached { listOf(parameter) }

    override val returnType: KaType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { firSymbol.fir.propertySymbol.receiver(builder) }


    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    @OptIn(KaAnalysisApiInternals::class)
    override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaPropertySetterSymbol>(this)
            ?: KaPropertySetterSymbolPointer(analysisSession.createOwnerPointer(this))
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
