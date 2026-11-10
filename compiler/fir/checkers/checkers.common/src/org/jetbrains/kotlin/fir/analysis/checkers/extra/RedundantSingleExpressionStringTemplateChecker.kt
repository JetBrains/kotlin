/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirStringConcatenationCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.types.resolvedType

object RedundantSingleExpressionStringTemplateChecker : FirStringConcatenationCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStringConcatenationCall) {
        for (argumentExpression in expression.arguments) {
            if (argumentExpression.resolvedType.fullyExpandedType().isString &&
                withSyntax { argumentExpression.source?.getContainingStringTemplateEntriesCount() } == 1 // there is no more children in original string template
            ) {
                reporter.reportOn(argumentExpression.source, REDUNDANT_SINGLE_EXPRESSION_STRING_TEMPLATE)
            }
        }
    }
}
