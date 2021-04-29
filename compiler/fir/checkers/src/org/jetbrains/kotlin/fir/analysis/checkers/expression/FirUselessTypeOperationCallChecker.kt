/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

// See .../types/CastDiagnosticsUtil.kt for counterparts, including isRefinementUseless, isExactTypeCast, isUpcast.
object FirUselessTypeOperationCallChecker : FirTypeOperatorCallChecker() {
    override fun check(expression: FirTypeOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.operation !in FirOperation.TYPES) return
        val arg = expression.argument

        val candidateType = arg.typeRef.coneType.upperBoundIfFlexible().fullyExpandedType(context.session)
        if (candidateType is ConeKotlinErrorType) return

        val targetType = expression.conversionTypeRef.coneType.fullyExpandedType(context.session)
        if (targetType is ConeKotlinErrorType) return

        // x as? Type <=> x as Type?
        val refinedTargetType =
            if (expression.operation == FirOperation.SAFE_AS) {
                targetType.withNullability(ConeNullability.NULLABLE, context.session.typeContext)
            } else {
                targetType
            }
        if (isRefinementUseless(context, candidateType, refinedTargetType, shouldCheckForExactType(expression, context))) {
            when (expression.operation) {
                FirOperation.IS -> reporter.reportOn(expression.source, FirErrors.USELESS_IS_CHECK, true, context)
                FirOperation.NOT_IS -> reporter.reportOn(expression.source, FirErrors.USELESS_IS_CHECK, false, context)
                FirOperation.AS, FirOperation.SAFE_AS -> reporter.reportOn(expression.source, FirErrors.USELESS_CAST, context)
                else -> throw AssertionError("Should not be here: ${expression.operation}")
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun shouldCheckForExactType(expression: FirTypeOperatorCall, context: CheckerContext): Boolean {
        return when (expression.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> false
            // TODO: differentiate if this expression defines the enclosing thing's type
            //   e.g.,
            //   val c1 get() = 1 as Number
            //   val c2: Number get() = 1 <!USELESS_CAST!>as Number<!>
            FirOperation.AS, FirOperation.SAFE_AS -> true
            else -> throw AssertionError("Should not be here: ${expression.operation}")
        }
    }

    private fun isRefinementUseless(
        context: CheckerContext,
        candidateType: ConeKotlinType,
        targetType: ConeKotlinType,
        shouldCheckForExactType: Boolean,
    ): Boolean {
        return if (shouldCheckForExactType) {
            isExactTypeCast(context, candidateType, targetType)
        } else {
            isUpcast(context, candidateType, targetType)
        }
    }

    private fun isExactTypeCast(context: CheckerContext, candidateType: ConeKotlinType, targetType: ConeKotlinType): Boolean {
        if (!AbstractTypeChecker.equalTypes(context.session.typeContext, candidateType, targetType, stubTypesEqualToAnything = false))
            return false
        // See comments at [isUpcast] why we need to check the existence of @ExtensionFunctionType
        return candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
    }

    private fun isUpcast(context: CheckerContext, candidateType: ConeKotlinType, targetType: ConeKotlinType): Boolean {
        if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, candidateType, targetType, stubTypesEqualToAnything = false))
            return false

        // E.g., foo(p1: (X) -> Y), where p1 has a functional type whose receiver type is X and return type is Y.
        // For bar(p2: X.() -> Y), p2 has the same functional type (with same receiver and return types).
        // The only difference is the existence of type annotation, @ExtensionFunctionType,
        //   which indicates that the annotated type represents an extension function.
        // If one casts p1 to p2 (or vice versa), it is _not_ up cast, i.e., not redundant, yet meaningful.
        return candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
    }
}
