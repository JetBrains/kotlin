/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.declarations.utils.isClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.resolve.isSubclassOf
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

object FirEqualityCompatibilityChecker : FirEqualityOperatorCallChecker() {
    override fun check(expression: FirEqualityOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val arguments = expression.argumentList.arguments
        if (arguments.size != 2) error("Equality operator call with non-2 arguments")

        val (lArgument, lType) = arguments[0].selfWithMostOriginalTypeIfSmartCast
        val (rArgument, rType) = arguments[1].selfWithMostOriginalTypeIfSmartCast

        val checkApplicability = when (expression.operation) {
            FirOperation.EQ, FirOperation.NOT_EQ -> ::checkEqualityApplicability
            FirOperation.IDENTITY, FirOperation.NOT_IDENTITY -> ::checkIdentityApplicability
            else -> error("Invalid operator of FirEqualityOperatorCall")
        }

        checkApplicability(lType, rType, context).ifInapplicable {
            return reporter.reportOn(
                expression, it, expression.operation,
                isWarning = false, lType, rType, context,
            )
        }

        if (lArgument !is FirSmartCastExpression && rArgument !is FirSmartCastExpression) {
            return
        }

        val lSmartCastType = lArgument.typeRef.coneType
        val rSmartCastType = rArgument.typeRef.coneType

        checkApplicability(lSmartCastType, rSmartCastType, context).ifInapplicable {
            return reporter.reportOn(
                expression, it, expression.operation,
                isWarning = true, lType, rType, context,
            )
        }
    }

    private val FirExpression.selfWithMostOriginalTypeIfSmartCast
        get() = this to mostOriginalTypeIfSmartCast

    private val FirExpression.mostOriginalTypeIfSmartCast: ConeKotlinType
        get() = when (this) {
            is FirSmartCastExpression -> originalExpression.mostOriginalTypeIfSmartCast
            else -> typeRef.coneType
        }

    private fun checkEqualityApplicability(
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        context: CheckerContext,
    ): Applicability {
        val oneIsBuiltin = lType.isBuiltin(context) || rType.isBuiltin(context)
        val oneIsNotNull = !lType.isNullable || !rType.isNullable

        // The compiler should only check comparisons
        // when builtins are involved.

        return when {
            !oneIsBuiltin || !oneIsNotNull || !shouldReportAsPerRules1(lType, rType, context) -> Applicability.APPLICABLE
            lType.isNullableNothing || rType.isNullableNothing -> Applicability.INAPPLICABLE_AS_SENSELESS
            lType.isEnumType(context) || rType.isEnumType(context) -> Applicability.INAPPLICABLE_AS_ENUMS
            else -> Applicability.GENERALLY_INAPPLICABLE
        }
    }

    private fun ConeKotlinType.isBuiltin(context: CheckerContext) = isPrimitiveOrNullablePrimitive || isString || isEnumType(context)

    private fun checkIdentityApplicability(
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        context: CheckerContext,
    ): Applicability {
        // The compiler should only check comparisons
        // when identity-less types are involved.

        return if (lType.isIdentityLess(context) || rType.isIdentityLess(context)) {
            Applicability.INAPPLICABLE_AS_IDENTITY_LESS
        } else {
            Applicability.APPLICABLE
        }
    }

    private fun ConeKotlinType.isIdentityLess(context: CheckerContext) = isIdentityLess(context.session)

    private fun ConeKotlinType.isIdentityLess(session: FirSession) = isPrimitive || !isNullable && isValueClass(session)

    private fun shouldReportAsPerRules1(lType: ConeKotlinType, rType: ConeKotlinType, context: CheckerContext): Boolean {
        val lClass = lType.representativeClassType(context)
        val rClass = rType.representativeClassType(context)

        return areUnrelatedClasses(lClass, rClass, context)
                || areInterfaceAndUnrelatedFinalClassAccordingly(lClass, rClass, context)
                || areInterfaceAndUnrelatedFinalClassAccordingly(rClass, lClass, context)
    }

    private fun areUnrelatedClasses(lClass: FirClassSymbol<*>, rClass: FirClassSymbol<*>, context: CheckerContext): Boolean {
        fun FirClassSymbol<*>.isSubclassOf(other: FirClassSymbol<*>) =
            isSubclassOf(other.toLookupTag(), context.session, isStrict = false, lookupInterfaces = true)

        return when {
            !lClass.isClass || !rClass.isClass -> false
            lClass.isFinal && rClass.isFinal && lClass != rClass -> true
            else -> !lClass.isSubclassOf(rClass) && !rClass.isSubclassOf(lClass)
        }
    }

    private fun areInterfaceAndUnrelatedFinalClassAccordingly(
        lClass: FirClassSymbol<*>,
        rClass: FirClassSymbol<*>,
        context: CheckerContext,
    ): Boolean {
        return lClass.isInterface && rClass.isFinalClass && !rClass.isSubclassOf(
            lClass.toLookupTag(), context.session, isStrict = false, lookupInterfaces = true,
        )
    }

    private val FirClassSymbol<*>.isFinalClass get() = isClass && isFinal

    private val representativeClassCache = mutableMapOf<ConeKotlinType, FirClassSymbol<*>>()

