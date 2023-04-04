/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.isPrimitiveType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

object FirEqualityCompatibilityChecker : FirEqualityOperatorCallChecker() {
    override fun check(expression: FirEqualityOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val arguments = expression.argumentList.arguments
        require(arguments.size == 2) { "Equality operator call with non-2 arguments" }

        val l = arguments[0].toArgumentInfo(context)
        val r = arguments[1].toArgumentInfo(context)

        checkSenselessness(l.smartCastType, r.smartCastType, context, expression, reporter)

        val checkApplicability = when (expression.operation) {
            FirOperation.EQ, FirOperation.NOT_EQ -> ::checkEqualityApplicability
            FirOperation.IDENTITY, FirOperation.NOT_IDENTITY -> ::checkIdentityApplicability
            else -> error("Invalid operator of FirEqualityOperatorCall")
        }

        checkApplicability(l.originalTypeInfo, r.originalTypeInfo, context).ifInapplicable {
            // Ideally this should match cases when K1
            // sees a non-empty intersection and none of the
            // types is an enum, but intersections in K1
            // work differently from intersections in K2.
            val isCaseMissedByK1 = it != Applicability.INAPPLICABLE_AS_ENUMS
                    && l.originalTypeInfo.isLiterallyTypeParameter
                    && r.originalTypeInfo.isLiterallyTypeParameter
            val replicateK1Behavior = !context.languageVersionSettings.supportsFeature(LanguageFeature.ReportErrorsForComparisonOperators)

            return reporter.reportInapplicabilityDiagnostic(
                expression, it, expression.operation, forceWarning = isCaseMissedByK1 && replicateK1Behavior,
                l.originalTypeInfo, r.originalTypeInfo,
                l.userType, r.userType, context,
            )
        }

        if (l.argument !is FirSmartCastExpression && r.argument !is FirSmartCastExpression) {
            return
        }

        checkApplicability(l.smartCastTypeInfo, r.smartCastTypeInfo, context).ifInapplicable {
            return reporter.reportInapplicabilityDiagnostic(
                expression, it, expression.operation, forceWarning = true,
                l.smartCastTypeInfo, r.smartCastTypeInfo,
                l.userType, r.userType, context,
            )
        }
    }

    private fun checkEqualityApplicability(l: TypeInfo, r: TypeInfo, context: CheckerContext): Applicability {
        val oneIsBuiltin = l.isBuiltin || r.isBuiltin

        // The compiler should only check comparisons
        // when builtins are involved.
        // Builtins' supertypes must not be present in
        // the list of special fqNames described in RULES1

        return when {
            oneIsBuiltin && shouldReportAsPerRules1(l, r, context) -> getInapplicabilityFor(l, r)
            else -> Applicability.APPLICABLE
        }
    }

    private fun checkIdentityApplicability(l: TypeInfo, r: TypeInfo, context: CheckerContext): Applicability {
        // The compiler should only check comparisons
        // when identity-less types or builtins are involved.

        val oneIsBuiltin = l.isBuiltin || r.isBuiltin
        val oneIsNotNull = !l.type.isNullable || !r.type.isNullable

        return when {
            l.isIdentityLess || r.isIdentityLess -> Applicability.INAPPLICABLE_AS_IDENTITY_LESS
            oneIsBuiltin && oneIsNotNull && shouldReportAsPerRules1(l, r, context) -> getInapplicabilityFor(l, r)
            else -> Applicability.APPLICABLE
        }
    }

    private fun getInapplicabilityFor(l: TypeInfo, r: TypeInfo): Applicability {
        val isIntersectionEmpty = l.enforcesEmptyIntersection || r.enforcesEmptyIntersection
        val isOneEnum = l.isEnumClass || r.isEnumClass

        return when {
            !isIntersectionEmpty && isOneEnum -> Applicability.INAPPLICABLE_AS_ENUMS
            else -> Applicability.GENERALLY_INAPPLICABLE
        }
    }

