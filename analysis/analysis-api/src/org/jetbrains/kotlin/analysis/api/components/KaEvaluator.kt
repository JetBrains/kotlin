/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.psi.KtExpression

public interface KaEvaluator {
    /**
     * Evaluates the given expression.
     * Returns a [KaConstantValue] if the expression evaluates to a compile-time constant, otherwise returns `null`.
     */
    public fun KtExpression.evaluate(): KaConstantValue?

    /**
     * Returns a [KaConstantValue] if the expression evaluates to a value that can be used as an annotation parameter value
     * (such as an array or constants). If the expression is not a valid annotation parameter value, returns `null`.
     */
    @KaExperimentalApi
    public fun KtExpression.evaluateAsAnnotationValue(): KaAnnotationValue?
}