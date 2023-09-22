/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isExactTypeCast
import org.jetbrains.kotlin.fir.analysis.checkers.isFunctionForExpectTypeFromCastFeature
import org.jetbrains.kotlin.fir.analysis.checkers.isUpcast
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*

object FirUselessTypeOperationCallChecker : FirTypeOperatorCallChecker() {
    override fun check(expression: FirTypeOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.operation !in FirOperation.TYPES) return
        val arg = expression.argument

        val candidateType = arg.resolvedType.upperBoundIfFlexible().fullyExpandedType(context.session)
        if (candidateType is ConeErrorType) return

        val targetType = expression.conversionTypeRef.coneType.fullyExpandedType(context.session)
        if (targetType is ConeErrorType) return

        // Convert `x as? Type` to `x as Type?`
        val refinedTargetType =
            if (expression.operation == FirOperation.SAFE_AS) {
                targetType.withNullability(ConeNullability.NULLABLE, context.session.typeContext)
            } else {
                targetType
            }
        if (isRefinementUseless(context, candidateType, refinedTargetType, expression, arg)) {
            when (expression.operation) {
                FirOperation.IS -> reporter.reportOn(expression.source, FirErrors.USELESS_IS_CHECK, true, context)
                FirOperation.NOT_IS -> reporter.reportOn(expression.source, FirErrors.USELESS_IS_CHECK, false, context)
                FirOperation.AS, FirOperation.SAFE_AS -> {
                    if (!expression.argFromStubType) {
                        reporter.reportOn(expression.source, FirErrors.USELESS_CAST, context)
                    }
                }
                else -> throw AssertionError("Should not be here: ${expression.operation}")
            }
        }
    }

    private fun isRefinementUseless(
        context: CheckerContext,
        candidateType: ConeSimpleKotlinType,
        targetType: ConeKotlinType,
        expression: FirTypeOperatorCall,
        arg: FirExpression,
    ): Boolean {
        return if (shouldCheckForExactType(expression, context)) {
            if (arg is FirFunctionCall) {
                val functionSymbol = arg.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
                if (functionSymbol != null && functionSymbol.isFunctionForExpectTypeFromCastFeature()) return false
            }

            isExactTypeCast(context, candidateType, targetType)
        } else {
            isUpcast(context, candidateType, targetType)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun shouldCheckForExactType(expression: FirTypeOperatorCall, context: CheckerContext): Boolean {
        return when (expression.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> false
            // TODO, KT-59820: differentiate if this expression defines the enclosing thing's type
            //   e.g.,
            //   val c1 get() = 1 as Number
            //   val c2: Number get() = 1 <!USELESS_CAST!>as Number<!>
            FirOperation.AS, FirOperation.SAFE_AS -> expression.usedAsExpression
            else -> throw AssertionError("Should not be here: ${expression.operation}")
        }
    }
}
