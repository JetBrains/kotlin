/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirPropertyAccessorSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.requireOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.name.CallableId

internal class KtFirPropertyGetterSymbol(
    override val firSymbol: FirPropertyAccessorSymbol,
    override val firResolveSession: LLFirResolveSession,
    override val token: KtLifetimeToken,
    private val builder: KtSymbolByFirBuilder,
) : KtPropertyGetterSymbol(), KtFirSymbol<FirPropertyAccessorSymbol> {
    init {
        require(firSymbol.isGetter)
    }

    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val isDefault: Boolean get() = withValidityAssertion { firSymbol.fir is FirDefaultPropertyAccessor }
    override val isInline: Boolean get() = withValidityAssertion { firSymbol.isInline }
    override val isOverride: Boolean
        get() = withValidityAssertion {
            if (firSymbol.isOverride) return@withValidityAssertion true
            return firSymbol.fir.propertySymbol.isOverride
        }

    override val hasBody: Boolean get() = withValidityAssertion { firSymbol.fir.hasBody }

    override val modality: Modality get() = withValidityAssertion { firSymbol.modalityOrFinal }
    override val visibility: Visibility get() = withValidityAssertion { firSymbol.visibility }


    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverType: KtType? get() = withValidityAssertion { firSymbol.fir.propertySymbol.receiverType(builder) }

    override val annotationsList by cached { KtFirAnnotationListForDeclaration.create(firSymbol, firResolveSession.useSiteFirSession, token) }

    /**
     * Returns [CallableId] of the delegated Java method if the corresponding property of this setter is a synthetic Java property.
     * Otherwise, returns `null`
     */
    override val callableIdIfNonLocal: CallableId? by cached {
        val fir = firSymbol.fir
        if (fir is FirSyntheticPropertyAccessor) {
            fir.delegate.symbol.callableId
        } else null
    }

    override val valueParameters: List<KtValueParameterSymbol> get() = withValidityAssertion { emptyList() }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override fun createPointer(): KtSymbolPointer<KtPropertyGetterSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }

        @Suppress("UNCHECKED_CAST")
        KtFirPropertyAccessorSymbolPointer(requireOwnerPointer(), true) as KtSymbolPointer<KtPropertyGetterSymbol>
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
