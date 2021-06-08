/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSubtypeForTypeMismatch
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NULL_FOR_NONNULL_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RETURN_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SMARTCAST_IMPOSSIBLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.isExhaustive
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.SmartcastStability

object FirFunctionReturnTypeMismatchChecker : FirReturnExpressionChecker() {
    override fun check(expression: FirReturnExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.source == null) return
        val targetElement = expression.target.labeledElement
        if (targetElement !is FirSimpleFunction) return
        val resultExpression = expression.result
        // To avoid duplications with NO_ELSE_IN_WHEN or INVALID_IF_AS_EXPRESSION
        if (resultExpression is FirWhenExpression && !resultExpression.isExhaustive) return

        val functionReturnType = targetElement.returnTypeRef.coneType
        val typeContext = context.session.typeContext
        val returnExpressionType = resultExpression.typeRef.coneTypeSafe<ConeKotlinType>() ?: return

        if (!isSubtypeForTypeMismatch(typeContext, subtype = returnExpressionType, supertype = functionReturnType)) {
            if (resultExpression.isNullLiteral && functionReturnType.nullability == ConeNullability.NOT_NULL) {
                reporter.reportOn(resultExpression.source, NULL_FOR_NONNULL_TYPE, context)
            } else {
                if (resultExpression is FirExpressionWithSmartcast && resultExpression.smartcastStability != SmartcastStability.STABLE_VALUE &&
                    isSubtypeForTypeMismatch(typeContext, subtype = resultExpression.smartcastType.coneType, supertype = functionReturnType)
                ) {
                    reporter.reportOn(
                        returnExpressionSource,
                        SMARTCAST_IMPOSSIBLE,
                        functionReturnType,
                        resultExpression,
                        resultExpression.smartcastStability.description,
                        context
                    )
                } else {
                    reporter.reportOn(
                    resultExpression.source,
                        RETURN_TYPE_MISMATCH,
                    functionReturnType,
                    returnExpressionType,
                    targetElement,
                        context
                    )
                }
            }
        }
    }
}
