/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirReceiverParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.requireOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

internal class KtFirReceiverParameterSymbol(
    val firSymbol: FirCallableSymbol<*>,
    val firResolveSession: LLFirResolveSession,
    override val token: KtLifetimeToken,
    private val builder: KtSymbolByFirBuilder
) : KtReceiverParameterSymbol(), KtLifetimeOwner {
    override val psi: PsiElement? by cached { firSymbol.fir.receiverTypeRef?.findPsi(firSymbol.fir.moduleData.session) }

    init {
        require(firSymbol.fir.receiverTypeRef != null) { "$firSymbol doesn't have an extension receiver." }
    }

    override val type: KtType by cached {
        firSymbol.receiverType(builder)
            ?: error("$firSymbol doesn't have an extension receiver.")
    }

    override val owningCallableSymbol: KtCallableSymbol by cached { builder.callableBuilder.buildCallableSymbol(firSymbol) }

    override val origin: KtSymbolOrigin = withValidityAssertion { firSymbol.fir.ktSymbolOrigin() }

    override fun createPointer(): KtSymbolPointer<KtReceiverParameterSymbol> = withValidityAssertion {
        KtFirReceiverParameterSymbolPointer(owningCallableSymbol.createPointer())
    }
}
