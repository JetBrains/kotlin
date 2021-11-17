/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.resolve.transformers.FirWhenExhaustivenessTransformer
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.api.components.KtExpressionInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

internal class KtFirExpressionInfoProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtExpressionInfoProvider(), KtFirAnalysisSessionComponent {
    override fun getReturnExpressionTargetSymbol(returnExpression: KtReturnExpression): KtCallableSymbol? {
        val fir = returnExpression.getOrBuildFirSafe<FirReturnExpression>(firResolveState) ?: return null
        val firTargetSymbol = fir.target.labeledElement
        if (firTargetSymbol is FirErrorFunction) return null
        return firSymbolBuilder.callableBuilder.buildCallableSymbol(firTargetSymbol)
    }

    override fun getWhenMissingCases(whenExpression: KtWhenExpression): List<WhenMissingCase> {
        val firWhenExpression = whenExpression.getOrBuildFirSafe<FirWhenExpression>(analysisSession.firResolveState) ?: return emptyList()
        return FirWhenExhaustivenessTransformer.computeAllMissingCases(analysisSession.firResolveState.rootModuleSession, firWhenExpression)
    }
}