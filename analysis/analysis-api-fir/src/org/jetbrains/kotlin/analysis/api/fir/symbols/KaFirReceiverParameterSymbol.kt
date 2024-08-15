/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForReceiverParameter
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirReceiverParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

internal class KaFirReceiverParameterSymbol(
    val firSymbol: FirCallableSymbol<*>,
    val analysisSession: KaFirSession,
) : KaReceiverParameterSymbol() {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override val psi: PsiElement?
        get() = withValidityAssertion { firSymbol.fir.receiverParameter?.typeRef?.psi }

    init {
        requireWithAttachment(firSymbol.fir.receiverParameter != null, { "${firSymbol::class} doesn't have an extension receiver." }) {
            withFirEntry("callable", firSymbol.fir)
        }
    }

    override val returnType: KaType
        get() = withValidityAssertion {
            firSymbol.receiverType(analysisSession.firSymbolBuilder)
                ?: errorWithAttachment("${firSymbol::class} doesn't have an extension receiver") {
                    withFirEntry("callable", firSymbol.fir)
                }
        }

    override val owningCallableSymbol: KaCallableSymbol
        get() = withValidityAssertion {
            analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(firSymbol)
        }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { firSymbol.fir.ktSymbolOrigin() }

    @KaExperimentalApi
    override val compilerVisibility: Visibility
        get() = withValidityAssertion { FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS.visibility }

    override fun createPointer(): KaSymbolPointer<KaReceiverParameterSymbol> = withValidityAssertion {
        KaFirReceiverParameterSymbolPointer(owningCallableSymbol.createPointer())
    }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForReceiverParameter.create(firSymbol, builder = analysisSession.firSymbolBuilder)
        }

    override fun equals(other: Any?): Boolean = this === other ||
            other is KaFirReceiverParameterSymbol &&
            other.firSymbol == this.firSymbol

    override fun hashCode(): Int = 31 * firSymbol.hashCode()
}
