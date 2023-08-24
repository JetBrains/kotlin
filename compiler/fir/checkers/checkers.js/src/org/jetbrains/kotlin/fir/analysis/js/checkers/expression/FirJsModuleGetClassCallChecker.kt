/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirGetClassCallChecker
import org.jetbrains.kotlin.fir.analysis.js.checkers.checkJsModuleUsage
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.toSymbol


object FirJsModuleGetClassCallChecker : FirGetClassCallChecker() {
    override fun check(expression: FirGetClassCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val callee = expression.argument.coneTypeOrNull?.toSymbol(context.session) ?: return
        checkJsModuleUsage(callee, context, reporter, expression.source)
    }
}
