/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
private fun ConeKotlinType.classId(session: FirSession): ClassId? {
    contract {
        returns(true) implies (this@classId is ConeClassLikeType)
    }
    if (this !is ConeClassLikeType) return null
    return fullyExpandedType(session).lookupTag.classId
}

fun ConeKotlinType.isKMutableProperty(session: FirSession): Boolean {
    val classId = classId(session) ?: return false
    return classId.packageFqName == StandardClassIds.BASE_REFLECT_PACKAGE &&
            classId.shortClassName.identifier.startsWith("KMutableProperty")
}

fun ConeKotlinType.functionClassKind(session: FirSession): FunctionClassKind? {
    return classId(session)?.toFunctionClassKind()
}

private fun ClassId.toFunctionClassKind(): FunctionClassKind? {
    return FunctionClassKind.byClassNamePrefix(packageFqName, relativeClassName.asString())
}

// Function, SuspendFunction, KFunction, KSuspendFunction
fun ConeKotlinType.isBuiltinFunctionalType(session: FirSession): Boolean {
    return functionClassKind(session) != null
}

// Function, SuspendFunction, KFunction, KSuspendFunction
fun ConeClassLikeLookupTag.isBuiltinFunctionalType(): Boolean {
    return classId.toFunctionClassKind() != null
}

inline fun ConeKotlinType.isFunctionalType(session: FirSession, predicate: (FunctionClassKind) -> Boolean): Boolean {
    val kind = functionClassKind(session) ?: return false
    return predicate(kind)
}

// Function
fun ConeKotlinType.isFunctionalType(session: FirSession): Boolean {
    return isFunctionalType(session) { it == FunctionClassKind.Function }
}

// SuspendFunction, KSuspendFunction
fun ConeKotlinType.isSuspendFunctionType(session: FirSession): Boolean {
    return isFunctionalType(session) { it.isSuspendType }
}

// KFunction, KSuspendFunction
fun ConeKotlinType.isKFunctionType(session: FirSession): Boolean {
    return isFunctionalType(session) { it.isReflectType }
}

fun ConeKotlinType.kFunctionTypeToFunctionType(session: FirSession): ConeClassLikeType {
    require(this.isKFunctionType(session))
    val kind =
        if (isSuspendFunctionType(session)) FunctionClassKind.SuspendFunction
        else FunctionClassKind.Function
    val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size - 1))
    return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(functionalTypeId), typeArguments, isNullable = false)
}

fun ConeKotlinType.suspendFunctionTypeToFunctionType(session: FirSession): ConeClassLikeType {
    require(this.isSuspendFunctionType(session))
    val kind =
        if (isKFunctionType(session)) FunctionClassKind.KFunction
        else FunctionClassKind.Function
    val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size - 1))
    return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(functionalTypeId), typeArguments, isNullable = false, attributes = attributes)
}

fun ConeKotlinType.suspendFunctionTypeToFunctionTypeWithContinuation(session: FirSession, continuationClassId: ClassId): ConeClassLikeType {
    require(this.isSuspendFunctionType(session))
    val kind =
        if (isKFunctionType(session)) FunctionClassKind.KFunction
        else FunctionClassKind.Function
    val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size))
    val fullyExpandedType = type.fullyExpandedType(session)
    val typeArguments = fullyExpandedType.typeArguments
    val lastTypeArgument = typeArguments.last()
    return ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(functionalTypeId),
        typeArguments = (typeArguments.dropLast(1) + ConeClassLikeLookupTagImpl(continuationClassId).constructClassType(
            arrayOf(lastTypeArgument),
            isNullable = false
        ) + lastTypeArgument).toTypedArray(),
        isNullable = false,
        attributes = attributes
    )
}

fun ConeKotlinType.isSubtypeOfFunctionalType(session: FirSession, expectedFunctionalType: ConeClassLikeType): Boolean {
    require(expectedFunctionalType.isBuiltinFunctionalType(session))
    return AbstractTypeChecker.isSubtypeOf(session.typeContext, this, expectedFunctionalType.replaceArgumentsWithStarProjections())
}

fun ConeKotlinType.findSubtypeOfNonSuspendFunctionalType(session: FirSession, expectedFunctionalType: ConeClassLikeType): ConeKotlinType? {
    require(expectedFunctionalType.isBuiltinFunctionalType(session) && !expectedFunctionalType.isSuspendFunctionType(session))
    return when (this) {
        is ConeClassLikeType -> {
            // Expect the argument type is not a suspend functional type.
            if (isSuspendFunctionType(session) || !isSubtypeOfFunctionalType(session, expectedFunctionalType))
                null
            else
                this
        }
        is ConeIntersectionType -> {
            if (intersectedTypes.any { it.isSuspendFunctionType(session) })
                null
            else
                intersectedTypes.find { it.findSubtypeOfNonSuspendFunctionalType(session, expectedFunctionalType) != null }
        }
        is ConeTypeParameterType -> {
            val bounds = lookupTag.typeParameterSymbol.resolvedBounds.map { it.coneType }
            if (bounds.any { it.isSuspendFunctionType(session) })
                null
            else
                bounds.find { it.findSubtypeOfNonSuspendFunctionalType(session, expectedFunctionalType) != null }
        }
        else -> null
    }
}

