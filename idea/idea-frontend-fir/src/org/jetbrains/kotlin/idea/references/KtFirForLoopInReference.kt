/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.idea.fir.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.fir.getResolvedSymbolOfNameReference
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtForExpression

open class KtFirForLoopInReference(expression: KtForExpression) : KtForLoopInReference(expression), KtFirReference {
    override fun resolveToSymbols(analysisSession: KtAnalysisSession): Collection<KtSymbol> {
        check(analysisSession is KtFirAnalysisSession)
        val firLoop = expression.getOrBuildFirSafe<FirWhileLoop>(analysisSession.firResolveState) ?: return emptyList()
        val condition = firLoop.condition as? FirFunctionCall
        val iterator = run {
            val callee = (condition?.explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference
            (callee?.getResolvedSymbolOfNameReference()?.fir as? FirProperty)?.getInitializerFunctionCall()
        }
        val hasNext = condition?.calleeReference?.getResolvedSymbolOfNameReference()
        val next = (firLoop.block.statements.firstOrNull() as? FirProperty?)?.getInitializerFunctionCall()
        return listOfNotNull(
            iterator?.fir?.buildSymbol(analysisSession.firSymbolBuilder),
            hasNext?.fir?.buildSymbol(analysisSession.firSymbolBuilder),
            next?.fir?.buildSymbol(analysisSession.firSymbolBuilder),
        )
    }

    private fun FirProperty.getInitializerFunctionCall() =
        (initializer as? FirFunctionCall)?.calleeReference?.getResolvedSymbolOfNameReference()
}