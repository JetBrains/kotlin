/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.components.KaCompileTimeConstantProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstantValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.components.KaConstantEvaluationMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtAnnotationValue
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.TypeUtils


internal class KaFe10CompileTimeConstantProvider(
    override val analysisSession: KaFe10Session
) : KaCompileTimeConstantProvider(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun evaluate(
        expression: KtExpression,
        mode: KaConstantEvaluationMode,
    ): KaConstantValue? {
        val bindingContext = analysisContext.analyze(expression)

        val constant = ConstantExpressionEvaluator.getPossiblyErrorConstant(expression, bindingContext)
        if (mode == KaConstantEvaluationMode.CONSTANT_EXPRESSION_EVALUATION) {
            // TODO: how to _not_ evaluate expressions with a compilation error, e.g., uninitialized property access
            if (constant?.usesNonConstValAsConstant == true) return null
        }
        return constant?.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)?.toKtConstantValue()
    }

    override fun evaluateAsAnnotationValue(expression: KtExpression): KaAnnotationValue? {
        val bindingContext = analysisContext.analyze(expression)
        val constant = ConstantExpressionEvaluator.getPossiblyErrorConstant(expression, bindingContext)
        return constant?.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)?.toKtAnnotationValue(analysisContext)
    }
}
