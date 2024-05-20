/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.components.KaCompileTimeConstantProvider
import org.jetbrains.kotlin.analysis.api.components.KaConstantEvaluationMode
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.psi.KtExpression

internal class KaFirCompileTimeConstantProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaCompileTimeConstantProvider(), KaFirSessionComponent {

    override fun evaluate(
        expression: KtExpression,
        mode: KaConstantEvaluationMode,
    ): KaConstantValue? {
        return evaluateFir(expression.getOrBuildFir(firResolveSession), expression, mode)
    }

    override fun evaluateAsAnnotationValue(expression: KtExpression): KaAnnotationValue? =
        (expression.getOrBuildFir(firResolveSession) as? FirExpression)?.let {
            FirAnnotationValueConverter.toConstantValue(it, analysisSession.firSymbolBuilder)
        }

    private fun evaluateFir(
        fir: FirElement?,
        sourcePsi: KtExpression,
        mode: KaConstantEvaluationMode,
    ): KaConstantValue? {
        return when {
            fir is FirPropertyAccessExpression || fir is FirExpression || fir is FirNamedReference -> {
                try {
                    FirCompileTimeConstantEvaluator.evaluateAsKtConstantValue(fir, mode)
                } catch (e: ArithmeticException) {
                    KaConstantValue.KaErrorConstantValue(e.localizedMessage, sourcePsi)
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
