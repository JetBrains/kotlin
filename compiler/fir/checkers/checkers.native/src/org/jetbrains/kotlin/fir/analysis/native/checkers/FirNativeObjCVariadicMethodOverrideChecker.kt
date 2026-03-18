/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.backend.native.interop.getObjCMethodInfoFromOverriddenFunctions
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.utils.isOverride

internal object FirNativeObjCVariadicMethodOverrideChecker : FirFunctionChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (!declaration.isOverride) return

        val symbol = declaration.symbol
        if (symbol.valueParameterSymbols.any { it.isVararg }) {
            val objCMethodInfo = symbol.getObjCMethodInfoFromOverriddenFunctions(context.session, context.scopeSession)
            if (objCMethodInfo != null) {
                reporter.reportOn(
                    declaration.source,
                    FirNativeErrors.OVERRIDING_VARIADIC_OBJECTIVE_C_METHODS_IS_NOT_SUPPORTED,
                    symbol
                )
            }
        }
    }
}
