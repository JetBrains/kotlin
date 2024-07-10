/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.checkTypeMismatch
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.types.resolvedType

object FirAssignmentTypeMismatchChecker : FirVariableAssignmentChecker() {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = expression.rValue.source ?: return
        val coneType = expression.lValue.resolvedType
        checkTypeMismatch(
            coneType,
            expression,
            expression.rValue,
            context,
            source,
            reporter,
            false,
        )
    }
}
