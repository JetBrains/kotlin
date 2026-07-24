/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.evaluation

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Attempts to evaluate the given [KtExpression] to a [compile-time constant value][KaConstantValue], or returns `null` if this is not
 * possible.
 */
context(session: KaSession)
public fun KtExpression.evaluate(): KaConstantValue? {
    @OptIn(KaImplementationDetail::class)
    return internals.evaluator.evaluate(this)
}

/**
 * Attempts to evaluate the given [KtExpression] to an [annotation value][KaAnnotationValue] (a constant value which can be used as an
 * annotation argument), or returns `null` if this is not possible.
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtExpression.evaluateAsAnnotationValue(): KaAnnotationValue? {
    @OptIn(KaImplementationDetail::class)
    return internals.evaluator.evaluateAsAnnotationValue(this)
}