fun ConeClassLikeType.findBaseInvokeSymbol(session: FirSession, scopeSession: ScopeSession): FirNamedFunctionSymbol? {
    require(this.isBuiltinFunctionalType(session))
    val functionN = (lookupTag.toSymbol(session)?.fir as? FirClass) ?: return null
    var baseInvokeSymbol: FirNamedFunctionSymbol? = null
    functionN.unsubstitutedScope(
        session,
        scopeSession,
        withForcedTypeCalculator = false
    ).processFunctionsByName(OperatorNameConventions.INVOKE) { functionSymbol ->
        baseInvokeSymbol = functionSymbol
        return@processFunctionsByName
    }
    return baseInvokeSymbol
}

fun ConeKotlinType.findContributedInvokeSymbol(
    session: FirSession,
    scopeSession: ScopeSession,
    expectedFunctionalType: ConeClassLikeType,
    shouldCalculateReturnTypesOfFakeOverrides: Boolean
): FirFunctionSymbol<*>? {
    val baseInvokeSymbol = expectedFunctionalType.findBaseInvokeSymbol(session, scopeSession) ?: return null

    val fakeOverrideTypeCalculator = if (shouldCalculateReturnTypesOfFakeOverrides) {
        FakeOverrideTypeCalculator.Forced
    } else {
        FakeOverrideTypeCalculator.DoNothing
    }
    val scope = scope(session, scopeSession, fakeOverrideTypeCalculator) ?: return null
    var declaredInvoke: FirNamedFunctionSymbol? = null
    scope.processFunctionsByName(OperatorNameConventions.INVOKE) { functionSymbol ->
        if (functionSymbol.fir.valueParameters.size == baseInvokeSymbol.fir.valueParameters.size) {
            declaredInvoke = functionSymbol
            return@processFunctionsByName
        }
    }

    var overriddenInvoke: FirFunctionSymbol<*>? = null
    if (declaredInvoke != null) {
        // Make sure the user-contributed or type-substituted invoke we just found above is an override of base invoke.
        scope.processOverriddenFunctions(declaredInvoke!!) { functionSymbol ->
            if (functionSymbol == baseInvokeSymbol || functionSymbol.originalForSubstitutionOverride == baseInvokeSymbol) {
                overriddenInvoke = functionSymbol
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }
    }

    return if (overriddenInvoke != null) declaredInvoke else null
}

fun ConeKotlinType.isKClassType(): Boolean {
    return classId == StandardClassIds.KClass
}

private fun ConeTypeProjection.typeOrDefault(default: ConeKotlinType): ConeKotlinType =
    when (this) {
        is ConeKotlinTypeProjection -> type
        is ConeStarProjection -> default
    }

fun ConeKotlinType.receiverType(session: FirSession): ConeKotlinType? {
    if (!isBuiltinFunctionalType(session) || !isExtensionFunctionType(session)) return null
    return fullyExpandedType(session).typeArguments.first().typeOrDefault(session.builtinTypes.nothingType.type)
}

fun ConeKotlinType.returnType(session: FirSession): ConeKotlinType {
    require(this is ConeClassLikeType)
    return fullyExpandedType(session).typeArguments.last().typeOrDefault(session.builtinTypes.nullableAnyType.type)
}

fun ConeKotlinType.valueParameterTypesIncludingReceiver(session: FirSession): List<ConeKotlinType> {
    require(this is ConeClassLikeType)
    return fullyExpandedType(session).typeArguments.dropLast(1).map { it.typeOrDefault(session.builtinTypes.nothingType.type) }
}

val FirAnonymousFunction.returnType: ConeKotlinType? get() = returnTypeRef.coneTypeSafe()
val FirAnonymousFunction.receiverType: ConeKotlinType? get() = receiverTypeRef?.coneTypeSafe()

fun ConeTypeContext.isTypeMismatchDueToNullability(
    actualType: ConeKotlinType,
    expectedType: ConeKotlinType
): Boolean {
    return actualType.isNullableType() && !expectedType.isNullableType() && AbstractTypeChecker.isSubtypeOf(
        this,
        actualType,
        expectedType.withNullability(ConeNullability.NULLABLE, this)
    )
}
