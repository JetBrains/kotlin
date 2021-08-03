/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.resolve.calls.ArgumentTypeMismatch
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.CheckerSink
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.createFunctionalType
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.isTypeMismatchDueToNullability
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun Candidate.preprocessLambdaArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirAnonymousFunctionExpression,
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef?,
    context: ResolutionContext,
    sink: CheckerSink?,
    duringCompletion: Boolean = false,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType? = null
): PostponedResolvedAtom {
    if (expectedType != null && expectedTypeRef != null && !duringCompletion && csBuilder.isTypeVariable(expectedType)) {
        return LambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType, expectedTypeRef, this)
    }

    val anonymousFunction = argument.anonymousFunction

    val resolvedArgument =
        extractLambdaInfoFromFunctionalType(
            expectedType,
            expectedTypeRef,
            anonymousFunction,
            returnTypeVariable,
            context.bodyResolveComponents,
            this
        ) ?: extraLambdaInfo(expectedType, anonymousFunction, csBuilder, context.session, this)

    if (expectedType != null) {
        // TODO: add SAM conversion processing
        val lambdaType = createFunctionalType(
            resolvedArgument.parameters,
            resolvedArgument.receiver,
            resolvedArgument.returnType,
            isSuspend = resolvedArgument.isSuspend
        )

        val position = ConeArgumentConstraintPosition(resolvedArgument.atom)
        if (duringCompletion || sink == null) {
            csBuilder.addSubtypeConstraint(lambdaType, expectedType, position)
        } else {
            if (!csBuilder.addSubtypeConstraintIfCompatible(lambdaType, expectedType, position)) {
                sink.reportDiagnostic(
                    ArgumentTypeMismatch(
                        lambdaType,
                        expectedType,
                        argument,
                        context.session.typeContext.isTypeMismatchDueToNullability(lambdaType, expectedType)
                    )
                )
            }
        }
    }

    return resolvedArgument
}

fun Candidate.preprocessCallableReference(
    argument: FirCallableReferenceAccess,
    expectedType: ConeKotlinType?,
    context: ResolutionContext
) {
    val lhs = context.bodyResolveComponents.doubleColonExpressionResolver.resolveDoubleColonLHS(argument)
    postponedAtoms += ResolvedCallableReferenceAtom(argument, expectedType, lhs, context.session)
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

    val nothingType = session.builtinTypes.nothingType.type
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
