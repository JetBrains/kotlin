/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.types.*

fun extractLambdaInfoFromFunctionalType(
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef?,
    argument: FirAnonymousFunction,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
    components: BodyResolveComponents,
    candidate: Candidate?,
    duringCompletion: Boolean,
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
            candidate,
            duringCompletion
        )
    }
    if (!expectedType.isBuiltinFunctionalType(session)) return null

    val singleStatement = argument.body?.statements?.singleOrNull() as? FirReturnExpression
    if (argument.returnType == null && singleStatement != null &&
        singleStatement.target.labeledElement == argument && singleStatement.result is FirUnitExpression
    ) {
        // Simply { }, i.e., function literals without body. Raw FIR added an implicit return with an implicit unit type ref.
        argument.replaceReturnTypeRef(session.builtinTypes.unitType)
    }
    val returnType = argument.returnType ?: expectedType.returnType(session)

    // `fun (x: T) = ...` and `fun T.() = ...` are both instances of `T.() -> V` and `(T) -> V`; `fun () = ...` is not.
    // For lambdas, the existence of the receiver is always implied by the expected type, and a value parameter
    // can never fill its role.
    val receiverType = if (argument.isLambda) expectedType.receiverType(session) else argument.receiverType
    val contextReceiversNumber =
        if (argument.isLambda) expectedType.contextReceiversNumberForFunctionType else argument.contextReceivers.size

    val valueParametersTypesIncludingReceiver = expectedType.valueParameterTypesIncludingReceiver(session)
    val isExtensionFunctionType = expectedType.isExtensionFunctionType(session)
    val expectedParameters = valueParametersTypesIncludingReceiver.let {
        val forExtension = if (receiverType != null && isExtensionFunctionType) 1 else 0
        val toDrop = forExtension + contextReceiversNumber

        if (toDrop > 0) it.drop(toDrop) else it
    }

    var coerceFirstParameterToExtensionReceiver = false
    val argumentValueParameters = argument.valueParameters
    val parameters = if (argument.isLambda && !argument.hasExplicitParameterList && expectedParameters.size < 2) {
        expectedParameters // Infer existence of a parameter named `it` of an appropriate type.
    } else {
        if (duringCompletion &&
            argument.isLambda &&
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

        argumentValueParameters.mapIndexed { index, parameter ->
            parameter.returnTypeRef.coneTypeSafe()
                ?: expectedParameters.getOrNull(index)
                ?: ConeErrorType(
                    ConeSimpleDiagnostic("Cannot infer type for parameter ${parameter.name}", DiagnosticKind.CannotInferParameterType)
                )
        }
    }

    val contextReceivers =
        when {
            contextReceiversNumber == 0 -> emptyList()
            argument.isLambda -> valueParametersTypesIncludingReceiver.subList(0, contextReceiversNumber)
            else -> argument.contextReceivers.map { it.typeRef.coneType }
        }

    return ResolvedLambdaAtom(
        argument,
        expectedType,
        expectedType.isSuspendFunctionType(session),
        receiverType,
        contextReceivers,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = returnTypeVariable,
        candidate,
        coerceFirstParameterToExtensionReceiver
    )
}
