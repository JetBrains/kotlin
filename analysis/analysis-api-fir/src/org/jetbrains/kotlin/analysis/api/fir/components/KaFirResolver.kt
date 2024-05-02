/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.unwrapSafeCall
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.psi.KtElement

internal class KaFirResolver(override val analysisSession: KtFirAnalysisSession) : KaResolver(), KtFirAnalysisSessionComponent {
    override fun resolveKtElementToSymbol(ktElement: KtElement): KtSymbol? {
        val originalFir = ktElement.getOrBuildFir(firResolveSession) ?: return null
        val unwrappedFir = originalFir.unwrapSafeCall()
        val resolvedSymbol = when (unwrappedFir) {
            is FirResolvable -> unwrappedFir.calleeReference.successSymbol
            is FirReference -> unwrappedFir.successSymbol
            is FirArrayLiteral -> with(analysisSession) { arrayOfSymbol(unwrappedFir)?.firSymbol }
            is FirReturnExpression -> unwrappedFir.target.labeledElement.symbol
            else -> null
        } ?: return null

        return analysisSession.firSymbolBuilder.buildSymbol(resolvedSymbol)
    }
}

private val FirReference.successSymbol: FirBasedSymbol<*>?
    get() = when (this) {
        is FirResolvedNamedReference -> toResolvedBaseSymbol(discardErrorReference = true)
        is FirThisReference -> boundSymbol
        else -> null
    }