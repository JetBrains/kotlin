/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isBasicFunctionOrKFunction
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
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
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

// ---------------------------------------------- is type is a function type ----------------------------------------------

fun ConeKotlinType.functionTypeKind(session: FirSession): FunctionTypeKind? {
    if (this !is ConeClassLikeType) return null
    return fullyExpandedType(session).lookupTag.functionTypeKind(session)
}

private fun ConeClassLikeLookupTag.functionTypeKind(session: FirSession): FunctionTypeKind? {
    val classId = classId
    return session.functionTypeService.getKindByClassNamePrefix(classId.packageFqName, classId.shortClassName.asString())
}

private inline fun ConeKotlinType.isFunctionTypeWithPredicate(
    session: FirSession,
    errorOnNotFunctionType: Boolean = false,
    predicate: (FunctionTypeKind) -> Boolean
): Boolean {
    val kind = functionTypeKind(session)
        ?: if (errorOnNotFunctionType) errorWithAttachment("${this::class.java} is not a function type") {
            withConeTypeEntry("type", this@isFunctionTypeWithPredicate)
        } else return false
    return predicate(kind)
}

// Function
fun ConeKotlinType.isBasicFunctionType(session: FirSession): Boolean {
    return isFunctionTypeWithPredicate(session) { it == FunctionTypeKind.Function }
}

// SuspendFunction, KSuspendFunction
fun ConeKotlinType.isSuspendOrKSuspendFunctionType(session: FirSession): Boolean {
    return isFunctionTypeWithPredicate(session) {
        it == FunctionTypeKind.SuspendFunction || it == FunctionTypeKind.KSuspendFunction
    }
}

// KFunction, KSuspendFunction, K[Custom]Function
fun ConeKotlinType.isReflectFunctionType(session: FirSession): Boolean {
    return isFunctionTypeWithPredicate(session) { it.isReflectType }
}

// Function, SuspendFunction, [Custom]Function
fun ConeKotlinType.isNonReflectFunctionType(session: FirSession): Boolean {
    return isFunctionTypeWithPredicate(session) { !it.isReflectType }
}

// Function, SuspendFunction, [Custom]Function, KFunction, KSuspendFunction, K[Custom]Function
fun ConeKotlinType.isSomeFunctionType(session: FirSession): Boolean {
    return functionTypeKind(session) != null
}

// Function, SuspendFunction, [Custom]Function, KFunction, KSuspendFunction, K[Custom]Function
fun ConeClassLikeLookupTag.isSomeFunctionType(session: FirSession): Boolean {
    return functionTypeKind(session) != null
}

// SuspendFunction, [Custom]Function, KSuspendFunction, K[Custom]Function
fun ConeKotlinType.isNotBasicFunctionType(session: FirSession): Boolean {
    return isFunctionTypeWithPredicate(session, errorOnNotFunctionType = false) { !it.isBasicFunctionOrKFunction }
}

// ---------------------------------------------- function type conversions ----------------------------------------------

/*
 * SuspendFunction/[Custom]Function -> Function
 * KSuspendFunction/K[Custom]Function -> KFunction
 */
fun ConeKotlinType.customFunctionTypeToSimpleFunctionType(session: FirSession): ConeClassLikeType {
    val kind = functionTypeKind(session)
    require(kind != null && kind != FunctionTypeKind.Function && kind != FunctionTypeKind.KFunction)
    val newKind = if (kind.isReflectType) {
        FunctionTypeKind.KFunction
    } else {
        FunctionTypeKind.Function
    }
    return createFunctionTypeWithNewKind(session, newKind)
}

private fun ConeKotlinType.createFunctionTypeWithNewKind(session: FirSession, kind: FunctionTypeKind): ConeClassLikeType {
    val expandedType = fullyExpandedType(session)
    val functionTypeId = ClassId(kind.packageFqName, kind.numberedClassName(expandedType.typeArguments.size - 1))
    return functionTypeId.toLookupTag().constructClassType(
        expandedType.typeArguments,
        isNullable = false,
        attributes = expandedType.attributes
    )
}

// ---------------------------------------------- function type subtyping ----------------------------------------------

// expectedfunctionType is kotlin.FunctionN or kotlin.reflect.KFunctionN
fun ConeKotlinType.findSubtypeOfBasicFunctionType(session: FirSession, expectedFunctionType: ConeClassLikeType): ConeKotlinType? {
    require(expectedFunctionType.isFunctionOrKFunctionType(session, errorOnNotFunctionType = true))
    return findSubtypeOfBasicFunctionTypeImpl(session, expectedFunctionType)
}

// Function, KFunction
private fun ConeKotlinType.isFunctionOrKFunctionType(session: FirSession, errorOnNotFunctionType: Boolean): Boolean {
    return isFunctionTypeWithPredicate(session, errorOnNotFunctionType) { it.isBasicFunctionOrKFunction }
}

