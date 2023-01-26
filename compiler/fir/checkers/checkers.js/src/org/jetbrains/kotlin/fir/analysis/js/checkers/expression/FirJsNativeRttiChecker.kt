/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeInterface
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.coneType

object FirJsNativeRttiChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        when (expression) {
            is FirGetClassCall -> checkGetClassCall(expression, context, reporter)
            is FirTypeOperatorCall -> checkTypeOperatorCall(expression, context, reporter)
            else -> {}
        }
    }

    private fun checkGetClassCall(expression: FirGetClassCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val declarationToCheck = expression.argument.typeRef.toRegularClassSymbol(context.session) ?: return

        if (declarationToCheck.isNativeInterface(context)) {
            reporter.reportOn(expression.source, FirJsErrors.EXTERNAL_INTERFACE_AS_CLASS_LITERAL, context)
        }
    }

    private fun checkTypeOperatorCall(expression: FirTypeOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val targetTypeRef = expression.conversionTypeRef
        val declarationToCheck = targetTypeRef.toRegularClassSymbol(context.session) ?: return

        if (!declarationToCheck.isNativeInterface(context)) {
            return
        }

        when (expression.operation) {
            FirOperation.AS, FirOperation.SAFE_AS -> reporter.reportOn(
                expression.source,
                FirJsErrors.UNCHECKED_CAST_TO_EXTERNAL_INTERFACE,
                expression.argument.typeRef.coneType,
                targetTypeRef.coneType,
                context,
            )
            FirOperation.IS, FirOperation.NOT_IS -> reporter.reportOn(
                expression.source,
                FirJsErrors.CANNOT_CHECK_FOR_EXTERNAL_INTERFACE,
                targetTypeRef.coneType,
                context,
            )
            else -> {}
        }
    }
}