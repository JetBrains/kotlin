/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds

object FirAnnotatedBinaryExpressionChecker : FirExpressionSyntaxChecker<FirStatement>() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(element: FirStatement, source: KtSourceElement) {
        if (source.kind !is KtRealSourceElementKind || source.syntaxId != KotlinSyntaxNodeTypeIds.BINARY_EXPRESSION) return

        if (withSyntax { source.isAnnotatedBinaryExpressionStatement() }) {
            reporter.reportOn(source, FirErrors.ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE)
        }
    }
}
