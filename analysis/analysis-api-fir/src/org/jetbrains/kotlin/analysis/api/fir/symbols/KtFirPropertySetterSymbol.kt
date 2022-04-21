/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.declarations.utils.modalityOrFinal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.getHasStableParameterNames
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.name.CallableId

internal class KtFirPropertySetterSymbol(
    override val firSymbol: FirPropertyAccessorSymbol,
    override val resolveState: LLFirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder,
) : KtPropertySetterSymbol(), KtFirSymbol<FirPropertyAccessorSymbol> {

    init {
        require(firSymbol.isSetter)
    }

    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val isDefault: Boolean get() = withValidityAssertion { firSymbol.fir is FirDefaultPropertyAccessor }
    override val isInline: Boolean get() = withValidityAssertion { firSymbol.isInline }
    override val isOverride: Boolean get() = withValidityAssertion { firSymbol.isOverride }
    override val hasBody: Boolean get() = withValidityAssertion { firSymbol.fir.body != null }

    override val modality: Modality get() = withValidityAssertion { firSymbol.modalityOrFinal }
    override val visibility: Visibility get() = withValidityAssertion { firSymbol.visibility }

    override val annotationsList by cached { KtFirAnnotationListForDeclaration.create(firSymbol, resolveState.rootModuleSession, token) }

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

    override val parameter: KtValueParameterSymbol by cached {
        firSymbol.createKtValueParameters(builder).single()
    }

    override val valueParameters: List<KtValueParameterSymbol> by cached { listOf(parameter) }

    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }
    override val receiverType: KtType? get() = withValidityAssertion { firSymbol.receiverType(builder) }
    

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { firSymbol.fir.getHasStableParameterNames(firSymbol.moduleData.session) }

    override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        TODO("Creating pointers for setters from library is not supported yet")
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
