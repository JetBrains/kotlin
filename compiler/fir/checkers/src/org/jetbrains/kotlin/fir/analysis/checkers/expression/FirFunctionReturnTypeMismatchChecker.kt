/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.hasExplicitReturnType
import org.jetbrains.kotlin.fir.analysis.checkers.isSubtypeForTypeMismatch
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NULL_FOR_NONNULL_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RETURN_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SMARTCAST_IMPOSSIBLE
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.isExhaustive
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.types.*

object FirFunctionReturnTypeMismatchChecker : FirReturnExpressionChecker() {
    override fun check(expression: FirReturnExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // checked in FirDelegatedPropertyChecker
        if (expression.source?.kind == KtFakeSourceElementKind.DelegatedPropertyAccessor) return

        val targetElement = expression.target.labeledElement
        if (targetElement is FirErrorFunction) {
            return
        }

        val sourceKind = expression.source?.kind
        if (
            !targetElement.symbol.hasExplicitReturnType &&
            sourceKind != KtRealSourceElementKind &&
            targetElement !is FirPropertyAccessor
        ) {
            return
        }
        val resultExpression = expression.result
        // To avoid duplications with NO_ELSE_IN_WHEN or INVALID_IF_AS_EXPRESSION
        if (resultExpression is FirWhenExpression && !resultExpression.isExhaustive) return

        val functionReturnType = if (targetElement is FirConstructor)
            context.session.builtinTypes.unitType.coneType
        else
            targetElement.returnTypeRef.coneType
        if (targetElement is FirAnonymousFunction &&
            sourceKind is KtFakeSourceElementKind.ImplicitReturn.FromLastStatement &&
            functionReturnType.isUnit
        ) {
            return
        }

        val typeContext = context.session.typeContext
        val returnExpressionType = resultExpression.resolvedType

        if (!isSubtypeForTypeMismatch(typeContext, subtype = returnExpressionType, supertype = functionReturnType)) {
            if (resultExpression.isNullLiteral && functionReturnType.nullability == ConeNullability.NOT_NULL) {
                reporter.reportOn(resultExpression.source, NULL_FOR_NONNULL_TYPE, functionReturnType, context)
            } else {
                val isDueToNullability =
                    context.session.typeContext.isTypeMismatchDueToNullability(returnExpressionType, functionReturnType)
                if (resultExpression is FirSmartCastExpression && !resultExpression.isStable &&
                    isSubtypeForTypeMismatch(typeContext, subtype = resultExpression.smartcastType.coneType, supertype = functionReturnType)
                ) {
                    reporter.reportOn(
                        resultExpression.source,
                        SMARTCAST_IMPOSSIBLE,
                        functionReturnType,
                        resultExpression,
                        resultExpression.smartcastStability.description,
                        isDueToNullability,
                        context
                    )
                } else {
                    reporter.reportOn(
                        resultExpression.source,
                        RETURN_TYPE_MISMATCH,
                        functionReturnType,
                        returnExpressionType,
                        targetElement,
                        isDueToNullability,
                        context
                    )
                }
            }
        } else if (resultExpression.source?.kind is KtFakeSourceElementKind.ImplicitUnit &&
            !functionReturnType.lowerBoundIfFlexible().isUnit &&
            shouldCheckMismatchForAnonymousFunction(targetElement, expression)
        ) {
            // Disallow cases like
            //     fun foo(): Any { return }
            // Allow cases like
            //     fun foo(): Unit { return }
            //     fun foo() { return Unit }
            // But ignore anonymous functions without explicit returns, the following code is valid (where type of parameter R is being inferenced):
            //     run {
            //        if (flag) return@run
            //        "str"
            //     }
            // If type parameter is specified explicitly, checking is performed in the branch above and RETURN_TYPE_MISMATCH is reported
            reporter.reportOn(
                resultExpression.source,
                RETURN_TYPE_MISMATCH,
                functionReturnType,
                returnExpressionType,
                targetElement,
                false,
                context
            )
        }
    }

    private fun shouldCheckMismatchForAnonymousFunction(targetElement: FirFunction, expression: FirReturnExpression): Boolean {
        if (targetElement !is FirAnonymousFunction || !targetElement.isLambda) return true
        val cfgNodes = targetElement.controlFlowGraphReference?.controlFlowGraph?.exitNode?.previousCfgNodes ?: return true
        // Check if any return expression other than the current is explicit and not Unit
        return cfgNodes.any {
            val fir = it.fir
            if (fir === expression) false else fir is FirReturnExpression && !fir.result.resolvedType.isUnit
        }
    }
}

