/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.backend.native.interop.isCFunctionOrGlobalAccessor
import org.jetbrains.kotlin.fir.backend.native.interop.isObjCMethod
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

internal object FirNativeVariadicCallableReferenceChecker : FirCallableReferenceAccessChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCallableReferenceAccess) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return
        if (symbol.valueParameterSymbols.none { it.isVararg }) return

        val session = context.session
        if (symbol.isCFunctionOrGlobalAccessor(session)) {
            reporter.reportOn(expression.source, FirNativeErrors.CALLABLE_REFERENCES_TO_VARIADIC_C_FUNCTIONS_ARE_NOT_SUPPORTED, symbol)
        }
        if (symbol.isObjCMethod(session)) {
            reporter.reportOn(expression.source, FirNativeErrors.CALLABLE_REFERENCES_TO_VARIADIC_OBJECTIVE_C_METHODS_ARE_NOT_SUPPORTED, symbol)
        }
    }
}
