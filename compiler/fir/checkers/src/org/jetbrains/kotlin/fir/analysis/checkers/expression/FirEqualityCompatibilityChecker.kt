/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker.isCompatible
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*

object FirEqualityCompatibilityChecker : FirEqualityOperatorCallChecker() {
    override fun check(expression: FirEqualityOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val arguments = expression.argumentList.arguments
        if (arguments.size != 2) return
        val lType = arguments[0].typeRef.coneType
        val rType = arguments[1].typeRef.coneType
        checkCompatibility(lType, rType, context, expression, reporter)
        checkSensibleness(lType, rType, context, expression, reporter)
    }

    private fun checkCompatibility(
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        context: CheckerContext,
        expression: FirEqualityOperatorCall,
        reporter: DiagnosticReporter
    ) {
        // If one of the type is already `Nothing?`, we skip reporting further comparison. This is to allow comparing with `null`, which has
        // type `Nothing?`
        if (lType.isNullableNothing || rType.isNullableNothing) return
        val inferenceContext = context.session.typeContext

        val compatibility = try {
            inferenceContext.isCompatible(lType, rType)
        } catch (e: Throwable) {
            throw IllegalStateException(
                "Exception while determining type compatibility: lType: $lType, rType: $rType, " +
                        "equality ${expression.render()}, " +
                        "file ${context.containingFile?.name}",
                e
            )
        }
        if (compatibility != ConeTypeCompatibilityChecker.Compatibility.COMPATIBLE) {
            when (expression.source?.kind) {
                KtRealSourceElementKind -> {
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
        val firRegularClassSymbol = (this as? ConeClassLikeType)?.lookupTag?.toFirRegularClassSymbol(context.session) ?: return false
        return firRegularClassSymbol.isEnumClass
    }

    private fun checkSensibleness(
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        context: CheckerContext,
        expression: FirEqualityOperatorCall,
        reporter: DiagnosticReporter
    ) {
        val type = when {
            rType.isNullableNothing -> lType
            lType.isNullableNothing -> rType
            else -> return
        }
        if (type is ConeErrorType) return
        val isPositiveCompare = expression.operation == FirOperation.EQ || expression.operation == FirOperation.IDENTITY
        val compareResult = with(context.session.typeContext) {
            when {
                // `null` literal has type `Nothing?`
                type.isNullableNothing -> isPositiveCompare
                !type.isNullableType() -> !isPositiveCompare
                else -> return
            }
        }
        // We only report `SENSELESS_NULL_IN_WHEN` if `lType = type` because `lType` is the type of the when subject. This diagnostic is
        // only intended for cases where the branch condition contains a null. Also, the error message for SENSELESS_NULL_IN_WHEN
        // says the value is *never* equal to null, so we can't report it if the value is *always* equal to null.
        if (expression.source?.elementType != KtNodeTypes.BINARY_EXPRESSION && type === lType && !compareResult) {
            reporter.reportOn(expression.source, FirErrors.SENSELESS_NULL_IN_WHEN, context)
        } else {
            reporter.reportOn(expression.source, FirErrors.SENSELESS_COMPARISON, expression, compareResult, context)
        }
    }
}
