/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionalTypeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
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
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

// ---------------------------------------------- is type is a functional type ----------------------------------------------

fun ConeKotlinType.functionalTypeKind(session: FirSession): FunctionalTypeKind? {
    if (this !is ConeClassLikeType) return null
    return fullyExpandedType(session).lookupTag.functionalTypeKind(session)
}

private fun ConeClassLikeLookupTag.functionalTypeKind(session: FirSession): FunctionalTypeKind? {
    val classId = classId
    return session.functionalTypeService.getKindByClassNamePrefix(classId.packageFqName, classId.shortClassName.asString())
}

private inline fun ConeKotlinType.isFunctionalTypeWithPredicate(
    session: FirSession,
    errorOnNotFunctionalType: Boolean = false,
    predicate: (FunctionalTypeKind) -> Boolean
): Boolean {
    val kind = functionalTypeKind(session)
        ?: if (errorOnNotFunctionalType) error("$this is not a functional type") else return false
    return predicate(kind)
}

// Function
fun ConeKotlinType.isSimpleFunctionType(session: FirSession): Boolean {
    return isFunctionalTypeWithPredicate(session) { it == FunctionalTypeKind.Function }
}

// Function, SuspendFunction, [Custom]Function
fun ConeKotlinType.isNonReflectFunctionalType(session: FirSession): Boolean {
    return isFunctionalTypeWithPredicate(session) { !it.isReflectType }
}

// SuspendFunction, KSuspendFunction
fun ConeKotlinType.isSuspendFunctionType(session: FirSession): Boolean {
    return isFunctionalTypeWithPredicate(session) {
        it == FunctionalTypeKind.SuspendFunction || it == FunctionalTypeKind.KSuspendFunction
    }
}

// KFunction, KSuspendFunction, K[Custom]Function
fun ConeKotlinType.isReflectFunctionalType(session: FirSession): Boolean {
    return isFunctionalTypeWithPredicate(session) { it.isReflectType }
}

// Function, SuspendFunction, [Custom]Function, KFunction, KSuspendFunction, K[Custom]Function
fun ConeKotlinType.isSomeFunctionalType(session: FirSession): Boolean {
    return functionalTypeKind(session) != null
}

// Function, SuspendFunction, [Custom]Function, KFunction, KSuspendFunction, K[Custom]Function
fun ConeClassLikeLookupTag.isSomeFunctionalType(session: FirSession): Boolean {
    return functionalTypeKind(session) != null
}

// Function, KFunction
private fun ConeKotlinType.isSimpleFunctionalType(session: FirSession, errorOnNotFunctionalType: Boolean): Boolean {
    return isFunctionalTypeWithPredicate(session, errorOnNotFunctionalType) {
        it == FunctionalTypeKind.Function || it == FunctionalTypeKind.KFunction
    }
}

// SuspendFunction, [Custom]Function, KSuspendFunction, K[Custom]Function
private fun ConeKotlinType.isNotSimpleFunctionalType(session: FirSession): Boolean {
    return !isSimpleFunctionalType(session, errorOnNotFunctionalType = false)
}

// ---------------------------------------------- functional type conversions ----------------------------------------------

/*
 * SuspendFunction/[Custom]Function -> Function
 * KSuspendFunction/K[Custom]Function -> KFunction
 */
fun ConeKotlinType.customFunctionalTypeToSimpleFunctionalType(session: FirSession): ConeClassLikeType {
    val kind = functionalTypeKind(session)
    require(kind != null && kind != FunctionalTypeKind.Function && kind != FunctionalTypeKind.KFunction)
    val newKind = if (kind.isReflectType) {
        FunctionalTypeKind.KFunction
    } else {
        FunctionalTypeKind.Function
    }
    return createFunctionalTypeWithNewKind(newKind)
}

/*
 * KFunction -> Function
 * KSuspendFunction -> SuspendFunction
 * K[Custom]Function -> [Custom]Function
 */
fun ConeKotlinType.reflectFunctionalTypeToNonReflectFunctionalType(session: FirSession): ConeClassLikeType {
    val kind = functionalTypeKind(session)
    require(kind != null && kind.isReflectType)
    return createFunctionalTypeWithNewKind(kind.nonReflectKind())
}

