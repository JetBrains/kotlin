/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirConventionFunctionCallChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeReference = expression.calleeReference as? FirErrorNamedReference ?: return
        val diagnostic = calleeReference.diagnostic as? ConeUnresolvedNameError ?: return

        if (expression.calleeReference.source?.kind == FirFakeSourceElementKind.ArrayAccessNameReference) {
            when (diagnostic.name) {
                OperatorNameConventions.GET -> reporter.reportOn(calleeReference.source, FirErrors.NO_GET_METHOD, context)
                OperatorNameConventions.SET -> reporter.reportOn(calleeReference.source, FirErrors.NO_SET_METHOD, context)
            }
        }
    }
}
