/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.syntax.withSyntax

/**
 * Reports a literal glued to an identifier, a number or a keyword, as in `a foo"bar"` or `1in a`, see
 * [org.jetbrains.kotlin.KtLiteralPrefixAndSuffix].
 *
 * Only the trees whose parser doesn't report this on its own provide the sources here,
 * see [org.jetbrains.kotlin.fir.syntax.FirSyntaxComponent.findProhibitedLiteralPrefixSource].
 */
object FirPrefixAndSuffixSyntaxChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        val source = expression.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        withSyntax {
            source.findProhibitedLiteralPrefixSource()?.let { reporter.reportOn(it, FirSyntaxErrors.TRAILING_WHITESPACE_REQUIRED) }
            source.findProhibitedLiteralSuffixSource()?.let { reporter.reportOn(it, FirSyntaxErrors.LEADING_WHITESPACE_REQUIRED) }
        }
    }
}
