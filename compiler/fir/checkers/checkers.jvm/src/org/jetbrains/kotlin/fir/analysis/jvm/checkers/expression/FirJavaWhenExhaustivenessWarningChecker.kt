/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirWhenExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.java.enhancement.enhancedTypeForWarning
import org.jetbrains.kotlin.fir.types.*

object FirJavaWhenExhaustivenessWarningChecker : FirWhenExpressionChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        if (!expression.isExhaustive) return
        val variable = expression.subjectVariable ?: return
        // Take the type of the initializer to account for smart casts.
        val initializerType = variable.initializer!!.resolvedType
        // Take the type of the variable to account for explicit variable types overriding the inferred type
        val variableType = variable.returnTypeRef.coneType
        val enhancedType = variableType.enhancedTypeForWarning
        if (enhancedType != null && !enhancedType.lowerBoundIfFlexible().canBeNull() ||
            !variableType.hasFlexibleMarkedNullability ||
            !initializerType.hasFlexibleMarkedNullability
        ) return

        if (expression.branches.none { it.condition.handlesNull() && !it.hasGuard }) {
            if (enhancedType != null) {
                reporter.reportOn(expression.source, FirJvmErrors.UNEXHAUSTIVE_WHEN_BASED_ON_JAVA_ANNOTATIONS, enhancedType)
            } else {
                reporter.reportOn(expression.source, FirJvmErrors.WHEN_SUBJECT_CAN_BE_NULL_IN_JAVA, initializerType)
            }
        }
    }

    private fun FirExpression.handlesNull(): Boolean {
        return when (this) {
            is FirEqualityOperatorCall -> arguments[1].resolvedType.isNullableNothing
            is FirTypeOperatorCall -> operation == FirOperation.IS && conversionTypeRef.coneType.isMarkedOrFlexiblyNullable
            is FirElseIfTrueCondition -> true
            is FirBooleanOperatorExpression -> kind == LogicOperationKind.OR && (leftOperand.handlesNull() || rightOperand.handlesNull())
            else -> false
        }
    }
}
