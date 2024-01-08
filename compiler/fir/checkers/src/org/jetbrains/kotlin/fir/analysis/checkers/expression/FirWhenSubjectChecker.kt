/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.lexer.KtTokens

object FirWhenSubjectChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirWhenExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val subject = expression.subject
        val subjectVariable = expression.subjectVariable
        val source = (subjectVariable ?: subject)?.source ?: return
        when {
            subject?.source?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION -> {
                reporter.reportOn(source, FirErrors.ILLEGAL_DECLARATION_IN_WHEN_SUBJECT, "destructuring declaration", context)
            }
            subjectVariable?.source?.getChild(KtTokens.VAR_KEYWORD) != null -> {
                reporter.reportOn(source, FirErrors.ILLEGAL_DECLARATION_IN_WHEN_SUBJECT, "var", context)
            }
            subjectVariable?.source?.getChild(KtNodeTypes.PROPERTY_DELEGATE) != null -> {
                reporter.reportOn(source, FirErrors.ILLEGAL_DECLARATION_IN_WHEN_SUBJECT, "delegated property", context)
            }
            subjectVariable != null && subjectVariable.initializer == null -> {
                reporter.reportOn(source, FirErrors.ILLEGAL_DECLARATION_IN_WHEN_SUBJECT, "variable without initializer", context)
            }
        }
    }
}
