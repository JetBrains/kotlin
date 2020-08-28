/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.createFunctionalType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun Candidate.preprocessLambdaArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirAnonymousFunction,
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef?,
    forceResolution: Boolean = false,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType? = null
): PostponedResolvedAtom {
    if (expectedType != null && expectedTypeRef != null && !forceResolution && csBuilder.isTypeVariable(expectedType)) {
        return LambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType, expectedTypeRef, this)
    }

    val resolvedArgument =
        extractLambdaInfoFromFunctionalType(expectedType, expectedTypeRef, argument, returnTypeVariable, bodyResolveComponents, this)
            ?: extraLambdaInfo(expectedType, argument, csBuilder, bodyResolveComponents.session, this)

    if (expectedType != null) {
        // TODO: add SAM conversion processing
        val lambdaType = createFunctionalType(
            resolvedArgument.parameters,
            resolvedArgument.receiver,
            resolvedArgument.returnType,
            isSuspend = resolvedArgument.isSuspend
        )
        csBuilder.addSubtypeConstraint(lambdaType, expectedType, SimpleConstraintSystemConstraintPosition)
    }

    return resolvedArgument
}

fun Candidate.preprocessCallableReference(
    argument: FirCallableReferenceAccess,
    expectedType: ConeKotlinType?
) {
    val lhs = bodyResolveComponents.doubleColonExpressionResolver.resolveDoubleColonLHS(argument)
    postponedAtoms += ResolvedCallableReferenceAtom(argument, expectedType, lhs, bodyResolveComponents.session)
}

private fun extraLambdaInfo(
    expectedType: ConeKotlinType?,
    argument: FirAnonymousFunction,
    csBuilder: ConstraintSystemBuilder,
    session: FirSession,
    candidate: Candidate?
): ResolvedLambdaAtom {
    val isSuspend = expectedType?.isSuspendFunctionType(session) ?: false

    val isFunctionSupertype =
        expectedType != null && expectedType.lowerBoundIfFlexible()
            .isBuiltinFunctionalType(session)//isNotNullOrNullableFunctionSupertype(expectedType)

    val typeVariable = ConeTypeVariableForLambdaReturnType(argument, "_L")

    val receiverType = argument.receiverType
    val returnType =
        argument.returnType
            ?: expectedType?.typeArguments?.singleOrNull()?.safeAs<ConeKotlinTypeProjection>()?.type?.takeIf { isFunctionSupertype }
            ?: typeVariable.defaultType

    val nothingType = argument.session.builtinTypes.nothingType.type
    val parameters = argument.valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: nothingType
    }

    val newTypeVariableUsed = returnType == typeVariable.defaultType
    if (newTypeVariableUsed) csBuilder.registerVariable(typeVariable)

    return ResolvedLambdaAtom(
        argument,
        expectedType,
        isSuspend,
        receiverType,
        parameters,
        returnType,
        typeVariable.takeIf { newTypeVariableUsed },
        candidate
    )
}
