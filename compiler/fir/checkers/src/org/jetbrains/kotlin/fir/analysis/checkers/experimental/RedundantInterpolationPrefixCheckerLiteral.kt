/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.experimental

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirLiteralExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirStringConcatenationCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.types.ConstantValueKind

object RedundantInterpolationPrefixCheckerConcatenation : FirStringConcatenationCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStringConcatenationCall) {
        if (expression.interpolationPrefix == "$") {
            expression.reportRedundantInterpolationPrefix()
        }
    }
}

object RedundantInterpolationPrefixCheckerLiteral : FirLiteralExpressionChecker(MppCheckerKind.Common) {
    // substrings that look like an interpolation
    private val redundancyRegex = Regex("""(\$+)(\w|\{|`[^`])""")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirLiteralExpression) {
        val prefixLength = expression.prefix?.length ?: return
        when {
            prefixLength == 1 -> expression.reportRedundantInterpolationPrefix()
            expression.kind == ConstantValueKind.String -> {
                val value = expression.source.text?.drop(prefixLength) ?: return
                if (!redundancyRegex.containsMatchIn(value)) {
                    expression.reportRedundantInterpolationPrefix()
                }
            }
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun FirExpression.reportRedundantInterpolationPrefix() {
    reporter.reportOn(source, FirErrors.REDUNDANT_INTERPOLATION_PREFIX)
}
