/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.analysis.api.fir.getResolvedSymbolOfNameReference
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtForExpression

open class KtFirForLoopInReference(expression: KtForExpression) : KtForLoopInReference(expression), KtFirReference {
    override fun KtAnalysisSession.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtFirAnalysisSession)
        val firLoop = expression.getOrBuildFirSafe<FirWhileLoop>(firResolveSession) ?: return emptyList()
        val condition = firLoop.condition as? FirFunctionCall
        val iterator = this@KtFirForLoopInReference.run {
            val callee = (condition?.explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference
            (callee?.getResolvedSymbolOfNameReference()?.fir as? FirProperty)?.getInitializerFunctionCall()
        }
        val hasNext = condition?.calleeReference?.getResolvedSymbolOfNameReference()
        val next = (firLoop.block.statements.firstOrNull() as? FirProperty?)?.getInitializerFunctionCall()
        return listOfNotNull(
            iterator?.fir?.buildSymbol(firSymbolBuilder),
            hasNext?.fir?.buildSymbol(firSymbolBuilder),
            next?.fir?.buildSymbol(firSymbolBuilder),
        )
    }

    private fun FirProperty.getInitializerFunctionCall() =
        (initializer as? FirFunctionCall)?.calleeReference?.getResolvedSymbolOfNameReference()
}