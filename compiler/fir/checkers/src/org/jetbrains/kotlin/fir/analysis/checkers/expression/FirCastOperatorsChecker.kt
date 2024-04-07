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
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

object FirCastOperatorsChecker : FirTypeOperatorCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirTypeOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val arguments = expression.argumentList.arguments
        require(arguments.size == 1) { "Type operator call with non-1 arguments" }

        val l = arguments[0].toArgumentInfo(context)
        val r = expression.conversionTypeRef.coneType
            .fullyExpandedType(context.session)
            .finalApproximationOrSelf(context)
            .toTypeInfo(context.session)

        if (expression.operation in FirOperation.TYPES && r.directType is ConeDynamicType) {
            reporter.reportOn(expression.conversionTypeRef.source, FirErrors.DYNAMIC_NOT_ALLOWED, context)
        }

        val checkApplicability = when (expression.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> ::checkIsApplicability
            FirOperation.AS, FirOperation.SAFE_AS -> ::checkAsApplicability
            else -> error("Invalid operator of FirTypeOperatorCall")
        }

        val rUserType = expression.conversionTypeRef.coneType.finalApproximationOrSelf(context)

        // No need to check original types separately from smartcast types, because we only report warnings
        checkApplicability(l.smartCastTypeInfo, r, expression, context).ifInapplicable {
            return reporter.reportInapplicabilityDiagnostic(expression, it, l.originalTypeInfo, r.type, l.userType, rUserType, context)
        }
    }

    private fun checkIsApplicability(l: TypeInfo, r: TypeInfo, expression: FirTypeOperatorCall, context: CheckerContext): Applicability =
        checkAnyApplicability(l, r, expression, Applicability.IMPOSSIBLE_IS_CHECK, Applicability.USELESS_IS_CHECK, context)

    private fun checkAsApplicability(l: TypeInfo, r: TypeInfo, expression: FirTypeOperatorCall, context: CheckerContext): Applicability {
        val isNullableNothingWithNotNull = !l.type.isNullable && r.type.isNullableNothing
                || l.type.isNullableNothing && !r.type.isNullable

        return when {
            l.type.isNothing -> Applicability.APPLICABLE
            r.type.isNothing -> Applicability.IMPOSSIBLE_CAST
            isNullableNothingWithNotNull -> when (expression.operation) {
                // (null as? WhatEver) == null
                FirOperation.SAFE_AS -> Applicability.USELESS_CAST
                else -> Applicability.IMPOSSIBLE_CAST
            }
            else -> checkAnyApplicability(l, r, expression, Applicability.IMPOSSIBLE_CAST, Applicability.USELESS_CAST, context)
        }
    }

    private fun checkAnyApplicability(
        l: TypeInfo, r: TypeInfo,
        expression: FirTypeOperatorCall,
        impossible: Applicability,
        useless: Applicability,
        context: CheckerContext,
    ): Applicability {
        val oneIsNotNull = !l.type.isNullable || !r.type.isNullable

        return when {
            isRefinementUseless(context, l.directType.upperBoundIfFlexible(), r.directType, expression) -> useless
            shouldReportAsPerRules1(l, r, context) -> when {
                oneIsNotNull -> impossible
                else -> useless
            }
            isCastErased(l.directType, r.directType, context) -> Applicability.CAST_ERASED
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

    private inline fun Applicability.ifInapplicable(block: (Applicability) -> Unit) = when (this) {
        Applicability.APPLICABLE -> {}
        else -> block(this)
    }

    private fun DiagnosticReporter.reportInapplicabilityDiagnostic(
        expression: FirTypeOperatorCall,
        applicability: Applicability,
        l: TypeInfo,
        r: ConeKotlinType,
        lUserType: ConeKotlinType,
        rUserType: ConeKotlinType,
        context: CheckerContext,
    ) {
        when (applicability) {
            Applicability.IMPOSSIBLE_CAST -> getImpossibilityDiagnostic(l, r, context)?.let {
                reportOn(expression.source, it, context)
            }
            Applicability.USELESS_CAST -> getUselessCastDiagnostic(context)?.let {
                reportOn(expression.source, it, context)
            }
            Applicability.IMPOSSIBLE_IS_CHECK -> reportOn(
                expression.source, FirErrors.USELESS_IS_CHECK, expression.operation != FirOperation.IS, context,
            )
            Applicability.USELESS_IS_CHECK -> reportOn(
                expression.source, FirErrors.USELESS_IS_CHECK, expression.operation == FirOperation.IS, context,
            )
            Applicability.CAST_ERASED -> when {
                expression.operation == FirOperation.AS || expression.operation == FirOperation.SAFE_AS -> {
                    reportOn(expression.source, FirErrors.UNCHECKED_CAST, lUserType, rUserType, context)
                }
                else -> reportOn(expression.conversionTypeRef.source, FirErrors.CANNOT_CHECK_FOR_ERASED, rUserType, context)
            }
            else -> error("Shouldn't be here")
        }
    }

    private fun getImpossibilityDiagnostic(l: TypeInfo, rType: ConeKotlinType, context: CheckerContext) = when {
        !context.languageVersionSettings.supportsFeature(LanguageFeature.EnableDfaWarningsInK2) -> null
        context.session.firPlatformSpecificCastChecker.shouldSuppressImpossibleCast(context.session, l.type, rType) -> null
        else -> FirErrors.CAST_NEVER_SUCCEEDS
    }

    private fun getUselessCastDiagnostic(context: CheckerContext) = when {
        !context.languageVersionSettings.supportsFeature(LanguageFeature.EnableDfaWarningsInK2) -> null
        else -> FirErrors.USELESS_CAST
    }
}
