/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtExpression

public enum class KaConstantEvaluationMode {
    /**
     * In this mode, what a compiler views as constants will be evaluated. In other words,
     * expressions and properties that are free from runtime behaviors/changes will be evaluated,
     *   such as `const val` properties or binary expressions whose operands are constants.
     */
    CONSTANT_EXPRESSION_EVALUATION,

    /**
     * In this mode, what a checker can approximate as constants will be evaluated. In other words,
     *   more expressions and properties that could be composites of other constants will be evaluated,
     *   such as `val` properties with constant initializers or binary expressions whose operands could be constants.
     *
     * Note that, as an approximation, the result can be sometimes incorrect or present even though there is an error.
     */
    CONSTANT_LIKE_EXPRESSION_EVALUATION;
}

public typealias KtConstantEvaluationMode = KaConstantEvaluationMode

public abstract class KaCompileTimeConstantProvider : KaSessionComponent() {
    public abstract fun evaluate(
        expression: KtExpression,
        mode: KaConstantEvaluationMode,
    ): KaConstantValue?

    public abstract fun evaluateAsAnnotationValue(expression: KtExpression): KaAnnotationValue?
}

public typealias KtCompileTimeConstantProvider = KaCompileTimeConstantProvider

public interface KaCompileTimeConstantProviderMixIn : KaSessionMixIn {
    /**
     * Tries to evaluate the provided expression using the specified mode.
     * Returns a [KaConstantValue] if the expression evaluates to a compile-time constant, otherwise returns null..
     */
    public fun KtExpression.evaluate(mode: KaConstantEvaluationMode): KaConstantValue? =
        withValidityAssertion { analysisSession.compileTimeConstantProvider.evaluate(this, mode) }

    /**
     * Returns a [KaConstantValue] if the expression evaluates to a value that can be used as an annotation parameter value,
     * e.g. an array of constants, otherwise returns null.
     */
    public fun KtExpression.evaluateAsAnnotationValue(): KaAnnotationValue? =
        withValidityAssertion { analysisSession.compileTimeConstantProvider.evaluateAsAnnotationValue(this) }
}

public typealias KtCompileTimeConstantProviderMixIn = KaCompileTimeConstantProviderMixIn