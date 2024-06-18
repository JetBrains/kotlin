/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.createFunctionType
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.utils.addToStdlib.runIf

fun Candidate.preprocessLambdaArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirAnonymousFunctionExpression,
    expectedType: ConeKotlinType?,
    context: ResolutionContext,
    sink: CheckerSink?,
    duringCompletion: Boolean = false,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType? = null
): ConePostponedResolvedAtom {
    if (expectedType != null && !duringCompletion && csBuilder.isTypeVariable(expectedType)) {
        val expectedTypeVariableWithConstraints =
            csBuilder.currentStorage().notFixedTypeVariables[expectedType.typeConstructor(context.typeContext)]

        if (expectedTypeVariableWithConstraints != null) {
            val explicitTypeArgument = expectedTypeVariableWithConstraints.constraints.find {
                it.kind == ConstraintKind.EQUALITY && it.position.from is ConeExplicitTypeParameterConstraintPosition
            }?.type as ConeKotlinType?

            if (explicitTypeArgument == null || explicitTypeArgument.typeArguments.isNotEmpty()) {
                return ConeLambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType, this).also {
                    addPostponedAtom(it)
                }
            }
        }
    }

    val anonymousFunction = argument.anonymousFunction

    val resolvedArgument =
        extractLambdaInfoFromFunctionType(
            expectedType,
            argument,
            argument.anonymousFunction,
            returnTypeVariable,
            context.bodyResolveComponents,
            this,
            duringCompletion || sink == null,
            sourceForFunctionExpression = argument.source,
        ) ?: extractLambdaInfo(
            expectedType, argument, csBuilder, context.session, this,
            sourceForFunctionExpression = argument.source,
        )

    if (expectedType != null) {
        val parameters = resolvedArgument.parameters
        val functionTypeKind = context.session.functionTypeService.extractSingleSpecialKindForFunction(anonymousFunction.symbol)
            ?: resolvedArgument.expectedFunctionTypeKind?.nonReflectKind()
            ?: FunctionTypeKind.Function
        val lambdaType = createFunctionType(
            functionTypeKind,
            parameters,
            resolvedArgument.receiver,
            resolvedArgument.returnType,
            contextReceivers = resolvedArgument.contextReceivers,
        )

        val position = ConeArgumentConstraintPosition(resolvedArgument.fir)
        if (duringCompletion || sink == null) {
            csBuilder.addSubtypeConstraint(lambdaType, expectedType, position)
        } else {
            if (!csBuilder.addSubtypeConstraintIfCompatible(lambdaType, expectedType, position)) {
                sink.reportDiagnostic(
                    ArgumentTypeMismatch(
                        expectedType,
                        lambdaType,
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
    addPostponedAtom(ConeResolvedCallableReferenceAtom(argument, expectedType, lhs, context.session))
}

private fun extractLambdaInfo(
    expectedType: ConeKotlinType?,
    argument: FirAnonymousFunctionExpression,
    csBuilder: ConstraintSystemBuilder,
    session: FirSession,
    candidate: Candidate?,
    sourceForFunctionExpression: KtSourceElement?,
): ConeResolvedLambdaAtom {
    require(expectedType?.lowerBoundIfFlexible()?.functionTypeKind(session) == null) {
        "Currently, we only extract lambda info from its shape when expected type is not function, but $expectedType"
    }
    val lambda = argument.anonymousFunction
    val typeVariable = ConeTypeVariableForLambdaReturnType(lambda, "_L")

    val receiverType = lambda.receiverType
    val returnType =
        lambda.returnType
            ?: typeVariable.defaultType

    val defaultType = runIf(candidate?.symbol?.origin == FirDeclarationOrigin.DynamicScope) { ConeDynamicType.create(session) }

    val parameters = lambda.valueParameters.mapIndexed { i, it ->
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>()
            ?: defaultType
            ?: ConeTypeVariableForLambdaParameterType("_P$i").apply { csBuilder.registerVariable(this) }.defaultType
    }

    val contextReceivers = lambda.contextReceivers.mapIndexed { i, it ->
        it.typeRef.coneTypeSafe<ConeKotlinType>()
            ?: defaultType
            ?: ConeTypeVariableForLambdaParameterType("_C$i").apply { csBuilder.registerVariable(this) }.defaultType
    }

    val newTypeVariableUsed = returnType == typeVariable.defaultType
    if (newTypeVariableUsed) csBuilder.registerVariable(typeVariable)

    return ConeResolvedLambdaAtom(
        lambda,
        argument,
        expectedType,
        expectedFunctionTypeKind = lambda.typeRef.coneTypeSafe<ConeKotlinType>()?.lowerBoundIfFlexible()?.functionTypeKind(session),
        receiverType,
        contextReceivers,
        parameters,
        returnType,
        typeVariable.takeIf { newTypeVariableUsed },
        coerceFirstParameterToExtensionReceiver = false,
        sourceForFunctionExpression,
    ).also {
        candidate?.addPostponedAtom(it)
    }
}
