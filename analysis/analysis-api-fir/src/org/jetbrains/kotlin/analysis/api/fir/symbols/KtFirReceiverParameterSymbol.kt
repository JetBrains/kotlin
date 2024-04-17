/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForReceiverParameter
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirReceiverParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

internal class KtFirReceiverParameterSymbol(
    val firSymbol: FirCallableSymbol<*>,
    val analysisSession: KtFirAnalysisSession,
) : KtReceiverParameterSymbol(), KtLifetimeOwner {
    override val token: KtLifetimeToken get() = analysisSession.token
    override val psi: PsiElement? = withValidityAssertion { firSymbol.fir.receiverParameter?.typeRef?.psi }

    init {
        requireWithAttachment(firSymbol.fir.receiverParameter != null, { "${firSymbol::class} doesn't have an extension receiver." }) {
            withFirEntry("callable", firSymbol.fir)
        }
    }

    override val type: KtType by cached {
        firSymbol.receiverType(analysisSession.firSymbolBuilder)
            ?: errorWithAttachment("${firSymbol::class} doesn't have an extension receiver") {
                withFirEntry("callable", firSymbol.fir)
            }
    }

    override val owningCallableSymbol: KtCallableSymbol by cached { analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(firSymbol) }

    override val origin: KtSymbolOrigin = withValidityAssertion { firSymbol.fir.ktSymbolOrigin() }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtReceiverParameterSymbol> = withValidityAssertion {
        KtFirReceiverParameterSymbolPointer(owningCallableSymbol.createPointer())
    }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForReceiverParameter.create(firSymbol, builder = analysisSession.firSymbolBuilder)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KtFirReceiverParameterSymbol && this.firSymbol == other.firSymbol
    }

    override fun hashCode(): Int = 31 * firSymbol.hashCode()
}
