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
     * Attempts to evaluate the given [KtExpression] to a [compile-time constant value][KaConstantValue], or returns `null` if this is not
     * possible.
     */
    public fun KtExpression.evaluate(): KaConstantValue?

    /**
     * Attempts to evaluate the given [KtExpression] to an [annotation value][KaAnnotationValue] (a constant value which can be used as an
     * annotation argument), or returns `null` if this is not possible.
     */
    @KaExperimentalApi
    public fun KtExpression.evaluateAsAnnotationValue(): KaAnnotationValue?
}
