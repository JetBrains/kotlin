/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.KtInitializerValue
import org.jetbrains.kotlin.analysis.api.KtNonConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.components.KtCompileTimeConstantProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.api.fir.symbols.getKtConstantInitializer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.fir.declarations.utils.referredPropertySymbol
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression

internal class KtFirCompileTimeConstantProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtCompileTimeConstantProvider(), KtFirAnalysisSessionComponent {

    override fun evaluate(expression: KtExpression): KtConstantValue? = withValidityAssertion {
        when (val fir = expression.getOrBuildFir(firResolveState)) {
            is FirPropertyAccessExpression -> {
                fir.referredPropertySymbol?.toKtConstantValue()
            }
            is FirExpression -> {
                try {
                    FirCompileTimeConstantEvaluator.evaluateAsKtConstantExpression(fir)
                } catch (e: ArithmeticException) {
                    KtConstantValue.KtErrorConstantValue(e.localizedMessage, fir.psi as? KtElement)
                }
            }
            is FirNamedReference -> {
                when (val resolvedSymbol = fir.resolvedSymbol) {
                    is FirPropertySymbol -> resolvedSymbol.toKtConstantValue()
                    else -> null
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
            else -> throwUnexpectedFirElementError(fir, expression)
        }
    }

    private fun FirPropertySymbol.toKtConstantValue(): KtConstantValue? = withValidityAssertion {
        if (isVal) {
            getKtConstantInitializer()?.toKtConstantValue()
        } else null
    }

    private fun KtInitializerValue?.toKtConstantValue(): KtConstantValue? = withValidityAssertion {
        when (this) {
            null -> null
            is KtConstantInitializerValue -> constant
            is KtNonConstantInitializerValue -> initializerPsi?.let { evaluate(it) }
        }
    }
}
