/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.FirEvaluatorResult.CompileTimeException
import org.jetbrains.kotlin.fir.FirEvaluatorResult.Evaluated
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression

sealed class FirEvaluatorResult {
    class Evaluated(val result: FirElement) : FirEvaluatorResult() {
        override fun toString(): String = result.render()
    }

    data object NotEvaluated : FirEvaluatorResult()
    data object DuringEvaluation : FirEvaluatorResult()

    sealed class CompileTimeException : FirEvaluatorResult()
    data object DivisionByZero : CompileTimeException()
    data object RecursionInInitializer : CompileTimeException()
}


inline fun <reified T : FirElement> FirEvaluatorResult.unwrapOr(action: (CompileTimeException) -> Unit): T? {
    when (this) {
        is CompileTimeException -> action(this)
        is Evaluated -> return this.result as? T
        else -> return null
    }
    return null
}
