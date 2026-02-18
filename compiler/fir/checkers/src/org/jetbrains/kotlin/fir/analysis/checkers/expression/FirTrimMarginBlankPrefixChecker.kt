/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

object FirTrimMarginBlankPrefixChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val trimMarginCallableId = CallableId(StandardNames.TEXT_PACKAGE_FQ_NAME, Name.identifier(String::trimMargin.name))

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val callableId = expression.calleeReference.toResolvedNamedFunctionSymbol()?.callableId
        if (callableId == trimMarginCallableId) {
            val firstValue = (expression.arguments.singleOrNull() as? FirLiteralExpression)?.value
            if (firstValue != null && firstValue is String && firstValue.isBlank()) {
                reporter.reportOn(expression.source, FirErrors.TRIM_MARGIN_BLANK_PREFIX)
            }
        }
    }
}
