/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

sealed class FirEvaluatorResult {
    class Evaluated(val result: FirElement) : FirEvaluatorResult() {
        override fun toString(): String = result.render()
    }

    sealed class NotEvaluated : FirEvaluatorResult()

    sealed class DiagnosticError : NotEvaluated()
    data object NotConst : DiagnosticError()
    data object ResolutionError : DiagnosticError()
    data object EnumNotConst : DiagnosticError()
    data object NotKClassLiteral : DiagnosticError()
    data object NotConstValInConstExpression : DiagnosticError()
    data object KClassLiteralOfTypeParameterError : DiagnosticError()

    sealed class CompileTimeException : NotEvaluated()
    data object DivisionByZero : CompileTimeException()
    data object TrimMarginBlankPrefix : CompileTimeException()
    data object RecursionInInitializer : CompileTimeException()
}

inline fun <reified T : FirElement> FirEvaluatorResult.resultOrNull(): T? {
    if (this !is FirEvaluatorResult.Evaluated) return null
    if (this.result !is T) return null
    return this.result
}
