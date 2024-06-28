/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkForLocalRedeclarations
import org.jetbrains.kotlin.fir.analysis.checkers.collectConflictingLocalFunctionsFrom
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getDestructuredParameter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement

object FirConflictsExpressionChecker : FirBlockChecker(MppCheckerKind.Common) {
    private fun FirStatement.isDestructuredParameter() = this is FirVariable && getDestructuredParameter() != null

    override fun check(expression: FirBlock, context: CheckerContext, reporter: DiagnosticReporter) {
        val elements =
            if (expression.statements.none { it.isDestructuredParameter() }) expression.statements // optimization
            else expression.statements.filterNot { it.isDestructuredParameter() }
        checkForLocalRedeclarations(elements, context, reporter)
        checkForLocalConflictingFunctions(expression, context, reporter)
    }

    private fun checkForLocalConflictingFunctions(expression: FirBlock, context: CheckerContext, reporter: DiagnosticReporter) {
        val conflictingFunctions = collectConflictingLocalFunctionsFrom(expression, context)

        for ((function, otherFunctionsThatConflictWithIt) in conflictingFunctions) {
            if (otherFunctionsThatConflictWithIt.isEmpty()) {
                continue
            }

            reporter.reportOn(function.source, FirErrors.CONFLICTING_OVERLOADS, otherFunctionsThatConflictWithIt, context)
        }
    }
}
