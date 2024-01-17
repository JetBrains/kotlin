/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isFunctionForExpectTypeFromCastFeature
import org.jetbrains.kotlin.fir.analysis.checkers.isUpcast
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirUselessTypeOperationCallChecker : FirTypeOperatorCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirTypeOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.operation !in FirOperation.TYPES) return
        val arg = expression.argument

        val lhsType = arg.resolvedType.upperBoundIfFlexible().fullyExpandedType(context.session)
        if (lhsType is ConeErrorType) return

        val targetType = expression.conversionTypeRef.coneType.fullyExpandedType(context.session)
        if (targetType is ConeErrorType) return

        if (isRefinementUseless(context, lhsType, targetType, expression, arg)) {
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
        lhsType: ConeSimpleKotlinType,
        targetType: ConeKotlinType,
        expression: FirTypeOperatorCall,
        arg: FirExpression,
    ): Boolean {
        return when (expression.operation) {
            FirOperation.AS, FirOperation.SAFE_AS -> {
                if (arg is FirFunctionCall) {
                    val functionSymbol = arg.toResolvedCallableSymbol(context.session) as? FirFunctionSymbol<*>
                    if (functionSymbol != null && functionSymbol.isFunctionForExpectTypeFromCastFeature()) return false
                }

                // Normalize `targetType` for cases like the following:
                // fun f(x: Int?) { x as? Int } // USELESS_CAST is reasonable here
                val refinedTargetType =
                    if (expression.operation == FirOperation.SAFE_AS && lhsType.isNullable) {
                        targetType.withNullability(ConeNullability.NULLABLE, context.session.typeContext)
                    } else {
                        targetType
                    }
                isExactTypeCast(context, lhsType, refinedTargetType)
            }
            FirOperation.IS, FirOperation.NOT_IS -> {
                isUpcast(context, lhsType, targetType)
            }
            else -> throw AssertionError("Should not be here: ${expression.operation}")
        }
    }

    private fun isExactTypeCast(context: CheckerContext, lhsType: ConeSimpleKotlinType, targetType: ConeKotlinType): Boolean {
        if (!AbstractTypeChecker.equalTypes(context.session.typeContext, lhsType, targetType, stubTypesEqualToAnything = false))
            return false
        // See comments at [isUpcast] why we need to check the existence of @ExtensionFunctionType
        return lhsType.isExtensionFunctionType == targetType.isExtensionFunctionType
    }
}
