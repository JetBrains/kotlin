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
    allowCoercionToExtensionReceiver: Boolean,
    sourceForFunctionExpression: KtSourceElement?,
): ConeResolvedLambdaAtom? {
    val session = components.session
    val expectedClassLikeType = expectedType?.lowerBoundIfFlexible() as? ConeClassLikeType ?: return null
    val expectedFunctionKind = expectedClassLikeType.functionTypeKind(session) ?: return null

    val actualFunctionKind = session.functionTypeService.extractSingleSpecialKindForFunction(lambda.symbol)
        ?: runIf(!lambda.isLambda) {
            // There is no function -> suspend function conversions for non-lambda anonymous functions
            // If function is suspend then functionTypeService will return SuspendFunction kind
            FunctionTypeKind.Function
        }

    val returnType = lambda.returnType ?: expectedClassLikeType.returnType(session)

    // `fun (x: T) = ...` and `fun T.() = ...` are both instances of `T.() -> V` and `(T) -> V`; `fun () = ...` is not.
    // For lambdas, the existence of the receiver is always implied by the expected type, and a value parameter
    // can never fill its role.
    val receiverType = if (lambda.isLambda) expectedClassLikeType.receiverType(session) else lambda.receiverType
    val contextParameterNumber =
        if (lambda.isLambda) expectedClassLikeType.contextParameterNumberForFunctionType else lambda.contextParameters.size

    val valueParametersTypesIncludingReceiver = expectedClassLikeType.valueParameterTypesIncludingReceiver(session)
    val isExtensionFunctionType = expectedClassLikeType.isExtensionFunctionType(session)
    val expectedParameters = valueParametersTypesIncludingReceiver.let {
        val forExtension = if (receiverType != null && isExtensionFunctionType) 1 else 0
        val toDrop = forExtension + contextParameterNumber

        if (toDrop > 0) it.drop(toDrop) else it
    }.map {
        // @ParameterName is assumed to be used for Ctrl+P on the call site of a property with a function type.
        // Propagating it further may affect further inference might work weirdly, and for sure,
        // it's not expected to leak in implicitly typed declarations.
        it.removeParameterNameAnnotation()
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
            if (firstValueParameter?.returnTypeRef?.coneTypeSafe<ConeKotlinType>() == extensionParameter) {
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

    val contextParameters =
        when {
            contextParameterNumber == 0 -> emptyList()
            lambda.isLambda -> valueParametersTypesIncludingReceiver.subList(0, contextParameterNumber)
            else -> lambda.contextParameters.map { it.returnTypeRef.coneType }
        }

    return ConeResolvedLambdaAtom(
        argument,
        expectedClassLikeType,
        actualFunctionKind ?: expectedFunctionKind,
        receiverType,
        contextParameters,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = returnTypeVariable,
        coerceFirstParameterToExtensionReceiver,
        sourceForFunctionExpression,
    )
}
