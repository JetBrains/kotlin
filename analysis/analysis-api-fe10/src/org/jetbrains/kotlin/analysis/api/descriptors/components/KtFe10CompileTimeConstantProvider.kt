/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtCompileTimeConstantProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtConstantValue
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.TypeUtils

internal class KtFe10CompileTimeConstantProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtCompileTimeConstantProvider(), Fe10KtAnalysisSessionComponent {
    override val token: ValidityToken
        get() = analysisSession.token

    override fun evaluate(expression: KtExpression): KtConstantValue? {
        val bindingContext = analysisContext.analyze(expression)
        val constant = ConstantExpressionEvaluator.getPossiblyErrorConstant(expression, bindingContext)
        return constant?.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)?.toKtConstantValue()
    }
}
