/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.firPlatformSpecificCastChecker
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

object FirCastOperatorsChecker : FirTypeOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirTypeOperatorCall) {
        val arguments = expression.argumentList.arguments
        require(arguments.size == 1) { "Type operator call with non-1 arguments" }

        val l = arguments[0].toArgumentInfo()
        val r = expression.conversionTypeRef.coneType
            .fullyExpandedType()
            .finalApproximationOrSelf()
            .toTypeInfo(context.session)

        if (expression.operation in FirOperation.TYPES && r.directType is ConeDynamicType) {
            reporter.reportOn(expression.conversionTypeRef.source, FirErrors.DYNAMIC_NOT_ALLOWED)
        }

        val applicability = when (expression.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> checkIsApplicability(l.smartCastTypeInfo, r, expression)
            FirOperation.AS, FirOperation.SAFE_AS -> checkAsApplicability(l.smartCastTypeInfo, r, expression)
            else -> error("Invalid operator of FirTypeOperatorCall")
        }

        val rUserType = expression.conversionTypeRef.coneType.finalApproximationOrSelf()

        // No need to check original types separately from smartcast types, because we only report warnings
        if (applicability != Applicability.APPLICABLE) {
            reporter.reportInapplicabilityDiagnostic(expression, applicability, l, r.type, rUserType)
        }
    }

    context(context: CheckerContext)
    private fun checkIsApplicability(l: TypeInfo, r: TypeInfo, expression: FirTypeOperatorCall): Applicability = checkCastErased(l, r)
        .orIfApplicable {
            checkAnyApplicability(
                l, r, expression,
                Applicability.IMPOSSIBLE_IS_CHECK,
                Applicability.USELESS_IS_CHECK,
                isForIsApplicability = true,
            )
        }

    context(context: CheckerContext)
    private fun checkAsApplicability(l: TypeInfo, r: TypeInfo, expression: FirTypeOperatorCall): Applicability {
        val isNullableNothingWithNotNull = !l.type.isMarkedOrFlexiblyNullable && r.type.isNullableNothing
                || l.type.isNullableNothing && !r.type.isMarkedOrFlexiblyNullable

        return when {
            l.type.isNothing -> Applicability.APPLICABLE
            r.type.isNothing -> Applicability.IMPOSSIBLE_CAST
            isNullableNothingWithNotNull -> when (expression.operation) {
                // (null as? WhatEver) == null
                FirOperation.SAFE_AS -> Applicability.USELESS_CAST
                else -> Applicability.IMPOSSIBLE_CAST
            }
            // For `as`-casts, `CAST_ERASED` is an error and is more important, whereas
            // for `is`-checks, usually, diagnostics for useless checks are more useful.
            else -> checkAnyApplicability(
                l, r, expression,
                Applicability.IMPOSSIBLE_CAST,
                Applicability.USELESS_CAST,
                isForIsApplicability = false,
            ).orIfApplicable { checkCastErased(l, r) }
        }
    }

    context(context: CheckerContext)
    private fun checkCastErased(l: TypeInfo, r: TypeInfo): Applicability = when {
        !(context.isContractBody
                && LanguageFeature.AllowCheckForErasedTypesInContracts.isEnabled()
                ) && isCastErased(l.directType, r.directType) -> {
            Applicability.CAST_ERASED
        }
        else -> Applicability.APPLICABLE
    }

    context(context: CheckerContext)
    private fun checkAnyApplicability(
        l: TypeInfo, r: TypeInfo,
        expression: FirTypeOperatorCall,
        impossible: Applicability,
        useless: Applicability,
        isForIsApplicability: Boolean,
    ): Applicability {
        val oneIsNotNull = !l.type.isMarkedOrFlexiblyNullable || !r.type.isMarkedOrFlexiblyNullable

        return when {
            isRefinementUseless(l.directType.upperBoundIfFlexible(), r.directType, expression) -> useless
            shouldReportAsPerRules1(l, r) -> when {
                isForIsApplicability || oneIsNotNull -> impossible
                else -> useless
            }
            else -> Applicability.APPLICABLE
        }
    }

    /**
     * K1 reports different diagnostics for different
     * cases, and this enum helps to replicate the K1's
     * choice of diagnostics.
     *
     * Should the K2's diagnostic severity differ,
     * the proper version will be picked later
     * when reporting the diagnostic.
     */
    private enum class Applicability {
        APPLICABLE,
        IMPOSSIBLE_CAST,
        IMPOSSIBLE_IS_CHECK,
        USELESS_CAST,
        USELESS_IS_CHECK,
        CAST_ERASED,
    }

    private inline fun Applicability.orIfApplicable(other: () -> Applicability): Applicability {
        return when {
            this == Applicability.APPLICABLE -> other()
            else -> this
        }
    }


    context(context: CheckerContext)
    private fun DiagnosticReporter.reportInapplicabilityDiagnostic(
        expression: FirTypeOperatorCall,
        applicability: Applicability,
        l: ArgumentInfo,
        r: ConeKotlinType,
        rUserType: ConeKotlinType,
    ) {
        when (applicability) {
            Applicability.IMPOSSIBLE_CAST -> getImpossibilityDiagnostic(l.originalTypeInfo, r)?.let {
                reportOn(expression.source, it)
            }
            Applicability.USELESS_CAST -> getUselessCastDiagnostic()?.let {
                reportOn(expression.source, it)
            }
            Applicability.IMPOSSIBLE_IS_CHECK -> reportOn(
                expression.source,
                FirErrors.IMPOSSIBLE_IS_CHECK,
                expression.operation != FirOperation.IS
            )
            Applicability.USELESS_IS_CHECK -> when {
                !isLastBranchOfExhaustiveWhen(l, r) -> reportOn(
                    expression.source,
                    FirErrors.USELESS_IS_CHECK,
                    expression.operation == FirOperation.IS
                )
            }
            Applicability.CAST_ERASED -> when {
                expression.operation == FirOperation.AS || expression.operation == FirOperation.SAFE_AS -> {
                    reportOn(expression.source, FirErrors.UNCHECKED_CAST, l.userType, rUserType)
                }
                else -> reportOn(expression.conversionTypeRef.source, FirErrors.CANNOT_CHECK_FOR_ERASED, rUserType)
            }
            else -> error("Shouldn't be here")
        }
    }

    context(context: CheckerContext)
    private fun isLastBranchOfExhaustiveWhen(l: ArgumentInfo, r: ConeKotlinType): Boolean {
        if (context.containingElements.size < 2) {
            return false
        }

        val (whenExpression, whenBranch) = context.containingElements.dropLast(1).takeLast(2)

        return whenExpression is FirWhenExpression && whenBranch is FirWhenBranch
                && whenExpression.isExhaustive && whenBranch == whenExpression.branches.lastOrNull()
                // Ensures it's not redundantly exhaustive
                && !l.argument.resolvedType.isNothing
                // Having an exhaustive `when` with only one branch is useless in general
                && (whenExpression.branches.size > 1 || l.smartCastTypeInfo.type.equalTypes(r, context.session))
    }

    context(context: CheckerContext)
    private fun getImpossibilityDiagnostic(l: TypeInfo, rType: ConeKotlinType) = when {
        !LanguageFeature.EnableDfaWarningsInK2.isEnabled() -> null
        context.session.firPlatformSpecificCastChecker.shouldSuppressImpossibleCast(context.session, l.type, rType) -> null
        else -> FirErrors.CAST_NEVER_SUCCEEDS
    }

    context(context: CheckerContext)
    private fun getUselessCastDiagnostic() = when {
        !LanguageFeature.EnableDfaWarningsInK2.isEnabled() -> null
        else -> FirErrors.USELESS_CAST
    }
}
