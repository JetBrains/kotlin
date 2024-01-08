/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference

object FirPropertyAccessTypeArgumentsChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirPropertyAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // Property accesses may not have explicit type arguments (see KT-54978). Additionally, the callee reference's errors should take
        // precedence, if any exist. For example, the callee reference might be a function `Collections.emptyList<Int>`, but the programmer
        // has forgotten to add function call parentheses, making it a property access.
        if (expression.calleeReference !is FirErrorNamedReference) {
            val hasExplicitTypeArgument = expression.typeArguments.any { it.source != null }
            if (hasExplicitTypeArgument) {
                reporter.reportOn(expression.source, FirErrors.EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS, "Property", context)
            }
        }
    }
}
