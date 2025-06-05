/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.psi.KtExpression

@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaEvaluator : KaSessionComponent {
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

/**
 * @see KaEvaluator.evaluate
 */
@KaContextParameterApi
context(context: KaEvaluator)
public fun KtExpression.evaluate(): KaConstantValue? {
    return with(context) { evaluate() }
}

/**
 * @see KaEvaluator.evaluateAsAnnotationValue
 */
@KaContextParameterApi
@KaExperimentalApi
context(context: KaEvaluator)
public fun KtExpression.evaluateAsAnnotationValue(): KaAnnotationValue? {
    return with(context) { evaluateAsAnnotationValue() }
}