/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.psi.KtExpression

@Deprecated(
    message = "Session components are an implementation detail of the legacy 'KaSession' API. Use the context-parameter API instead.",
    level = DeprecationLevel.WARNING,
)
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaEvaluator : KaLifetimeOwner {
    /**
     * Attempts to evaluate the given [KtExpression] to a [compile-time constant value][KaConstantValue], or returns `null` if this is not
     * possible.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.evaluation.evaluate' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.evaluate()",
            imports = [
                "org.jetbrains.kotlin.analysis.api.evaluation.evaluate",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public fun KtExpression.evaluate(): KaConstantValue?

    /**
     * Attempts to evaluate the given [KtExpression] to an [annotation value][KaAnnotationValue] (a constant value which can be used as an
     * annotation argument), or returns `null` if this is not possible.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.evaluation.evaluateAsAnnotationValue' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.evaluateAsAnnotationValue()",
            imports = [
                "org.jetbrains.kotlin.analysis.api.evaluation.evaluateAsAnnotationValue",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun KtExpression.evaluateAsAnnotationValue(): KaAnnotationValue?
}

/**
 * Attempts to evaluate the given [KtExpression] to a [compile-time constant value][KaConstantValue], or returns `null` if this is not
 * possible.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.evaluation' endpoint instead.",
    replaceWith = ReplaceWith(
        "this.evaluate()",
        "org.jetbrains.kotlin.analysis.api.evaluation.evaluate",
    ),
    level = DeprecationLevel.HIDDEN,
)
@KaContextParameterApi
context(session: KaSession)
public fun KtExpression.evaluate(): KaConstantValue? {
    return with(session) {
        evaluate()
    }
}
