/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.compareTypesList
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RETURN_TYPE_MISMATCH
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.resolve.inference.isFunctionalType
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirFunctionReturnTypeMismatch : FirReturnExpressionChecker() {
    override fun check(expression: FirReturnExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val returnExpressionType = expression.result.typeRef.coneTypeSafe<ConeKotlinType>() ?: return
        val targetElement = expression.target.labeledElement

        if (!(targetElement is FirSimpleFunction || targetElement is FirPropertyAccessor)) return

        if (isNeedToBeReported(expression, targetElement, context.session.typeContext)) {
            val returnExpressionSource = expression.source ?: return
            val expectedConeType = targetElement.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return
            reporter.report(RETURN_TYPE_MISMATCH.on(returnExpressionSource, expectedConeType, returnExpressionType), context)
        }
    }

    private fun isNeedToBeReported(
        returnExpression: FirReturnExpression,
        function: FirFunction<*>,
        context: ConeInferenceContext
    ): Boolean {
        val functionReturnType = function.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return false
        val returnExpressionType = returnExpression.result.typeRef.coneTypeSafe<ConeKotlinType>() ?: return false
        val functionLastTypeArg = functionReturnType.typeArguments.lastOrNull()?.type

        if (functionReturnType.isFunctionalType(context.session) && functionLastTypeArg?.isUnit == true) {
            // hack: if type is (args) -> Unit, and function returns non-Unit value, the type of the function
            // will be non-unit, but it's OK
            if (returnExpressionType.isFunctionalType(context.session)) {
                // dropping the return type (getting only the functional type args)
                val expectedArgs = functionReturnType.typeArguments.dropLast(1)
                val actualArgs = returnExpressionType.typeArguments.dropLast(1)
                return !compareTypesList(actualArgs, expectedArgs, context)
            }
        }

        if (!AbstractTypeChecker.isSubtypeOf(context, returnExpressionType, functionReturnType))
            return true

        val returnResultType = returnExpression.result.typeRef.coneTypeSafe<ConeKotlinType>() ?: return false
        if (!AbstractTypeChecker.isSubtypeOf(context, returnResultType, functionReturnType))
            return true

        return false
    }
}