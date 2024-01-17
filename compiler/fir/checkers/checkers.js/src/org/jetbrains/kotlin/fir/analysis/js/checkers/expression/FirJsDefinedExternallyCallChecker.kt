/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.closestNonLocal
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.analysis.js.checkers.isPredefinedObject
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsDefinedExternallyCallChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.toReference(context.session)?.toResolvedCallableSymbol() ?: return

        if (symbol.callableId !in JsStandardClassIds.Callables.definedExternallyPropertyNames) {
            return
        }

        val container = context.closestNonLocal?.symbol ?: return

        if (!container.isNativeObject(context) && !container.isPredefinedObject(context)) {
            reporter.reportOn(expression.source, FirJsErrors.CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION, context)
        }
    }
}
