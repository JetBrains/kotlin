/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.StandardClassIds

object ArrayEqualityCanBeReplacedWithEquals : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirEqualityOperatorCall) return
        if (expression.operation != FirOperation.EQ && expression.operation != FirOperation.NOT_EQ) return
        val arguments = expression.arguments
        val left = arguments.getOrNull(0) ?: return
        val right = arguments.getOrNull(1) ?: return

        if (left.resolvedType.fullyExpandedClassId(context.session) != StandardClassIds.Array) return
        if (right.resolvedType.fullyExpandedClassId(context.session) != StandardClassIds.Array) return

        reporter.reportOn(expression.source, ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS, context)
    }
}
