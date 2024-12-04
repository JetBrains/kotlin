/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkCondition
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirErrorLoop
import org.jetbrains.kotlin.fir.expressions.FirLoop

object FirLoopConditionChecker : FirLoopExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirLoop, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression is FirErrorLoop) return
        val condition = expression.condition
        checkCondition(condition, context, reporter)
    }
}
