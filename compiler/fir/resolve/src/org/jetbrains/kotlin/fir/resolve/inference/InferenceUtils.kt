/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferValueParameterType
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolvedLambdaAtom
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.removeParameterNameAnnotation
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * @return null if and only if expectedType is not function type (or flexible type with function type as bound)
 */
fun extractLambdaInfoFromFunctionType(
    expectedType: ConeKotlinType?,
    argument: FirAnonymousFunctionExpression,
    lambda: FirAnonymousFunction,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
    components: BodyResolveComponents,
    candidate: Candidate?,
    allowCoercionToExtensionReceiver: Boolean,
    sourceForFunctionExpression: KtSourceElement?,
): ConeResolvedLambdaAtom? {
    val session = components.session
    if (expectedType == null) return null
    if (expectedType is ConeFlexibleType) {
        return extractLambdaInfoFromFunctionType(
            expectedType.lowerBound,
            argument,
            lambda,
            returnTypeVariable,
            components,
            candidate,
            allowCoercionToExtensionReceiver,
            sourceForFunctionExpression,
        )
    }
    val expectedFunctionKind = expectedType.functionTypeKind(session) ?: return null

    val actualFunctionKind = session.functionTypeService.extractSingleSpecialKindForFunction(lambda.symbol)
        ?: runIf(!lambda.isLambda) {
            // There is no function -> suspend function conversions for non-lambda anonymous functions
            // If function is suspend then functionTypeService will return SuspendFunction kind
            FunctionTypeKind.Function
        }

    val returnType = lambda.returnType ?: expectedType.returnType(session)

    // `fun (x: T) = ...` and `fun T.() = ...` are both instances of `T.() -> V` and `(T) -> V`; `fun () = ...` is not.
    // For lambdas, the existence of the receiver is always implied by the expected type, and a value parameter
    // can never fill its role.
    val receiverType = if (lambda.isLambda) expectedType.receiverType(session) else lambda.receiverType
    val contextReceiversNumber =
        if (lambda.isLambda) expectedType.contextReceiversNumberForFunctionType else lambda.contextReceivers.size

    val valueParametersTypesIncludingReceiver = expectedType.valueParameterTypesIncludingReceiver(session)
    val isExtensionFunctionType = expectedType.isExtensionFunctionType(session)
    val expectedParameters = valueParametersTypesIncludingReceiver.let {
        val forExtension = if (receiverType != null && isExtensionFunctionType) 1 else 0
        val toDrop = forExtension + contextReceiversNumber

        if (toDrop > 0) it.drop(toDrop) else it
    }.map {
        // @ParameterName is assumed to be used for Ctrl+P on the call site of a property with a function type.
        // Propagating it further may affect further inference might work weirdly, and for sure,
        // it's not expected to leak in implicitly typed declarations.
        it.removeParameterNameAnnotation(session)
    }

    var coerceFirstParameterToExtensionReceiver = false
    val argumentValueParameters = lambda.valueParameters
    val parameters = if (lambda.isLambda && !lambda.hasExplicitParameterList && expectedParameters.size < 2) {
        expectedParameters // Infer existence of a parameter named `it` of an appropriate type.
    } else {
        if (allowCoercionToExtensionReceiver &&
            lambda.isLambda &&
            isExtensionFunctionType &&
            valueParametersTypesIncludingReceiver.size == argumentValueParameters.size
        ) {
            // (T, ...) -> V can be converter to T.(...) -> V
            val firstValueParameter = argumentValueParameters.firstOrNull()
            val extensionParameter = valueParametersTypesIncludingReceiver.firstOrNull()
            if (firstValueParameter?.returnTypeRef?.coneTypeSafe<ConeKotlinType>() == extensionParameter?.type) {
                coerceFirstParameterToExtensionReceiver = true
            }
        }

        if (coerceFirstParameterToExtensionReceiver) {
            argumentValueParameters.drop(1)
        } else {
            argumentValueParameters
        }.mapIndexed { index, parameter ->
            parameter.returnTypeRef.coneTypeSafe()
                ?: expectedParameters.getOrNull(index)
                ?: ConeErrorType(ConeCannotInferValueParameterType(parameter.symbol))
        }
    }

    val contextReceivers =
        when {
            contextReceiversNumber == 0 -> emptyList()
            lambda.isLambda -> valueParametersTypesIncludingReceiver.subList(0, contextReceiversNumber)
            else -> lambda.contextReceivers.map { it.typeRef.coneType }
        }

    return ConeResolvedLambdaAtom(
        argument,
        expectedType,
        actualFunctionKind ?: expectedFunctionKind,
        receiverType,
        contextReceivers,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = returnTypeVariable,
        coerceFirstParameterToExtensionReceiver,
        sourceForFunctionExpression,
    )
}
