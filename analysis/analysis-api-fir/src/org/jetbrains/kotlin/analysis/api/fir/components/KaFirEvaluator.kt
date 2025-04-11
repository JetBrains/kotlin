/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.components.KaEvaluator
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.api.impl.base.KaErrorConstantValueImpl
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.psi.KtExpression

internal class KaFirEvaluator(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaEvaluator, KaFirSessionComponent {
    override fun KtExpression.evaluate(): KaConstantValue? = withValidityAssertion {
        return evaluateFir(getOrBuildFir(resolutionFacade), this)
    }

    override fun KtExpression.evaluateAsAnnotationValue(): KaAnnotationValue? = withValidityAssertion {
        return (getOrBuildFir(resolutionFacade) as? FirExpression)?.let {
            FirAnnotationValueConverter.toConstantValue(it, analysisSession.firSymbolBuilder)
        }
    }

    private fun evaluateFir(
        fir: FirElement?,
        sourcePsi: KtExpression,
    ): KaConstantValue? {
        return when {
            fir is FirPropertyAccessExpression || fir is FirExpression || fir is FirNamedReference -> {
                try {
                    FirCompileTimeConstantEvaluator.evaluateAsKtConstantValue(fir)
                } catch (e: ArithmeticException) {
                    KaErrorConstantValueImpl(e.localizedMessage, sourcePsi)
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