    private fun ConeKotlinType.representativeClassType(context: CheckerContext): FirClassSymbol<*> {
        val symbol = toSymbol(context.session)

        if (symbol is FirClassSymbol<*> && (symbol.isClass || symbol.isInterface)) {
            return symbol
        }

        return representativeClassType(context.session, representativeClassCache)
    }

    private fun ConeKotlinType.representativeClassType(
        session: FirSession,
        cache: MutableMap<ConeKotlinType, FirClassSymbol<*>>,
    ): FirClassSymbol<*> = cache.getOrPut(this) {
        when (val symbol = toSymbol(session)) {
            is FirClassSymbol<*> -> when {
                symbol.isClass -> symbol
                else -> symbol.resolvedSuperTypes.firstNonAnyRepresentativeClass(session, cache)
            }
            is FirTypeParameterSymbol -> symbol.resolvedBounds.map { it.type }.firstNonAnyRepresentativeClass(session, cache)
            is FirTypeAliasSymbol -> symbol.resolvedExpandedTypeRef.type.representativeClassType(session, cache)
            else -> session.anyClassSymbol
        }
    }

    private fun List<ConeKotlinType>.firstNonAnyRepresentativeClass(
        session: FirSession,
        cache: MutableMap<ConeKotlinType, FirClassSymbol<*>>,
    ): FirClassSymbol<*> {
        return firstNotNullOfOrNull { type ->
            type.representativeClassType(session, cache).takeIf {
                it != session.anyClassSymbol
            }
        } ?: session.anyClassSymbol
    }

    private val FirSession.anyClassSymbol
        get() = builtinTypes.anyType.coneType.toSymbol(this) as? FirClassSymbol<*>
            ?: error("Any type symbol is not a class symbol")

    private enum class Applicability {
        APPLICABLE,
        GENERALLY_INAPPLICABLE,
        INAPPLICABLE_AS_SENSELESS,
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

    private fun getEnumInapplicabilityDiagnostic(isWarning: Boolean) = when {
        isWarning -> FirErrors.INCOMPATIBLE_ENUM_COMPARISON
        else -> FirErrors.INCOMPATIBLE_ENUM_COMPARISON_ERROR
    }

    private fun getIdentityLessInapplicabilityDiagnostic(isWarning: Boolean) = when {
        isWarning -> FirErrors.FORBIDDEN_IDENTITY_EQUALS
        else -> FirErrors.FORBIDDEN_IDENTITY_EQUALS_WARNING
    }

    private fun getSourceLessInapplicabilityDiagnostic(isWarning: Boolean) = when {
        isWarning -> FirErrors.INCOMPATIBLE_TYPES_WARNING
        else -> FirErrors.INCOMPATIBLE_TYPES
    }

    private fun getSenselessComparisonInapplicabilityDiagnostic(isWarning: Boolean) = when {
        isWarning -> FirErrors.SENSELESS_COMPARISON
        else -> FirErrors.SENSELESS_COMPARISON_ERROR
    }

    private fun getSenselessWhenBranchInapplicabilityDiagnostic(isWarning: Boolean) = when {
        isWarning -> FirErrors.SENSELESS_NULL_IN_WHEN
        else -> FirErrors.SENSELESS_NULL_IN_WHEN_ERROR
    }

    private fun isWhenBranch(source: KtSourceElement?, rType: ConeKotlinType): Boolean =
        source?.elementType != KtNodeTypes.BINARY_EXPRESSION && rType.isNullableNothing

    private fun DiagnosticReporter.reportOn(
        expression: FirEqualityOperatorCall,
        applicability: Applicability,
        operation: FirOperation,
        isWarning: Boolean,
        lType: ConeKotlinType,
        rType: ConeKotlinType,
        context: CheckerContext,
    ): Unit = when {
        applicability == Applicability.INAPPLICABLE_AS_IDENTITY_LESS -> reportOn(
            expression.source, getIdentityLessInapplicabilityDiagnostic(isWarning),
            lType, rType, context,
        )
        expression.source?.kind !is KtRealSourceElementKind -> reportOn(
            expression.source, getSourceLessInapplicabilityDiagnostic(isWarning),
            lType, rType, context,
        )
        applicability == Applicability.GENERALLY_INAPPLICABLE -> reportOn(
            expression.source, getGeneralInapplicabilityDiagnostic(isWarning),
            operation.operator, lType, rType, context,
        )
        applicability == Applicability.INAPPLICABLE_AS_ENUMS -> reportOn(
            expression.source, getEnumInapplicabilityDiagnostic(isWarning),
            lType, rType, context,
        )
        applicability == Applicability.INAPPLICABLE_AS_SENSELESS -> when {
            isWhenBranch(expression.source, rType) -> reportOn(
                expression.source, getSenselessWhenBranchInapplicabilityDiagnostic(isWarning), context,
            )
            else -> reportOn(
                expression.source, getSenselessComparisonInapplicabilityDiagnostic(isWarning),
                expression, false, context,
            )
        }
        else -> error("Shouldn't be here")
    }

    private fun ConeKotlinType.isEnumType(
        context: CheckerContext
    ): Boolean {
        if (isEnum) return true
        val firRegularClassSymbol = (this as? ConeClassLikeType)?.lookupTag?.toFirRegularClassSymbol(context.session) ?: return false
        return firRegularClassSymbol.isEnumClass
    }
}