private fun ConeKotlinType.createFunctionalTypeWithNewKind(kind: FunctionalTypeKind): ConeClassLikeType {
    val functionalTypeId = ClassId(kind.packageFqName, kind.numberedClassName(typeArguments.size - 1))
    return functionalTypeId.toLookupTag().constructClassType(typeArguments, isNullable = false, attributes = attributes)
}

// ---------------------------------------------- functional type subtyping ----------------------------------------------

// expectedFunctionalType is kotlin.FunctionN or kotlin.reflect.KFunctionN
fun ConeKotlinType.findSubtypeOfSimpleFunctionalType(session: FirSession, expectedFunctionalType: ConeClassLikeType): ConeKotlinType? {
    require(expectedFunctionalType.isSimpleFunctionalType(session, errorOnNotFunctionalType = true))
    return findSubtypeOfSimpleFunctionalTypeImpl(session, expectedFunctionalType)
}

private fun ConeKotlinType.findSubtypeOfSimpleFunctionalTypeImpl(
    session: FirSession,
    expectedFunctionalType: ConeClassLikeType
): ConeKotlinType? {
    return when (this) {
        is ConeClassLikeType -> {
            // Expect the argument type is a simple functional type.
            when {
                isNotSimpleFunctionalType(session) -> null
                isSubtypeOfFunctionalType(session, expectedFunctionalType) -> this
                else -> null
            }
        }

        is ConeIntersectionType -> {
            runUnless(intersectedTypes.any { it.isNotSimpleFunctionalType(session) }) {
                intersectedTypes.find { it.findSubtypeOfSimpleFunctionalTypeImpl(session, expectedFunctionalType) != null }
            }
        }

        is ConeTypeParameterType -> {
            val bounds = lookupTag.typeParameterSymbol.resolvedBounds.map { it.coneType }
            runUnless(bounds.any { it.isNotSimpleFunctionalType(session) }) {
                bounds.find { it.findSubtypeOfSimpleFunctionalTypeImpl(session, expectedFunctionalType) != null }
            }
        }
        else -> null
    }
}

private fun ConeKotlinType.isSubtypeOfFunctionalType(session: FirSession, expectedFunctionalType: ConeClassLikeType): Boolean {
    return AbstractTypeChecker.isSubtypeOf(session.typeContext, this, expectedFunctionalType.replaceArgumentsWithStarProjections())
}

// ---------------------------------------------- functional type scope utils ----------------------------------------------

fun ConeClassLikeType.findBaseInvokeSymbol(session: FirSession, scopeSession: ScopeSession): FirNamedFunctionSymbol? {
    require(this.isSomeFunctionalType(session))
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
    val scope = scope(session, scopeSession, fakeOverrideTypeCalculator, requiredPhase = FirResolvePhase.STATUS) ?: return null
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

// ---------------------------------------------- functional type type argument extraction ----------------------------------------------

fun ConeKotlinType.receiverType(session: FirSession): ConeKotlinType? {
    if (!isSomeFunctionalType(session) || !isExtensionFunctionType(session)) return null
    return fullyExpandedType(session).let { expanded ->
        expanded.typeArguments[expanded.contextReceiversNumberForFunctionType].typeOrDefault(session.builtinTypes.nothingType.type)
    }
}

fun ConeKotlinType.returnType(session: FirSession): ConeKotlinType {
    require(this is ConeClassLikeType)
    // TODO: add requirement
    return fullyExpandedType(session).typeArguments.last().typeOrDefault(session.builtinTypes.nullableAnyType.type)
}

fun ConeKotlinType.valueParameterTypesIncludingReceiver(session: FirSession): List<ConeKotlinType> {
    require(this is ConeClassLikeType)
    // TODO: add requirement
    return fullyExpandedType(session).typeArguments.dropLast(1).map { it.typeOrDefault(session.builtinTypes.nothingType.type) }
}

private fun ConeTypeProjection.typeOrDefault(default: ConeKotlinType): ConeKotlinType = when (this) {
    is ConeKotlinTypeProjection -> type
    is ConeStarProjection -> default
}
