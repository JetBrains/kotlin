/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtCompileTimeConstantProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstantValue
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.components.KtConstantEvaluationMode
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.TypeUtils

internal class KtFe10CompileTimeConstantProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtCompileTimeConstantProvider(), Fe10KtAnalysisSessionComponent {
    override val token: ValidityToken
        get() = analysisSession.token

    override fun evaluate(
        expression: KtExpression,
        mode: KtConstantEvaluationMode,
    ): KtConstantValue? {
        val bindingContext = analysisContext.analyze(expression)

        if (mode == KtConstantEvaluationMode.CONSTANT_EXPRESSION_EVALUATION) {
            // TODO: how to _not_ evaluate composite expression with a non-const property?
            // TODO: how to _not_ evaluate expressions with a compilation error, e.g., uninitialized property access
            when (expression) {
                is KtNameReferenceExpression -> {
                    val reference = bindingContext[BindingContext.REFERENCE_TARGET, expression]
                    if (reference is PropertyDescriptor && !reference.isConst) return null
                }
            }
        }

        val constant = ConstantExpressionEvaluator.getPossiblyErrorConstant(expression, bindingContext)
        return constant?.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)?.toKtConstantValue()
    }
}
