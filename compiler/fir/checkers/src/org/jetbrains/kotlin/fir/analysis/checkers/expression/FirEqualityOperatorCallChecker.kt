/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.isEnumClass
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker.areCompatible

object FirEqualityCompatibilityChecker : FirEqualityOperatorCallChecker() {
    override fun check(expression: FirEqualityOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val arguments = expression.argumentList.arguments
        if (arguments.size != 2) return
        val lType = arguments[0].typeRef.coneType
        val rType = arguments[1].typeRef.coneType
        // If one of the type is already `Nothing?`, we skip reporting further comparison. This is to allow comparing with `null`, which has
        // type `Nothing?`
        if (lType.isNullableNothing || rType.isNullableNothing) return
        val inferenceContext = context.session.inferenceComponents.ctx
        val intersectionType = inferenceContext.intersectTypesOrNull(listOf(lType, rType)) as? ConeIntersectionType ?: return

        val compatibility = intersectionType.intersectedTypes.areCompatible(inferenceContext)
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
}