    private fun shouldReportAsPerRules1(l: TypeInfo, r: TypeInfo, context: CheckerContext): Boolean {
        // Builtins are always final classes, so
        // we only need to check if one is related
        // to the other

        fun TypeInfo.isSubtypeOf(other: TypeInfo) =
            notNullType.isSubtypeOf(other.notNullType, context.session)

        return when {
            l.type.isNothingOrNullableNothing || r.type.isNothingOrNullableNothing -> false
            else -> !l.isSubtypeOf(r) && !r.isSubtypeOf(l)
        }
    }

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

    private fun getGeneralInapplicabilityDiagnostic(forceWarning: Boolean) = when {
        forceWarning -> FirErrors.EQUALITY_NOT_APPLICABLE_WARNING
        else -> FirErrors.EQUALITY_NOT_APPLICABLE
    }

    private fun getIdentityLessInapplicabilityDiagnostic(
        l: TypeInfo,
        r: TypeInfo,
        forceWarning: Boolean,
        context: CheckerContext,
    ): KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> {
        val areBothPrimitives = l.isPrimitive && r.isPrimitive
        val areSameTypes = l.type.classId == r.type.classId
        val shouldProperlyReportError = context.languageVersionSettings.supportsFeature(LanguageFeature.ReportErrorsForComparisonOperators)

        // In this case K1 reports nothing
        val shouldRelaxDiagnostic = (l.type.isNullableNothing || r.type.isNullableNothing) && !shouldProperlyReportError

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
        isPrimitiveWithNonPrimitiveSupertype(l, r, session) || isPrimitiveWithNonPrimitiveSupertype(r, l, session)

    private fun isPrimitiveWithNonPrimitiveSupertype(l: TypeInfo, r: TypeInfo, session: FirSession) =
        l.isPrimitive && !r.isPrimitive && l.type.isSubtypeOf(r.type, session)

    private fun getSourceLessInapplicabilityDiagnostic(forceWarning: Boolean) = when {
        forceWarning -> FirErrors.INCOMPATIBLE_TYPES_WARNING
        else -> FirErrors.INCOMPATIBLE_TYPES
    }

    private fun getEnumInapplicabilityDiagnostic(
        l: TypeInfo,
        r: TypeInfo,
        forceWarning: Boolean,
        context: CheckerContext,
    ): KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> {
        // Preserving the behavior on the old test data
        // simplifies detecting fir-differences,
        // which is crucial for this checker
        val isOldTestData = !context.languageVersionSettings.supportsFeature(
            LanguageFeature.ProhibitComparisonOfIncompatibleEnums,
        )

        // In this corner case K1 reports nothing
        val bothNullableEnums = l.isNullableEnum && r.isNullableEnum
        // When comparing enums, for type parameters K1
        // tries to pick the "representative" superclass
        // instead of the proper intersection.
        // We can't guarantee that in such cases
        // K2 reports the same as K1
        val areIntersectionsInvolved = l.type is ConeIntersectionType || r.type is ConeIntersectionType

        val shouldProperlyReportError = context.languageVersionSettings.supportsFeature(LanguageFeature.ReportErrorsForComparisonOperators)
        val shouldRelaxDiagnostic = (bothNullableEnums || areIntersectionsInvolved) && !shouldProperlyReportError

        return when {
            forceWarning || isOldTestData || shouldRelaxDiagnostic -> FirErrors.INCOMPATIBLE_ENUM_COMPARISON
            else -> FirErrors.INCOMPATIBLE_ENUM_COMPARISON_ERROR
        }
    }

    private fun DiagnosticReporter.reportInapplicabilityDiagnostic(
        expression: FirEqualityOperatorCall,
        applicability: Applicability,
        operation: FirOperation,
        forceWarning: Boolean,
        l: TypeInfo,
        r: TypeInfo,
        lUserType: ConeKotlinType,
        rUserType: ConeKotlinType,
        context: CheckerContext,
    ): Unit = when {
        applicability == Applicability.INAPPLICABLE_AS_IDENTITY_LESS -> reportOn(
            expression.source, getIdentityLessInapplicabilityDiagnostic(l, r, forceWarning, context),
            lUserType, rUserType, context,
        )
        applicability == Applicability.INAPPLICABLE_AS_ENUMS -> reportOn(
            expression.source, getEnumInapplicabilityDiagnostic(l, r, forceWarning, context),
            lUserType, rUserType, context,
        )
        // This check ensures K2 reports the same diagnostics as K1 used to.
        expression.source?.kind !is KtRealSourceElementKind -> reportOn(
            expression.source, getSourceLessInapplicabilityDiagnostic(forceWarning),
            lUserType, rUserType, context,
        )
        applicability == Applicability.GENERALLY_INAPPLICABLE -> reportOn(
            expression.source, getGeneralInapplicabilityDiagnostic(forceWarning),
            operation.operator, lUserType, rUserType, context,
        )
        else -> error("Shouldn't be here")
    }

    private fun checkSenselessness(
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

private class TypeInfo(
    val type: ConeKotlinType,
    val notNullType: ConeKotlinType,
    val isFinalClass: Boolean,
    val isEnumClass: Boolean,
    val isPrimitive: Boolean,
    val isBuiltin: Boolean,
    val isValueClass: Boolean,
    val isLiterallyTypeParameter: Boolean,
) {
    override fun toString() = "$type"
}

private val FirClassSymbol<*>.isBuiltin get() = isPrimitiveType() || classId == StandardClassIds.String || isEnumClass

// This property is used to replicate K1 behavior, and it
// tries to predict empty intersections from the K1 point-of-view.
// Enum classes are final, but enum entries are their subclasses.
private val TypeInfo.enforcesEmptyIntersection get() = isFinalClass && !isEnumClass

private val TypeInfo.isNullableEnum get() = isEnumClass && type.isNullable

private val TypeInfo.isIdentityLess get() = isPrimitive || !type.isNullable && isValueClass

private val FirClassSymbol<*>.isFinalClass get() = isClass && isFinal

// NB: This is what RULES1 means then it says "class".
private val FirClassSymbol<*>.isClass get() = !isInterface

private fun ConeKotlinType.toTypeInfo(session: FirSession): TypeInfo {
    val bounds = collectUpperBounds().map { type -> toKotlinType(type).replaceArgumentsWithStarProjections() }
    val type = bounds.ifNotEmpty { ConeTypeIntersector.intersectTypes(session.typeContext, this) }
        ?: session.builtinTypes.nullableAnyType.type
    val notNullType = type.withNullability(ConeNullability.NOT_NULL, session.typeContext)

    return TypeInfo(
        type, notNullType,
        isFinalClass = bounds.any { it.toClassSymbol(session)?.isFinalClass == true },
        isEnumClass = bounds.any { it.toClassSymbol(session)?.isEnumClass == true },
        isPrimitive = bounds.any { it.isPrimitive },
        isBuiltin = bounds.any { it.toClassSymbol(session)?.isBuiltin == true },
        isValueClass = bounds.any { it.toClassSymbol(session)?.isInline == true },
        isLiterallyTypeParameter = this.lowerBoundIfFlexible() is ConeTypeParameterType,
    )
}

private fun toKotlinType(type: ConeClassLikeType): ConeClassLikeType {
    // Type arguments are ignored by design
    return ConeClassLikeTypeImpl(type.lookupTag, type.typeArguments, type.isNullable)
}

private class ArgumentInfo(
    val argument: FirExpression,
    val userType: ConeKotlinType,
    val originalType: ConeKotlinType,
    val session: FirSession,
) {
    val smartCastType: ConeKotlinType by lazy {
        if (argument !is FirSmartCastExpression) originalType else userType.fullyExpandedType(session)
    }

    val originalTypeInfo get() = originalType.toTypeInfo(session)

    val smartCastTypeInfo get() = smartCastType.toTypeInfo(session)

    override fun toString() = "${argument.source?.text} :: $userType"
}

@Suppress("RecursivePropertyAccessor")
private val FirExpression.mostOriginalTypeIfSmartCast: ConeKotlinType
    get() = when (this) {
        is FirSmartCastExpression -> originalExpression.mostOriginalTypeIfSmartCast
        else -> typeRef.coneType
    }

private fun FirExpression.toArgumentInfo(context: CheckerContext) =
    ArgumentInfo(
        this, typeRef.coneType, mostOriginalTypeIfSmartCast.fullyExpandedType(context.session), context.session,
    )
