/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker.isCompatible
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcastToNull
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*

object FirEqualityCompatibilityChecker : FirEqualityOperatorCallChecker() {
    override fun check(expression: FirEqualityOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val arguments = expression.argumentList.arguments
        if (arguments.size != 2) return
        val lExpr = arguments[0]
        val rExpr = arguments[1]
        checkCompatibility(lExpr, rExpr, context, expression, reporter)
        checkSensibleness(lExpr, rExpr, context, expression, reporter)
    }

    private fun checkCompatibility(
        lExpr: FirExpression,
        rExpr: FirExpression,
        context: CheckerContext,
        expression: FirEqualityOperatorCall,
        reporter: DiagnosticReporter
    ) {
        val lType = lExpr.typeRef.coneType
        val rType = rExpr.typeRef.coneType
        // If one of the type is already `Nothing?`, we skip reporting further comparison. This is to allow comparing with `null`, which has
        // type `Nothing?`
        if (lType.isNullableNothing || rType.isNullableNothing) return
        val inferenceContext = context.session.inferenceComponents.ctx

        val compatibility = try {
            inferenceContext.isCompatible(lType, rType)
        } catch (e: Throwable) {
            throw IllegalStateException(
                "Exception while determining type compatibility: lType: $lType, rType: $rType, " +
                        "equality ${expression.render()}, " +
                        "file ${context.containingDeclarations.filterIsInstance<FirFile>().firstOrNull()?.name}",
                e
            )
        }
        if (compatibility != ConeTypeCompatibilityChecker.Compatibility.COMPATIBLE) {
            when (expression.source?.kind) {
                FirRealSourceElementKind -> {
                    // Note: FE1.0 reports INCOMPATIBLE_ENUM_COMPARISON_ERROR only when TypeIntersector.isIntersectionEmpty() thinks the
                    // given types are compatible. Exactly mimicking the behavior of FE1.0 is difficult and does not seem to provide any
                    // value. So instead, we deterministically output INCOMPATIBLE_ENUM_COMPARISON_ERROR if at least one of the value is an
                    // enum.
                    if (compatibility == ConeTypeCompatibilityChecker.Compatibility.HARD_INCOMPATIBLE &&
                        (lType.isEnumType(context) || rType.isEnumType(context))
                    ) {
                        reporter.reportOn(
                            expression.source,
                            FirErrors.INCOMPATIBLE_ENUM_COMPARISON_ERROR,
                            lType,
                            rType,
                            context
                        )
                    } else {
                        reporter.reportOn(
                            expression.source,
                            if (compatibility == ConeTypeCompatibilityChecker.Compatibility.HARD_INCOMPATIBLE) {
                                FirErrors.EQUALITY_NOT_APPLICABLE
                            } else {
                                FirErrors.EQUALITY_NOT_APPLICABLE_WARNING
                            },
                            expression.operation.operator,
                            lType,
                            rType,
                            context
                        )
                    }
                }
                else -> reporter.reportOn(
                    expression.source,
                    if (compatibility == ConeTypeCompatibilityChecker.Compatibility.HARD_INCOMPATIBLE) {
                        FirErrors.INCOMPATIBLE_TYPES
                    } else {
                        FirErrors.INCOMPATIBLE_TYPES_WARNING
                    },
                    lType,
                    rType,
                    context
                )
            }
        }
    }

    private fun ConeKotlinType.isEnumType(
        context: CheckerContext
    ): Boolean {
        if (isEnum) return true
        val firRegularClass = (this as? ConeClassLikeType)?.lookupTag?.toFirRegularClass(context.session) ?: return false
        return firRegularClass.isEnumClass
    }

    private fun checkSensibleness(
        lExpr: FirExpression,
        rExpr: FirExpression,
        context: CheckerContext,
        expression: FirEqualityOperatorCall,
        reporter: DiagnosticReporter
    ) {
        val expressionComparedWithNull = when {
            lExpr.isNullLiteral -> rExpr
            rExpr.isNullLiteral -> lExpr
            else -> return
        }
        val type = expressionComparedWithNull.typeRef.coneType
        if (type is ConeKotlinErrorType) return
        val isPositiveCompare = expression.operation == FirOperation.EQ || expression.operation == FirOperation.IDENTITY
        val compareResult = with(context.session.typeContext) {
            when {
                // `null` literal has type `Nothing?`
                type.isNullableNothing || (expressionComparedWithNull is FirExpressionWithSmartcastToNull && expressionComparedWithNull.isStable) -> isPositiveCompare
                !type.isNullableType() -> !isPositiveCompare
                else -> return
            }
        }
        if (expression.source?.elementType == KtNodeTypes.BINARY_EXPRESSION) {
            reporter.reportOn(expression.source, FirErrors.SENSELESS_COMPARISON, expression, compareResult, context)
        } else {
            reporter.reportOn(expression.source, FirErrors.SENSELESS_NULL_IN_WHEN, context)
        }
    }
}
