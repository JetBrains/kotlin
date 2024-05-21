/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtExpression

public abstract class KaCompileTimeConstantProvider : KaSessionComponent() {
    public abstract fun evaluate(expression: KtExpression): KaConstantValue?

    public abstract fun evaluateAsAnnotationValue(expression: KtExpression): KaAnnotationValue?
}

public typealias KtCompileTimeConstantProvider = KaCompileTimeConstantProvider

public interface KaCompileTimeConstantProviderMixIn : KaSessionMixIn {
    /**
     * Tries to evaluate the provided expression.
     * Returns a [KaConstantValue] if the expression evaluates to a compile-time constant, otherwise returns null.
     */
    public fun KtExpression.evaluate(): KaConstantValue? =
        withValidityAssertion { analysisSession.compileTimeConstantProvider.evaluate(this) }

    /**
     * Returns a [KaConstantValue] if the expression evaluates to a value that can be used as an annotation parameter value,
     * e.g. an array of constants, otherwise returns null.
     */
    public fun KtExpression.evaluateAsAnnotationValue(): KaAnnotationValue? =
        withValidityAssertion { analysisSession.compileTimeConstantProvider.evaluateAsAnnotationValue(this) }
}

public typealias KtCompileTimeConstantProviderMixIn = KaCompileTimeConstantProviderMixIn