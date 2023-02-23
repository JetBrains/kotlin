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
import org.jetbrains.kotlin.fir.analysis.checkers.ConeTypeCompatibilityChecker.collectUpperBounds
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumEntry
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.platformClassMapper
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.text
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

object FirEqualityCompatibilityChecker : FirEqualityOperatorCallChecker() {
    override fun check(expression: FirEqualityOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val arguments = expression.argumentList.arguments
        require(arguments.size == 2) { "Equality operator call with non-2 arguments" }

        val l = arguments[0].toArgumentInfo(context)
        val r = arguments[1].toArgumentInfo(context)

        checkSensibleness(l.smartCastType, r.smartCastType, context, expression, reporter)

        val checkApplicability = when (expression.operation) {
            FirOperation.EQ, FirOperation.NOT_EQ -> ::checkEqualityApplicability
            FirOperation.IDENTITY, FirOperation.NOT_IDENTITY -> ::checkIdentityApplicability
            else -> error("Invalid operator of FirEqualityOperatorCall")
        }

        checkApplicability(l.originalTypeInfo, r.originalTypeInfo, context).ifInapplicable {
            return reporter.reportInapplicabilityDiagnostic(
                expression, it, expression.operation, isWarning = false,
                l.originalTypeInfo, r.originalTypeInfo,
                l.userType, r.userType, context,
            )
        }

        if (l.argument !is FirSmartCastExpression && r.argument !is FirSmartCastExpression) {
            return
        }

        checkApplicability(l.smartCastTypeInfo, r.smartCastTypeInfo, context).ifInapplicable {
            return reporter.reportInapplicabilityDiagnostic(
                expression, it, expression.operation, isWarning = true,
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
            l.type.isIdentityLess(context) || r.type.isIdentityLess(context) -> Applicability.INAPPLICABLE_AS_IDENTITY_LESS
            oneIsBuiltin && oneIsNotNull && shouldReportAsPerRules1(l, r, context) -> getInapplicabilityFor(l, r)
            else -> Applicability.APPLICABLE
        }
    }

    private fun getInapplicabilityFor(l: TypeInfo, r: TypeInfo): Applicability {
        val isIntersectionEmpty = l.enforcesEmptyIntersection || r.enforcesEmptyIntersection
        val isOneEnum = l.isEnumClass || r.isEnumClass

        return when {
            !isIntersectionEmpty && isOneEnum -> {
                Applicability.INAPPLICABLE_AS_ENUMS
            }
            else -> Applicability.GENERALLY_INAPPLICABLE
        }
    }

    private fun ConeKotlinType.isIdentityLess(context: CheckerContext) = isPrimitive || !isNullable && isValueClass(context.session)

    private fun shouldReportAsPerRules1(l: TypeInfo, r: TypeInfo, context: CheckerContext): Boolean {
        return areUnrelatedClasses(l, r, context)
                || areInterfaceAndUnrelatedFinalClassAccordingly(l, r, context)
                || areInterfaceAndUnrelatedFinalClassAccordingly(r, l, context)
    }

    private fun areUnrelatedClasses(l: TypeInfo, r: TypeInfo, context: CheckerContext): Boolean {
        fun TypeInfo.isSubclassOf(other: TypeInfo) = when {
            other.enforcesEmptyIntersection -> type.classId == other.type.classId
            else -> notNullType.isSubtypeOf(other.notNullType, context.session)
        }

        return when {
            !l.isClass || !r.isClass -> false
            l.type.isNothingOrNullableNothing || r.type.isNothingOrNullableNothing -> false
            else -> !l.isSubclassOf(r) && !r.isSubclassOf(l)
        }
    }

    private fun areInterfaceAndUnrelatedFinalClassAccordingly(
        l: TypeInfo,
        r: TypeInfo,
        context: CheckerContext,
    ): Boolean {
        return l.isInterface && r.isFinalClass && !r.type.isNothingOrNullableNothing
                && !r.notNullType.isSubtypeOf(l.notNullType, context.session)
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

    private fun getGeneralInapplicabilityDiagnostic(isWarning: Boolean) = when {
        isWarning -> FirErrors.EQUALITY_NOT_APPLICABLE_WARNING
        else -> FirErrors.EQUALITY_NOT_APPLICABLE
    }

    private fun getIdentityLessInapplicabilityDiagnostic(
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        isWarning: Boolean,
        context: CheckerContext,
    ): KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> {
        val areBothPrimitives = lType.isPrimitive && rType.isPrimitive
        val areSameTypes = lType.classId == rType.classId
        val shouldProperlyReportError = context.languageVersionSettings.supportsFeature(LanguageFeature.ReportErrorsForComparisonOperators)

        // In this case K1 reports nothing
        val shouldRelaxDiagnostic = (lType.isNullableNothing || rType.isNullableNothing) && !shouldProperlyReportError

        return when {
            // See: KT-28252
            areSameTypes && areBothPrimitives -> FirErrors.DEPRECATED_IDENTITY_EQUALS
            // The same reason as above
            isIdentityComparedWithImplicitBoxing(lType, rType, context.session) -> FirErrors.IMPLICIT_BOXING_IN_IDENTITY_EQUALS
            isWarning || shouldRelaxDiagnostic -> FirErrors.FORBIDDEN_IDENTITY_EQUALS_WARNING
            else -> FirErrors.FORBIDDEN_IDENTITY_EQUALS
        }
    }

    private fun isIdentityComparedWithImplicitBoxing(lType: ConeKotlinType, rType: ConeKotlinType, session: FirSession) =
        isPrimitiveWithNonPrimitiveSupertype(lType, rType, session) || isPrimitiveWithNonPrimitiveSupertype(rType, lType, session)

    private fun isPrimitiveWithNonPrimitiveSupertype(lType: ConeKotlinType, rType: ConeKotlinType, session: FirSession) =
        lType.isPrimitive && !rType.isPrimitive && lType.isSubtypeOf(rType, session)

    private fun getSourceLessInapplicabilityDiagnostic(isWarning: Boolean) = when {
        isWarning -> FirErrors.INCOMPATIBLE_TYPES_WARNING
        else -> FirErrors.INCOMPATIBLE_TYPES
    }

    private fun getEnumInapplicabilityDiagnostic(
        l: TypeInfo,
        r: TypeInfo,
        isWarning: Boolean,
        context: CheckerContext,
    ): KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> {
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
            isWarning || isOldTestData || shouldRelaxDiagnostic -> FirErrors.INCOMPATIBLE_ENUM_COMPARISON
            else -> FirErrors.INCOMPATIBLE_ENUM_COMPARISON_ERROR
        }
    }

    private fun DiagnosticReporter.reportInapplicabilityDiagnostic(
        expression: FirEqualityOperatorCall,
        applicability: Applicability,
        operation: FirOperation,
        isWarning: Boolean,
        l: TypeInfo,
        r: TypeInfo,
        lUserType: ConeKotlinType,
        rUserType: ConeKotlinType,
        context: CheckerContext,
    ): Unit = when {
        applicability == Applicability.INAPPLICABLE_AS_IDENTITY_LESS -> reportOn(
            expression.source, getIdentityLessInapplicabilityDiagnostic(l.type, r.type, isWarning, context),
            lUserType, rUserType, context,
        )
        applicability == Applicability.INAPPLICABLE_AS_ENUMS -> reportOn(
            expression.source, getEnumInapplicabilityDiagnostic(l, r, isWarning, context),
            lUserType, rUserType, context,
        )
        // This check ensures K2 reports the same diagnostics as K1 used to.
        expression.source?.kind !is KtRealSourceElementKind -> reportOn(
            expression.source, getSourceLessInapplicabilityDiagnostic(isWarning),
            lUserType, rUserType, context,
        )
        applicability == Applicability.GENERALLY_INAPPLICABLE -> reportOn(
            expression.source, getGeneralInapplicabilityDiagnostic(isWarning),
            operation.operator, lUserType, rUserType, context,
        )
        else -> error("Shouldn't be here")
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

private class TypeInfo(val type: ConeKotlinType, session: FirSession) {
    val notNullType = type.withNullability(ConeNullability.NOT_NULL, session.typeContext)

    val superTypesTraverser = ConeSupertypesTraverser(type.lowerBoundIfFlexible(), session)

    val isClass by superTypesTraverser.anySuperClassSymbol { it.isClass }

    val hasInterfaceSupertype by superTypesTraverser.anySuperClassSymbol { !it.isClass }

    val isFinalClass by superTypesTraverser.anySuperClassSymbol { it.isFinalClass }

    val isEnumEntry by superTypesTraverser.anySuperClassSymbol { it.isEnumEntry }

    val hasEnumSupertype by superTypesTraverser.anySuperClassSymbol { it.isEnumClass }

    override fun toString() = "$type"
}

private val TypeInfo.isInterface get() = hasInterfaceSupertype && !isClass

private val TypeInfo.isBuiltin get() = type.isPrimitiveOrNullablePrimitive || type.isStringOrNullableString || isEnumClass

private val TypeInfo.isEnumClass get() = hasEnumSupertype && !isEnumEntry

// Enum classes are final, but enum entries are their subclasses.
private val TypeInfo.enforcesEmptyIntersection get() = isFinalClass && !isEnumClass

private val TypeInfo.isNullableEnum get() = isEnumClass && type.isNullable

private val FirClassSymbol<*>.isFinalClass get() = isClass && isFinal

// NB: This is what RULES1 means then it says "class".
private val FirClassSymbol<*>.isClass get() = !isInterface

private fun ConeKotlinType.toTypeInfo(session: FirSession): TypeInfo {
    return TypeInfo(toUpperBound(session), session)
}

private fun ConeKotlinType.toUpperBound(session: FirSession) =
    collectUpperBounds()
        .map { type -> session.toKotlinType(type).replaceArgumentsWithStarProjections() }
        .ifNotEmpty { ConeTypeIntersector.intersectTypes(session.typeContext, this) }
        ?: session.builtinTypes.nullableAnyType.type

private fun FirSession.toKotlinType(type: ConeClassLikeType): ConeClassLikeType {
    return platformClassMapper.getCorrespondingKotlinClass(type.lookupTag.classId)
        // Type arguments are ignored by design
        ?.let { ConeClassLikeTypeImpl(it.toLookupTag(), type.typeArguments, type.isNullable) }
        ?: type
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

private val FirExpression.mostOriginalTypeIfSmartCast: ConeKotlinType
    get() = when (this) {
        is FirSmartCastExpression -> originalExpression.mostOriginalTypeIfSmartCast
        else -> typeRef.coneType
    }

private fun FirExpression.toArgumentInfo(context: CheckerContext) =
    ArgumentInfo(
        this, typeRef.coneType, mostOriginalTypeIfSmartCast.fullyExpandedType(context.session), context.session,
    )
