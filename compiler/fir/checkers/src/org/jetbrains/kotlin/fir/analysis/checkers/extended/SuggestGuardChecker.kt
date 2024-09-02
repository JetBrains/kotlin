/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirWhenExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirBooleanOperatorExpression
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.typeContext

object SuggestGuardChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirWhenExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.WhenGuards)) return
        val subject = expression.subject ?: return
        val subjectType = subject.expandedType(context)
        // we cannot suggest replacing '&&' with 'if' when the subject is Boolean
        if (subjectType is ConeErrorType || subjectType.isBoolean(context)) return
        expression.branches.forEach { checkCondition(it.condition, context, reporter) }
    }

    fun checkCondition(condition: FirExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (condition !is FirEqualityOperatorCall) return
        val element = condition.arguments[1] // right-hand side of the equality introduced at FIR construction
        if (element !is FirBooleanOperatorExpression || element.kind != LogicOperationKind.AND) return
        val check = element.leftOperand // left-hand side of '&&'
        val checkType = check.resolvedType.fullyExpandedType(context.session).lowerBoundIfFlexible()
        if (checkType !is ConeErrorType && !checkType.isBoolean(context)) {
            reporter.reportOn(element.source, FirErrors.SUGGEST_GUARD_KEYWORD, context)
        }
    }

    fun FirExpression.expandedType(context: CheckerContext) =
        this.resolvedType.fullyExpandedType(context.session).lowerBoundIfFlexible()

    fun ConeKotlinType.isBoolean(context: CheckerContext): Boolean =
        isSubtypeOf(context.session.typeContext, context.session.builtinTypes.booleanType.coneType)
}