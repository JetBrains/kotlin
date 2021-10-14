/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.api.components.KtCompileTimeConstantProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.evaluate.KtFirConstantValueConverter
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.psi.KtExpression

internal class KtFirCompileTimeConstantProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtCompileTimeConstantProvider(), KtFirAnalysisSessionComponent {

    override fun evaluate(expression: KtExpression): KtConstantValue? = withValidityAssertion {
        when (val fir = expression.getOrBuildFir(firResolveState)) {
            is FirExpression -> {
                FirCompileTimeConstantEvaluator.evaluate(fir)?.let { KtFirConstantValueConverter.toConstantValue(it) }
                    ?: KtFirConstantValueConverter.toConstantValue(fir, firResolveState.rootModuleSession)
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
}