private fun ConeKotlinType.findSubtypeOfBasicFunctionTypeImpl(
    session: FirSession,
    expectedFunctionType: ConeClassLikeType
): ConeKotlinType? {
    return when (this) {
        is ConeClassLikeType -> {
            when {
                // Expect the argument type is a simple function type.
                isNotBasicFunctionType(session) -> null
                isSubtypeOfFunctionType(session, expectedFunctionType) -> this
                else -> null
            }
        }

        is ConeIntersectionType -> {
            runUnless(intersectedTypes.any { it.isNotBasicFunctionType(session) }) {
                intersectedTypes.find { it.findSubtypeOfBasicFunctionTypeImpl(session, expectedFunctionType) != null }
            }
        }

        is ConeTypeParameterType -> {
            val bounds = lookupTag.typeParameterSymbol.resolvedBounds.map { it.coneType }
            runUnless(bounds.any { it.isNotBasicFunctionType(session) }) {
                bounds.find { it.findSubtypeOfBasicFunctionTypeImpl(session, expectedFunctionType) != null }
            }
        }
        else -> null
    }
}

private fun ConeKotlinType.isSubtypeOfFunctionType(session: FirSession, expectedFunctionType: ConeClassLikeType): Boolean {
    return AbstractTypeChecker.isSubtypeOf(session.typeContext, this, expectedFunctionType.replaceArgumentsWithStarProjections())
}

// ---------------------------------------------- function type scope utils ----------------------------------------------

fun ConeClassLikeType.findBaseInvokeSymbol(session: FirSession, scopeSession: ScopeSession): FirNamedFunctionSymbol? {
    require(this.isSomeFunctionType(session))
    val functionN = (lookupTag.toSymbol(session)?.fir as? FirClass) ?: return null
    var baseInvokeSymbol: FirNamedFunctionSymbol? = null
    functionN.unsubstitutedScope(
        session,
        scopeSession,
        withForcedTypeCalculator = false,
        memberRequiredPhase = null,
    ).processFunctionsByName(OperatorNameConventions.INVOKE) { functionSymbol ->
        baseInvokeSymbol = functionSymbol
        return@processFunctionsByName
    }
    return baseInvokeSymbol
}

fun ConeKotlinType.findContributedInvokeSymbol(
    session: FirSession,
    scopeSession: ScopeSession,
    expectedFunctionType: ConeClassLikeType,
    shouldCalculateReturnTypesOfFakeOverrides: Boolean
): FirFunctionSymbol<*>? {
    val baseInvokeSymbol = expectedFunctionType.findBaseInvokeSymbol(session, scopeSession) ?: return null

    val fakeOverrideTypeCalculator = if (shouldCalculateReturnTypesOfFakeOverrides) {
        FakeOverrideTypeCalculator.Forced
    } else {
        FakeOverrideTypeCalculator.DoNothing
    }

    val scope = scope(
        useSiteSession = session,
        scopeSession = scopeSession,
        fakeOverrideTypeCalculator = fakeOverrideTypeCalculator,
        requiredMembersPhase = FirResolvePhase.STATUS,
    ) ?: return null

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

// ---------------------------------------------- function type type argument extraction ----------------------------------------------

fun ConeKotlinType.contextReceiversTypes(session: FirSession): List<ConeKotlinType> {
    if (!isSomeFunctionType(session)) return emptyList()
    return fullyExpandedType(session).let { expanded ->
        val contextReceivers = expanded.typeArguments.take(expanded.contextReceiversNumberForFunctionType)
        contextReceivers.map { it.typeOrDefault(session.builtinTypes.nothingType.type) }
    }
}

fun ConeKotlinType.receiverType(session: FirSession): ConeKotlinType? {
    if (!isSomeFunctionType(session) || !isExtensionFunctionType(session)) return null
    return fullyExpandedType(session).let { expanded ->
        expanded.typeArguments[expanded.contextReceiversNumberForFunctionType].typeOrDefault(session.builtinTypes.nothingType.type)
    }
}

fun ConeKotlinType.returnType(session: FirSession): ConeKotlinType {
    require(this is ConeClassLikeType)
    // TODO: add requirement
    return fullyExpandedType(session).typeArguments.last().typeOrDefault(session.builtinTypes.nullableAnyType.type)
}

fun ConeKotlinType.valueParameterTypesWithoutReceivers(session: FirSession): List<ConeKotlinType> {
    require(this is ConeClassLikeType)
    // TODO: add requirement
    val expandedType = fullyExpandedType(session)

    val receiversNumber = expandedType.contextReceiversNumberForFunctionType + if (expandedType.isExtensionFunctionType) 1 else 0
    val valueParameters = expandedType.typeArguments.drop(receiversNumber).dropLast(1)

    return valueParameters.map { it.typeOrDefault(session.builtinTypes.nothingType.type) }
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

// ----------------- TODO fir utils

fun FirFunction.specialFunctionTypeKind(session: FirSession): FunctionTypeKind? {
    return (symbol as? FirNamedFunctionSymbol)?.let {
        session.functionTypeService.extractSingleSpecialKindForFunction(it)
    }
}
