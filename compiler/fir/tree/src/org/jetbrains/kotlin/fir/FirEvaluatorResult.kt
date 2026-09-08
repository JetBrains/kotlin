/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.AbstractKtSourceElement

sealed class FirEvaluatorResult {
    class Evaluated(val result: FirElement) : FirEvaluatorResult() {
        override fun toString(): String = result.render()
    }

    sealed class NotEvaluated(val source: AbstractKtSourceElement?) : FirEvaluatorResult() {
        override fun toString(): String {
            return this::class.simpleName!!
        }
    }

    sealed class DiagnosticError(source: AbstractKtSourceElement?) : NotEvaluated(source)
    class NotConst(source: AbstractKtSourceElement?) : DiagnosticError(source)
    class ResolutionError(source: AbstractKtSourceElement?) : DiagnosticError(source)
    class EnumNotConst(source: AbstractKtSourceElement?) : DiagnosticError(source)
    class NotKClassLiteral(source: AbstractKtSourceElement?) : DiagnosticError(source)
    class NotConstValInConstExpression(source: AbstractKtSourceElement?) : DiagnosticError(source)
    class KClassLiteralOfTypeParameterError(source: AbstractKtSourceElement?) : DiagnosticError(source)
    class ControlFlowNotSupportedError(source: AbstractKtSourceElement?) : DiagnosticError(source)

    sealed class CompileTimeException(source: AbstractKtSourceElement?) : NotEvaluated(source)
    class DivisionByZero(source: AbstractKtSourceElement?) : CompileTimeException(source)
    class TrimMarginBlankPrefix(source: AbstractKtSourceElement?) : CompileTimeException(source)
    class RecursionInInitializer(source: AbstractKtSourceElement?) : CompileTimeException(source)
}

inline fun <reified T : FirElement> FirEvaluatorResult.resultOrNull(): T? {
    if (this !is FirEvaluatorResult.Evaluated) return null
    if (this.result !is T) return null
    return this.result
}
