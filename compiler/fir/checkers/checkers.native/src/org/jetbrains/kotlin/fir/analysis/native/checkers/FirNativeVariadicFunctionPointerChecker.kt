/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.backend.konan.IntrinsicType
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

internal object FirNativeVariadicFunctionPointerChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        if (tryGetIntrinsicType(expression) != IntrinsicType.INTEROP_STATIC_C_FUNCTION) return

        val argument = expression.arguments.firstOrNull() ?: return
        val referencedFunctionSymbol = when (argument) {
            is FirCallableReferenceAccess -> argument.calleeReference.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            is FirAnonymousFunctionExpression -> argument.anonymousFunction.symbol
            else -> null
        } ?: return

        if (referencedFunctionSymbol.valueParameterSymbols.any { it.isVararg }) {
            reporter.reportOn(argument.source, FirNativeErrors.VARIADIC_FUNCTION_POINTERS_ARE_NOT_SUPPORTED, referencedFunctionSymbol)
        }
    }
}
