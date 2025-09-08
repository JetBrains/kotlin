/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkUnderscoreDiagnostics
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isUnderscore
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

object FirUnderscoreChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        when (expression) {
            is FirResolvable -> {
                if (diagnosticsCheckNeeded(expression)) {
                    checkUnderscoreDiagnostics(expression.calleeReference.source, true)
                }
            }
            is FirResolvedQualifier -> {
                checkUnderscoreDiagnostics(expression.source, true)
            }
        }
    }

    private fun diagnosticsCheckNeeded(expression: FirResolvable): Boolean {
        if (expression.calleeReference is FirErrorNamedReference)
            return false
        return when (expression) {
            is FirImplicitInvokeCall -> expression.calleeReference.name.asString().isUnderscore
            is FirCall -> expression.calleeReference.toResolvedSymbol<FirFunctionSymbol<*>>()?.callableId?.callableName?.asString()?.isUnderscore == true
            else -> true
        }
    }
}
