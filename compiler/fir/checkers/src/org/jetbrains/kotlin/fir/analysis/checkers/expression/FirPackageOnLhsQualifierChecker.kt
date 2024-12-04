/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier

object FirPackageOnLhsQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        // Check that the expression is a package qualifier
        if (expression.symbol != null) return

        // Left-Hand Side check
        val lastCallableReferenceAccess = context.callsOrAssignments.lastOrNull() as? FirCallableReferenceAccess
        val lastGetClass = context.getClassCalls.lastOrNull()
        if (lastCallableReferenceAccess?.explicitReceiver !== expression && lastGetClass?.argument !== expression) return

        reporter.reportOn(expression.source, FirErrors.EXPRESSION_EXPECTED_PACKAGE_FOUND, context)
    }
}
