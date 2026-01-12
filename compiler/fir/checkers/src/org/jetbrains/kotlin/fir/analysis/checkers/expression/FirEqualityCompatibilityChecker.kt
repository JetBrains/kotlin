/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.analysis.checkers.firPlatformSpecificEqualityChecker
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.types.*

object FirEqualityCompatibilityChecker : FirEqualityOperatorCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirEqualityOperatorCall) {
        val arguments = expression.argumentList.arguments
        require(arguments.size == 2) { "Equality operator call with non-2 arguments" }

        val l = arguments[0].toArgumentInfo()
        val r = arguments[1].toArgumentInfo()

        checkSenselessness(l.smartCastType, r.smartCastType, expression)

        checkApplicability(expression.operation, l.originalTypeInfo, r.originalTypeInfo).ifInapplicable {
            // K1 checks consist of 2 parts: reporting a
            // diagnostic if the intersection is empty,
            // and otherwise reporting a diagnostic if
            // `isIncompatibleEnums` returns true.
            // In either case K1 may not report a diagnostic
            // due to some reasons, and we need to
            // account for them.
            val isCaseMissedByK1 = isCaseMissedByK1Intersector(l.originalTypeInfo, r.originalTypeInfo)
                    && isCaseMissedByAdditionalK1IncompatibleEnumsCheck(l.originalType, r.originalType, context.session)
            val replicateK1Behavior = !LanguageFeature.ReportErrorsForComparisonOperators.isEnabled()

            return reporter.reportInapplicabilityDiagnostic(
                expression, it, expression.operation, forceWarning = isCaseMissedByK1 && replicateK1Behavior,
                l.originalTypeInfo, r.originalTypeInfo,
                l.userType, r.userType,
            )
        }

        if (l.argument !is FirSmartCastExpression && r.argument !is FirSmartCastExpression) {
            return
        }

