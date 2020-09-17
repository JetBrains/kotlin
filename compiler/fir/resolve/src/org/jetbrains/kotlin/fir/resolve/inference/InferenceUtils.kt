/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
private fun ConeKotlinType.functionClassKind(session: FirSession): FunctionClassKind? {
    contract {
        returns(true) implies (this@functionClassKind is ConeClassLikeType)
    }
    if (this !is ConeClassLikeType) return null
    val classId = fullyExpandedType(session).lookupTag.classId
    return FunctionClassKind.byClassNamePrefix(classId.packageFqName, classId.relativeClassName.asString())
}

fun ConeKotlinType.isBuiltinFunctionalType(session: FirSession): Boolean {
    val kind = functionClassKind(session) ?: return false
    return kind == FunctionClassKind.Function ||
            kind == FunctionClassKind.KFunction ||
            kind == FunctionClassKind.SuspendFunction ||
            kind == FunctionClassKind.KSuspendFunction
}

fun ConeKotlinType.isSuspendFunctionType(session: FirSession): Boolean {
    val kind = functionClassKind(session) ?: return false
    return kind == FunctionClassKind.SuspendFunction ||
            kind == FunctionClassKind.KSuspendFunction
}

fun ConeKotlinType.isKFunctionType(session: FirSession): Boolean {
    val kind = functionClassKind(session) ?: return false
    return kind == FunctionClassKind.KFunction ||
            kind == FunctionClassKind.KSuspendFunction
}

fun ConeKotlinType.receiverType(expectedTypeRef: FirTypeRef?, session: FirSession): ConeKotlinType? {
    if (isBuiltinFunctionalType(session) && isExtensionFunctionType(session)) {
        return (this.fullyExpandedType(session).typeArguments.first() as ConeKotlinTypeProjection).type
    }
    return null
}

fun ConeKotlinType.receiverType(session: FirSession): ConeKotlinType? {
    if (isBuiltinFunctionalType(session)) {
        return (this.fullyExpandedType(session).typeArguments.first() as ConeKotlinTypeProjection).type
    }
    return null
}

fun ConeKotlinType.returnType(session: FirSession): ConeKotlinType? {
    require(this is ConeClassLikeType)
    val projection = fullyExpandedType(session).typeArguments.last()
    return (projection as? ConeKotlinTypeProjection)?.type
}

fun ConeKotlinType.valueParameterTypesIncludingReceiver(session: FirSession): List<ConeKotlinType?> {
    require(this is ConeClassLikeType)
    return fullyExpandedType(session).typeArguments.dropLast(1).map {
        (it as? ConeKotlinTypeProjection)?.type
    }
}

val FirAnonymousFunction.returnType: ConeKotlinType? get() = returnTypeRef.coneTypeSafe()
val FirAnonymousFunction.receiverType: ConeKotlinType? get() = receiverTypeRef?.coneTypeSafe()

fun extractLambdaInfoFromFunctionalType(
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef?,
    argument: FirAnonymousFunction,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
    components: BodyResolveComponents,
    candidate: Candidate?
): ResolvedLambdaAtom? {
    val session = components.session
    if (expectedType == null) return null
    if (expectedType is ConeFlexibleType) {
        return extractLambdaInfoFromFunctionalType(
            expectedType.lowerBound,
            expectedTypeRef,
            argument,
            returnTypeVariable,
            components,
            candidate
        )
    }
    if (!expectedType.isBuiltinFunctionalType(session)) return null

    val receiverType = argument.receiverType ?: expectedType.receiverType(expectedTypeRef, session)
    val returnType = argument.returnType ?: expectedType.returnType(session) ?: return null
    val parameters = extractLambdaParameters(expectedType, argument, expectedType.isExtensionFunctionType(session), session)

    return ResolvedLambdaAtom(
        argument,
        expectedType,
        expectedType.isSuspendFunctionType(session),
        receiverType,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = returnTypeVariable,
        candidate
    )
}

private fun extractLambdaParameters(
    expectedType: ConeKotlinType,
    argument: FirAnonymousFunction,
    expectedTypeIsExtensionFunctionType: Boolean,
    session: FirSession
): List<ConeKotlinType> {
    val parameters = argument.valueParameters
    val expectedParameters = expectedType.extractParametersForFunctionalType(expectedTypeIsExtensionFunctionType, session)

    val nullableAnyType = argument.session.builtinTypes.nullableAnyType.type
    if (parameters.isEmpty()) {
        return expectedParameters.map { it?.type ?: nullableAnyType }
    }

    return parameters.mapIndexed { index, parameter ->
        parameter.returnTypeRef.coneTypeSafe() ?: expectedParameters.getOrNull(index) ?: nullableAnyType
    }
}

private fun ConeKotlinType.extractParametersForFunctionalType(
    isExtensionFunctionType: Boolean,
    session: FirSession
): List<ConeKotlinType?> {
    return valueParameterTypesIncludingReceiver(session).let {
        if (isExtensionFunctionType) {
            it.drop(1)
        } else {
            it
        }
    }
}
