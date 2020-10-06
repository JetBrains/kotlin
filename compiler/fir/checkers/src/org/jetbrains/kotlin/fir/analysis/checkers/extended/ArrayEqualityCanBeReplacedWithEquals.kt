/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.lexer.KtTokens

object ArrayEqualityCanBeReplacedWithEquals : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirEqualityOperatorCall) return
        if (expression.operation != FirOperation.EQ && expression.operation != FirOperation.NOT_EQ) return
        val left = expression.arguments.getOrNull(0) ?: return
        val right = expression.arguments.getOrNull(1) ?: return

        if (left.typeRef.coneType.classId != StandardClassIds.Array) return
        if (right.typeRef.coneType.classId != StandardClassIds.Array) return

        val source = expression.source?.getChild(setOf(KtTokens.EQEQ, KtTokens.EXCLEQ))
        reporter.report(source, ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS)
    }
}