        checkApplicability(expression.operation, l.smartCastTypeInfo, r.smartCastTypeInfo).ifInapplicable {
            return reporter.reportInapplicabilityDiagnostic(
                expression, it, expression.operation, forceWarning = true,
                l.smartCastTypeInfo, r.smartCastTypeInfo,
                l.userType, r.userType,
            )
        }
    }

    context(context: CheckerContext)
    private fun checkApplicability(operation: FirOperation, l: TypeInfo, r: TypeInfo): Applicability = when (operation) {
        FirOperation.EQ, FirOperation.NOT_EQ -> { checkEqualityApplicability(l, r) }
        FirOperation.IDENTITY, FirOperation.NOT_IDENTITY -> { checkIdentityApplicability(l, r) }
        else -> error("Invalid operator of FirEqualityOperatorCall")
    }

    context(context: CheckerContext)
    private fun checkEqualityApplicability(l: TypeInfo, r: TypeInfo): Applicability {
        val oneIsBuiltin = l.isBuiltin || r.isBuiltin
        val oneIsIdentityLess = l.isIdentityLess(context.session) || r.isIdentityLess(context.session)

        // The compiler should only check comparisons
        // when builtins are involved.
        // Builtins' supertypes must not be present in
        // the list of special fqNames described in RULES1

        return when {
            (oneIsBuiltin || oneIsIdentityLess) && shouldReportAsPerRules1(l, r) -> getInapplicabilityFor(l, r)
            else -> Applicability.APPLICABLE
        }
    }

    context(context: CheckerContext)
    private fun checkIdentityApplicability(l: TypeInfo, r: TypeInfo): Applicability {
        val oneIsNotNull = !l.type.isMarkedOrFlexiblyNullable || !r.type.isMarkedOrFlexiblyNullable

        return when {
            l.type.isNullableNothing || r.type.isNullableNothing -> Applicability.APPLICABLE
            l.isIdentityLess(context.session) || r.isIdentityLess(context.session) -> Applicability.INAPPLICABLE_AS_IDENTITY_LESS
            oneIsNotNull && shouldReportAsPerRules1(l, r) -> getInapplicabilityFor(l, r)
            else -> Applicability.APPLICABLE
        }
    }

    private fun getInapplicabilityFor(l: TypeInfo, r: TypeInfo): Applicability {
        val isNonEmptyIntersectionInK1 = isCaseMissedByK1Intersector(l, r)
        val isOneEnum = l.isEnumClass || r.isEnumClass

        return when {
            // This code aims to replicate the K1's choice of diagnostics
            isNonEmptyIntersectionInK1 && isOneEnum -> Applicability.INAPPLICABLE_AS_ENUMS
            else -> Applicability.GENERALLY_INAPPLICABLE
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
        GENERALLY_INAPPLICABLE,
        INAPPLICABLE_AS_ENUMS,
        INAPPLICABLE_AS_IDENTITY_LESS,
    }

    private inline fun Applicability.ifInapplicable(block: (Applicability) -> Unit) = when (this) {
        Applicability.APPLICABLE -> {}
        else -> block(this)
    }

    context(context: CheckerContext)
    private fun getGeneralInapplicabilityDiagnostic(
        forceWarning: Boolean,
        operation: FirOperation,
        lType: ConeKotlinType,
        rType: ConeKotlinType,
    ): KtDiagnosticFactory3<String, ConeKotlinType, ConeKotlinType>? {
        val generalApplicabilityChecker = object : TypeOperationApplicabilityChecker() {
            override fun isApplicable(leftType: ConeKotlinType, rightType: ConeKotlinType) = checkApplicability(
                operation,
                leftType.toTypeInfo(context.session),
                rightType.toTypeInfo(context.session)
            ) == Applicability.APPLICABLE
        }

        return when {
            context.session.firPlatformSpecificEqualityChecker.shouldSuppressInapplicableEquality(
                lType,
                rType,
                generalApplicabilityChecker
            ) -> null
            forceWarning -> FirErrors.EQUALITY_NOT_APPLICABLE_WARNING
            else -> FirErrors.EQUALITY_NOT_APPLICABLE
        }
    }

    context(context: CheckerContext)
    private fun getIdentityLessInapplicabilityDiagnostic(
        l: TypeInfo,
        r: TypeInfo,
        forceWarning: Boolean,
    ): KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> {
        val areBothPrimitives = l.isNotNullPrimitive && r.isNotNullPrimitive
        val areSameTypes = l.type.classId == r.type.classId
        val shouldProperlyReportError = LanguageFeature.ReportErrorsForComparisonOperators.isEnabled()

        // In this case K1 reports nothing
        val shouldRelaxDiagnostic = (l.isPrimitive || r.isPrimitive) && areRelated(l, r)
                && !shouldProperlyReportError

        return when {
            // See: KT-28252
            areSameTypes && areBothPrimitives -> FirErrors.DEPRECATED_IDENTITY_EQUALS
            // The same reason as above
            isIdentityComparedWithImplicitBoxing(l, r, context.session) -> FirErrors.IMPLICIT_BOXING_IN_IDENTITY_EQUALS
            forceWarning || shouldRelaxDiagnostic -> FirErrors.FORBIDDEN_IDENTITY_EQUALS_WARNING
            else -> FirErrors.FORBIDDEN_IDENTITY_EQUALS
        }
    }

    private fun isIdentityComparedWithImplicitBoxing(l: TypeInfo, r: TypeInfo, session: FirSession) =
        arePrimitiveAndNonPrimitiveSupertypeRespectively(l, r, session) || arePrimitiveAndNonPrimitiveSupertypeRespectively(r, l, session)

    private fun arePrimitiveAndNonPrimitiveSupertypeRespectively(l: TypeInfo, r: TypeInfo, session: FirSession) =
        l.isNotNullPrimitive && !r.isNotNullPrimitive && l.type.isSubtypeOf(r.type, session)

    private fun getSourceLessInapplicabilityDiagnostic(forceWarning: Boolean) = when {
        forceWarning -> FirErrors.INCOMPATIBLE_TYPES_WARNING
        else -> FirErrors.INCOMPATIBLE_TYPES
    }

    context(context: CheckerContext)
    private fun getEnumInapplicabilityDiagnostic(
        l: TypeInfo,
        r: TypeInfo,
        forceWarning: Boolean,
    ): KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> {
        // In this corner case K1 reports nothing
        val bothNullableEnums = l.isNullableEnum && r.isNullableEnum
        // When comparing enums, for type parameters K1
        // tries to pick the "representative" superclass
        // instead of the proper intersection.
        // We can't guarantee that in such cases
        // K2 reports the same as K1
        val areIntersectionsInvolved = l.type is ConeIntersectionType || r.type is ConeIntersectionType

        val shouldProperlyReportError = LanguageFeature.ReportErrorsForComparisonOperators.isEnabled()
        val shouldRelaxDiagnostic = (bothNullableEnums || areIntersectionsInvolved) && !shouldProperlyReportError

        return when {
            forceWarning || shouldRelaxDiagnostic -> FirErrors.INCOMPATIBLE_ENUM_COMPARISON
            else -> FirErrors.INCOMPATIBLE_ENUM_COMPARISON_ERROR
        }
    }

    context(context: CheckerContext)
    private fun DiagnosticReporter.reportInapplicabilityDiagnostic(
        expression: FirEqualityOperatorCall,
        applicability: Applicability,
        operation: FirOperation,
        forceWarning: Boolean,
        l: TypeInfo,
        r: TypeInfo,
        lUserType: ConeKotlinType,
        rUserType: ConeKotlinType,
    ): Unit = when {
        applicability == Applicability.INAPPLICABLE_AS_IDENTITY_LESS -> reportOn(
            expression.source,
            getIdentityLessInapplicabilityDiagnostic(
                l,
                r,
                forceWarning
            ),
            lUserType,
            rUserType
        )
        applicability == Applicability.INAPPLICABLE_AS_ENUMS -> reportOn(
            expression.source,
            getEnumInapplicabilityDiagnostic(l, r, forceWarning),
            lUserType,
            rUserType
        )
        // This check ensures K2 reports the same diagnostics as K1 used to.
        expression.source?.kind !is KtRealSourceElementKind -> reportOn(
            expression.source,
            getSourceLessInapplicabilityDiagnostic(forceWarning),
            lUserType,
            rUserType
        )
        applicability == Applicability.GENERALLY_INAPPLICABLE ->
            getGeneralInapplicabilityDiagnostic(forceWarning, expression.operation, lUserType, rUserType)?.let {
                reportOn(
                    expression.source,
                    it,
                    operation.operator,
                    lUserType,
                    rUserType)
            } ?: Unit
        else -> error("Shouldn't be here")
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSenselessness(
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        expression: FirEqualityOperatorCall
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
        // We only report `SENSELESS_NULL_IN_WHEN` if `lType = type` because `lType` is the type of the `when` subject. This diagnostic is
        // only intended for cases where the branch condition contains a null. Also, the error message for SENSELESS_NULL_IN_WHEN
        // says the value is *never* equal to null, so we can't report it if the value is *always* equal to null.
        if (expression.source?.elementType != KtNodeTypes.BINARY_EXPRESSION && type === lType && !compareResult) {
            reporter.reportOn(expression.source, FirErrors.SENSELESS_NULL_IN_WHEN)
        } else {
            reporter.reportOn(expression.source, FirErrors.SENSELESS_COMPARISON, compareResult)
        }
    }
}

/**
 * Unfortunately, intersections in K1 are not
 * smart enough: K1 doesn't say that the
 * intersection is empty if all the input
 * types "can have subtypes". For example,
 * K1 thinks that type parameters always
 * allow subtypes, so it won't report
 * empty intersections for them regardless
 * the bounds.
 *
 * See: [org.jetbrains.kotlin.types.TypeIntersector.intersectTypes]
 */
internal fun isCaseMissedByK1Intersector(a: TypeInfo, b: TypeInfo) =
    a.canHaveSubtypesAccordingToK1 && b.canHaveSubtypesAccordingToK1

/**
 * This function simply replicates `if` with
 * early returns from the corresponding function
 * in K1.
 *
 * See: [org.jetbrains.kotlin.types.isIncompatibleEnums]
 */
internal fun isCaseMissedByAdditionalK1IncompatibleEnumsCheck(a: ConeKotlinType, b: ConeKotlinType, session: FirSession): Boolean {
    return when {
        !a.isEnum(session) && !b.isEnum(session) -> true
        a.isMarkedOrFlexiblyNullable && b.isMarkedOrFlexiblyNullable -> true
        a.isNothingOrNullableNothing || b.isNothingOrNullableNothing -> true
        else -> !a.isClass(session) || !b.isClass(session)
    }
}
