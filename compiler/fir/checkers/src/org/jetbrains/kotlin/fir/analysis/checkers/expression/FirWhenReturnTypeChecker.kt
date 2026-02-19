/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INFERRED_INVISIBLE_WHEN_TYPE
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.types.resolvedType

object FirWhenReturnTypeChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        val returnType = expression.resolvedType
        if (returnType.isTypeVisibilityBroken(checkTypeArguments = true)) {
            reporter.reportOn(expression.source, INFERRED_INVISIBLE_WHEN_TYPE, returnType, if (expression.isIfExpression) "if" else "when")
        }
    }

    private val FirWhenExpression.isIfExpression: Boolean
        get() = source?.elementType == KtNodeTypes.IF
}
