/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForReceiverParameter
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirReceiverParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

internal class KaFirReceiverParameterSymbol(
    val firSymbol: FirCallableSymbol<*>,
    val analysisSession: KaFirSession,
) : KaReceiverParameterSymbol(), KaLifetimeOwner {
    override val token: KaLifetimeToken get() = analysisSession.token
    override val psi: PsiElement? = withValidityAssertion { firSymbol.fir.receiverParameter?.typeRef?.psi }

    init {
        requireWithAttachment(firSymbol.fir.receiverParameter != null, { "${firSymbol::class} doesn't have an extension receiver." }) {
            withFirEntry("callable", firSymbol.fir)
        }
    }

    override val returnType: KaType by cached {
        firSymbol.receiverType(analysisSession.firSymbolBuilder)
            ?: errorWithAttachment("${firSymbol::class} doesn't have an extension receiver") {
                withFirEntry("callable", firSymbol.fir)
            }
    }

    override val owningCallableSymbol: KaCallableSymbol by cached { analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(firSymbol) }

    override val origin: KaSymbolOrigin = withValidityAssertion { firSymbol.fir.ktSymbolOrigin() }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { firSymbol.kaSymbolModality }

    @KaExperimentalApi
    override val compilerVisibility: Visibility
        get() = withValidityAssertion { firSymbol.visibility }

    override val isActual: Boolean
        get() = withValidityAssertion { false }

    override val isExpect: Boolean
        get() = withValidityAssertion { false }

    override val name: Name
        get() = withValidityAssertion { SpecialNames.RECEIVER }

    override fun createPointer(): KaSymbolPointer<KaReceiverParameterSymbol> = withValidityAssertion {
        KaFirReceiverParameterSymbolPointer(owningCallableSymbol.createPointer())
    }

    override val annotations: KaAnnotationList by cached {
        KaFirAnnotationListForReceiverParameter.create(firSymbol, builder = analysisSession.firSymbolBuilder)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KaFirReceiverParameterSymbol && this.firSymbol == other.firSymbol
    }

    override fun hashCode(): Int = 31 * firSymbol.hashCode()
}
