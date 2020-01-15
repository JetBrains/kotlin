/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.LHSArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableForLambdaReturnType
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun resolveKtPrimitive(
    csBuilder: ConstraintSystemBuilder,
    argument: KotlinCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: KotlinDiagnosticsHolder,
    isReceiver: Boolean
): ResolvedAtom = when (argument) {
    is SimpleKotlinCallArgument ->
        checkSimpleArgument(csBuilder, argument, expectedType, diagnosticsHolder, isReceiver)

    is LambdaKotlinCallArgument ->
        preprocessLambdaArgument(csBuilder, argument, expectedType)

    is CallableReferenceKotlinCallArgument ->
        preprocessCallableReference(csBuilder, argument, expectedType, diagnosticsHolder)

    is CollectionLiteralKotlinCallArgument ->
        preprocessCollectionLiteralArgument(argument, expectedType)

    else -> unexpectedArgument(argument)
}


// if expected type isn't function type, then may be it is Function<R>, Any or just `T`
private fun preprocessLambdaArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: LambdaKotlinCallArgument,
    expectedType: UnwrappedType?,
    forceResolution: Boolean = false,
    returnTypeVariable: TypeVariableForLambdaReturnType? = null
): ResolvedAtom {
    if (expectedType != null && !forceResolution && csBuilder.isTypeVariable(expectedType)) {
        return LambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType)
    }

    val resolvedArgument = extractLambdaInfoFromFunctionalType(expectedType, argument, returnTypeVariable)
        ?: extraLambdaInfo(expectedType, argument, csBuilder)

    if (expectedType != null) {
        val lambdaType = createFunctionType(
            csBuilder.builtIns, Annotations.EMPTY, resolvedArgument.receiver,
            resolvedArgument.parameters, null, resolvedArgument.returnType, resolvedArgument.isSuspend
        )
        csBuilder.addSubtypeConstraint(lambdaType, expectedType, ArgumentConstraintPosition(argument))
    }

    return resolvedArgument
}

private fun extraLambdaInfo(
    expectedType: UnwrappedType?,
    argument: LambdaKotlinCallArgument,
    csBuilder: ConstraintSystemBuilder
): ResolvedLambdaAtom {
    val builtIns = csBuilder.builtIns
    val isSuspend = expectedType?.isSuspendFunctionType ?: false

    val isFunctionSupertype = expectedType != null && KotlinBuiltIns.isNotNullOrNullableFunctionSupertype(expectedType)
    val argumentAsFunctionExpression = argument.safeAs<FunctionExpression>()

    val typeVariable = TypeVariableForLambdaReturnType(argument, builtIns, "_L")

    val receiverType = argumentAsFunctionExpression?.receiverType
    val returnType =
        argumentAsFunctionExpression?.returnType ?: expectedType?.arguments?.singleOrNull()?.type?.unwrap()?.takeIf { isFunctionSupertype }
        ?: typeVariable.defaultType

    val parameters = argument.parametersTypes?.map { it ?: builtIns.nothingType } ?: emptyList()

    val newTypeVariableUsed = returnType == typeVariable.defaultType
    if (newTypeVariableUsed) csBuilder.registerVariable(typeVariable)

    return ResolvedLambdaAtom(
        argument,
        isSuspend,
        receiverType,
        parameters,
        returnType,
        typeVariable.takeIf { newTypeVariableUsed },
        expectedType
    )
}

