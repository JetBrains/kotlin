/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirWhenExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isBooleanOrNullableBoolean
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds
import org.jetbrains.kotlin.syntax.SyntaxIdSet

object FirConfusingWhenBranchSyntaxChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    private val PROHIBITED_OPERATIONS = SyntaxIdSet.of(
        KotlinSyntaxNodeTypeIds.IS_EXPRESSION,
        KotlinSyntaxTokenIds.IN_MODIFIER, KotlinSyntaxTokenIds.NOT_IN,
        KotlinSyntaxTokenIds.LT, KotlinSyntaxTokenIds.LTEQ, KotlinSyntaxTokenIds.GT, KotlinSyntaxTokenIds.GTEQ,
        KotlinSyntaxTokenIds.EQEQ, KotlinSyntaxTokenIds.EXCLEQ, KotlinSyntaxTokenIds.EQEQEQ, KotlinSyntaxTokenIds.EXCLEQEQEQ,
        KotlinSyntaxTokenIds.ANDAND, KotlinSyntaxTokenIds.OROR,
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        val source = expression.source ?: return
        val subjectType = expression.subjectVariable?.initializer?.resolvedType ?: expression.subjectVariable?.returnTypeRef?.coneType ?: return
        val booleanSubject = subjectType.isBooleanOrNullableBoolean
        withSyntax {
            source.forEachWhenConditionOperation { expression, operation ->
                when (operation) {
                    KotlinSyntaxTokenIds.ANDAND if !booleanSubject -> reporter.reportOn(expression, FirErrors.WRONG_CONDITION_SUGGEST_GUARD)
                    in PROHIBITED_OPERATIONS -> reporter.reportOn(expression, FirErrors.CONFUSING_BRANCH_CONDITION_ERROR)
                }
            }
        }
    }
}
