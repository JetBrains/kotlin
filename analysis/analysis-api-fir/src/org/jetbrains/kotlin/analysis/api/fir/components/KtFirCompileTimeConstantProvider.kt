/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.components.KtCompileTimeConstantProvider
import org.jetbrains.kotlin.analysis.api.components.KtConstantEvaluationMode
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.psi.KtExpression

internal class KtFirCompileTimeConstantProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtCompileTimeConstantProvider(), KtFirAnalysisSessionComponent {

    override fun evaluate(
        expression: KtExpression,
        mode: KtConstantEvaluationMode,
    ): KtConstantValue? {
        return evaluateFir(expression.getOrBuildFir(firResolveSession), expression, mode)
    }

    override fun evaluateAsAnnotationValue(expression: KtExpression): KtAnnotationValue? =
        (expression.getOrBuildFir(firResolveSession) as? FirExpression)?.let {
            FirAnnotationValueConverter.toConstantValue(it, analysisSession.firSymbolBuilder)
        }

    private fun evaluateFir(
        fir: FirElement?,
        sourcePsi: KtExpression,
        mode: KtConstantEvaluationMode,
    ): KtConstantValue? {
        return when {
            fir is FirPropertyAccessExpression || fir is FirExpression || fir is FirNamedReference -> {
                try {
                    FirCompileTimeConstantEvaluator.evaluateAsKtConstantValue(fir, mode)
                } catch (e: ArithmeticException) {
                    KtConstantValue.KtErrorConstantValue(e.localizedMessage, sourcePsi)
                }
            }
            // For invalid code like the following,
            // ```
            // when {
            //   true, false -> {}
            // }
            // ```
            // `false` does not have a corresponding elements on the FIR side and hence the containing `FirWhenBranch` is returned. In this
            // case, we simply report null since FIR does not know about it.
            else -> null
        }
    }

}