private fun extractLambdaInfoFromFunctionalType(
    expectedType: UnwrappedType?,
    argument: LambdaKotlinCallArgument,
    returnTypeVariable: TypeVariableForLambdaReturnType? = null
): ResolvedLambdaAtom? {
    if (expectedType == null || !expectedType.isBuiltinFunctionalType) return null
    val parametersTypes = argument.parametersTypes
    val expectedParameters = expectedType.getValueParameterTypesFromFunctionType()
    val expectedReceiver = expectedType.getReceiverTypeFromFunctionType()?.unwrap()
    val argumentAsFunctionExpression = argument.safeAs<FunctionExpression>()

    val receiverFromExpected = argumentAsFunctionExpression?.receiverType == null && expectedReceiver != null

    fun UnwrappedType?.orExpected(index: Int) =
        this ?: expectedParameters.getOrNull(index)?.type?.unwrap() ?: expectedType.builtIns.nullableAnyType

    // Extracting parameters and receiver type, taking into account the actual lambda definition and expected lambda type
    val (parameters, receiver) = when {
        argumentAsFunctionExpression != null -> {
            // lambda has explicit functional type - use types from it if available
            (parametersTypes?.mapIndexed { index, type ->
                type.orExpected(index)
            } ?: emptyList()) to argumentAsFunctionExpression.receiverType
        }

        (parametersTypes?.size ?: 0) == expectedParameters.size && receiverFromExpected -> {
            // expected type has receiver, but arguments sizes are the same in actual and expected, so assuming missing (maybe unused) receiver in lambda
            // TODO: in case of implicit parameters in lambda ("this" and "it") this case assumes "this", probably we should generate two possible overloads and choose among them later
            (parametersTypes?.mapIndexed { index, type ->
                type.orExpected(index)
            } ?: expectedParameters.map { it.type.unwrap() }) to expectedReceiver
        }

        (parametersTypes?.size ?: 0) - expectedParameters.size == 1 && receiverFromExpected -> {
            // one "missing" parameter in the expected parameters - first lambda parameter should be mapped to expected receiver
            // TODO: same "this" or "it" case from above could be applicable here as well

            (parametersTypes?.mapIndexed { index, type ->
                type ?: run {
                    expectedParameters.getOrNull(index)?.type?.unwrap()
                } ?: expectedType.builtIns.nullableAnyType
            } ?: expectedParameters.map { it.type.unwrap() }) to expectedReceiver?.unwrap()
        }

        else ->
            (parametersTypes?.mapIndexed { index, type ->
                type.orExpected(index)
            } ?: expectedParameters.map { it.type.unwrap() }) to (if (receiverFromExpected) expectedReceiver else null)
    }

    val returnType = argumentAsFunctionExpression?.returnType ?: expectedType.getReturnTypeFromFunctionType().unwrap()

    return ResolvedLambdaAtom(
        argument,
        expectedType.isSuspendFunctionType,
        receiver,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = returnTypeVariable,
        expectedType = expectedType
    )
}

fun LambdaWithTypeVariableAsExpectedTypeAtom.transformToResolvedLambda(
    csBuilder: ConstraintSystemBuilder,
    expectedType: UnwrappedType? = null,
    returnTypeVariable: TypeVariableForLambdaReturnType? = null
): ResolvedLambdaAtom {
    val fixedExpectedType = (csBuilder.buildCurrentSubstitutor() as NewTypeSubstitutor)
        .safeSubstitute(expectedType ?: this.expectedType)
    val resolvedLambdaAtom = preprocessLambdaArgument(
        csBuilder,
        atom,
        fixedExpectedType,
        forceResolution = true,
        returnTypeVariable = returnTypeVariable
    ) as ResolvedLambdaAtom

    setAnalyzed(resolvedLambdaAtom)

    return resolvedLambdaAtom
}

private fun preprocessCallableReference(
    csBuilder: ConstraintSystemBuilder,
    argument: CallableReferenceKotlinCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: KotlinDiagnosticsHolder
): ResolvedAtom {
    val result = EagerCallableReferenceAtom(argument, expectedType)

    if (expectedType == null) return result

    val lhsResult = argument.lhsResult
    if (lhsResult is LHSResult.Type) {
        csBuilder.addConstraintFromLHS(argument, lhsResult, expectedType)
    }

    val notCallableTypeConstructor =
        csBuilder.getProperSuperTypeConstructors(expectedType)
            .firstOrNull { !ReflectionTypes.isPossibleExpectedCallableType(it.requireIs()) }

    if (notCallableTypeConstructor != null) {
        diagnosticsHolder.addDiagnostic(
            NotCallableExpectedType(
                argument,
                expectedType,
                notCallableTypeConstructor.requireIs()
            )
        )
    }
    return result
}

private fun ConstraintSystemBuilder.addConstraintFromLHS(
    argument: CallableReferenceKotlinCallArgument,
    lhsResult: LHSResult.Type,
    expectedType: UnwrappedType
) {
    if (!ReflectionTypes.isNumberedTypeWithOneOrMoreNumber(expectedType)) return

    val lhsType = lhsResult.unboundDetailedReceiver.stableType
    val expectedTypeProjectionForLHS = expectedType.arguments.first()
    val expectedTypeForLHS = expectedTypeProjectionForLHS.type
    val constraintPosition = LHSArgumentConstraintPosition(argument, lhsResult.qualifier ?: lhsResult.unboundDetailedReceiver)

    when (expectedTypeProjectionForLHS.projectionKind) {
        Variance.INVARIANT -> addEqualityConstraint(lhsType, expectedTypeForLHS, constraintPosition)
        Variance.IN_VARIANCE -> addSubtypeConstraint(expectedTypeForLHS, lhsType, constraintPosition)
        Variance.OUT_VARIANCE -> addSubtypeConstraint(lhsType, expectedTypeForLHS, constraintPosition)
    }
}

private fun preprocessCollectionLiteralArgument(
    collectionLiteralArgument: CollectionLiteralKotlinCallArgument,
    expectedType: UnwrappedType?
): ResolvedAtom {
    // todo add some checks about expected type
    return ResolvedCollectionLiteralAtom(collectionLiteralArgument, expectedType)
}

internal inline fun <reified T : Any> Any.requireIs(): T {
    require(this is T)
    return this
}
