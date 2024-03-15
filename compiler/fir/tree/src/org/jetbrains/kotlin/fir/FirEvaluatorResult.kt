/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.FirEvaluatorResult.CompileTimeException
import org.jetbrains.kotlin.fir.FirEvaluatorResult.Evaluated

sealed class FirEvaluatorResult {
    class Evaluated(val result: FirElement) : FirEvaluatorResult()
    data object NotEvaluated : FirEvaluatorResult()
    data object DuringEvaluation : FirEvaluatorResult()

    sealed class CompileTimeException : FirEvaluatorResult()
    data object DivisionByZero : CompileTimeException()
    data object RecursionInInitializer : CompileTimeException()
}


inline fun <reified T> FirEvaluatorResult.unwrapOr(action: (CompileTimeException) -> Unit): T? {
    when (this) {
        is CompileTimeException -> action(this)
        is Evaluated -> return this.result as? T
        else -> return null
    }
    return null
}
