/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirArrayOfSymbolProvider.arrayOfSymbol
import org.jetbrains.kotlin.analysis.api.fir.unwrapSafeCall
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtReferenceExpression

internal class KaFirResolver(override val analysisSession: KtFirAnalysisSession) : KaResolver(), KtFirAnalysisSessionComponent {
    override fun resolveCallElementToSymbol(callElement: KtCallElement): KtCallableSymbol? {
        val originalFir = callElement.getOrBuildFir(firResolveSession) ?: return null
        val unwrappedFir = originalFir.unwrapSafeCall()
        val resolvedSymbol = when (unwrappedFir) {
            is FirResolvable -> unwrappedFir.calleeReference.toResolvedCallableSymbol(discardErrorReference = true)
            is FirArrayLiteral -> with(analysisSession) { arrayOfSymbol(unwrappedFir)?.firSymbol }
            else -> null
        } ?: return null

        return analysisSession.firSymbolBuilder.callableBuilder.buildCallableSymbol(resolvedSymbol)
    }

    override fun resolveReferenceExpressionToSymbol(expression: KtReferenceExpression): KtSymbol? {
        val originalFir = expression.getOrBuildFir(firResolveSession) ?: return null
        val resolvedSymbol = when (originalFir) {
            is FirResolvable -> originalFir.calleeReference.toResolvedBaseSymbol(discardErrorReference = true)
            is FirArrayLiteral -> with(analysisSession) { arrayOfSymbol(originalFir)?.firSymbol }
            is FirResolvedNamedReference -> originalFir.toResolvedBaseSymbol(discardErrorReference = true)
            else -> null
        } ?: return null

        return analysisSession.firSymbolBuilder.buildSymbol(resolvedSymbol)
    }

}
