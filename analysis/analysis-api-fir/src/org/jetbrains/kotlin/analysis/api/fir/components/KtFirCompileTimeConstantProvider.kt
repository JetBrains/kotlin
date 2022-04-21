/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.base.KtConstantValueFactory
import org.jetbrains.kotlin.analysis.api.components.KtCompileTimeConstantProvider
import org.jetbrains.kotlin.analysis.api.components.KtConstantEvaluationMode
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.types.ConstantValueKind

internal class KtFirCompileTimeConstantProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtCompileTimeConstantProvider(), KtFirAnalysisSessionComponent {

    override fun evaluate(
        expression: KtExpression,
        mode: KtConstantEvaluationMode,
    ): KtConstantValue? = withValidityAssertion {
        evaluateFir(expression.getOrBuildFir(firResolveState), expression, mode)
    }

    private fun evaluateFir(
        fir: FirElement?,
        sourcePsi: KtExpression,
        mode: KtConstantEvaluationMode,
    ): KtConstantValue? = withValidityAssertion {
        when (fir) {
            is FirConstExpression<*> -> {
                val v = FirCompileTimeConstantEvaluator.evaluateAsKtConstantValue(fir, mode)
                if (v != null && fir.isConverted(sourcePsi)) {
                    val value = fir.kind.unaryMinus(v.value as? Number) ?: return null
                    KtConstantValueFactory.createConstantValue(value, v.sourcePsi)
                } else v
            }
            is FirPropertyAccessExpression,
            is FirExpression,
            is FirNamedReference -> {
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
            is FirWhenBranch -> null
            else -> throwUnexpectedFirElementError(fir, sourcePsi)
        }
    }

    private fun FirConstExpression<*>.isConverted(sourcePsi: KtExpression): Boolean {
        val firSourcePsi = this.source?.psi ?: return false
        return firSourcePsi is KtPrefixExpression && firSourcePsi.operationToken == KtTokens.MINUS && firSourcePsi != sourcePsi
    }

    private fun ConstantValueKind<*>.unaryMinus(value: Number?): Any? {
        if (value == null) {
            return null
        }
        return when (this) {
            ConstantValueKind.Byte -> value.toByte().unaryMinus()
            ConstantValueKind.Double -> value.toDouble().unaryMinus()
            ConstantValueKind.Float -> value.toFloat().unaryMinus()
            ConstantValueKind.Int -> value.toInt().unaryMinus()
            ConstantValueKind.Long -> value.toLong().unaryMinus()
            ConstantValueKind.Short -> value.toShort().unaryMinus()
            else -> null
        }
    }
}
