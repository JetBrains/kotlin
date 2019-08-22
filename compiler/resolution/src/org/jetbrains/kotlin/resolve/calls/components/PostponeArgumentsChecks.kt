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
import org.jetbrains.kotlin.resolve.calls.inference.model.VariadicTypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.KotlinType
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
    forceResolution: Boolean = false
// TODO add flag or check if there is no variadic type variables
): ResolvedAtom {
    if (expectedType != null && !forceResolution && csBuilder.isTypeVariable(expectedType)) {
        return LambdaWithTypeVariableAsExpectedTypeAtom(argument, expectedType)
    }

    val resolvedArgument = extractLambdaInfoFromFunctionalType(expectedType, argument, csBuilder)
        ?: extraLambdaInfo(expectedType, argument, csBuilder)

    if (expectedType != null) {
        val lambdaType = createFunctionType(
            csBuilder.builtIns, Annotations.EMPTY, resolvedArgument.receiver,
            resolvedArgument.parameters, null, resolvedArgument.returnType, resolvedArgument.isSuspend
        )
        val modifiedExpectedType = if (expectedType.isFunctionType) {
            val newParameters = expectedType.getValueParameterTypesFromFunctionType().flatMap {
                it.type.replaceTupleTypes(csBuilder)
            }
            createFunctionType(
                csBuilder.builtIns, expectedType.annotations, expectedType.getReceiverTypeFromFunctionType(),
                newParameters, null, expectedType.getReturnTypeFromFunctionType(), expectedType.isSuspendFunctionType
            )
        } else {
            expectedType // TODO also replace tuple types in non-function types
        }
        csBuilder.addSubtypeConstraint(lambdaType, modifiedExpectedType, ArgumentConstraintPosition(argument))
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
    csBuilder: ConstraintSystemBuilder
): ResolvedLambdaAtom? {
    if (expectedType == null || !expectedType.isBuiltinFunctionalType) return null
    val parameters = extractLambdaParameters(expectedType, argument, csBuilder)

    val argumentAsFunctionExpression = argument.safeAs<FunctionExpression>()
    val receiverType = argumentAsFunctionExpression?.receiverType ?: expectedType.getReceiverTypeFromFunctionType()?.unwrap()
    val returnType = argumentAsFunctionExpression?.returnType ?: expectedType.getReturnTypeFromFunctionType().unwrap()

    return ResolvedLambdaAtom(
        argument,
        expectedType.isSuspendFunctionType,
        receiverType,
        parameters,
        returnType,
        typeVariableForLambdaReturnType = null,
        expectedType = expectedType
    )
}

private fun extractLambdaParameters(
    expectedType: UnwrappedType,
    argument: LambdaKotlinCallArgument,
    csBuilder: ConstraintSystemBuilder
): List<UnwrappedType> {
    val parametersTypes = argument.parametersTypes
    val expectedParameters = expectedType
        .getValueParameterTypesFromFunctionType()
        .flatMap { it.type.replaceTupleTypes(csBuilder) }
    if (parametersTypes == null) {
        return expectedParameters.map { it.unwrap() }
    }

    return parametersTypes.mapIndexed { index, type ->
        type ?: expectedParameters.getOrNull(index)?.unwrap() ?: expectedType.builtIns.nullableAnyType
    }
}

// TODO deeply buried parameter pack
// TODO Add errors with csBuilder
fun KotlinType.replaceTupleTypes(csBuilder: ConstraintSystemBuilder): List<KotlinType> {
    if (!TupleType.isTupleType(this)) return listOf(this)

    val tupleTypeArgumentType = this.arguments.singleOrNull()?.type ?: return listOf(this)

    val csTypeVariables = csBuilder.currentStorage().allTypeVariables
    return csTypeVariables.values
        .filter {
            it.safeAs<VariadicTypeVariableFromCallableDescriptor>()?.packVariable?.defaultType == tupleTypeArgumentType
        }
        .map { (it as VariadicTypeVariableFromCallableDescriptor).defaultType }
}

fun LambdaWithTypeVariableAsExpectedTypeAtom.transformToResolvedLambda(
    csBuilder: ConstraintSystemBuilder
): ResolvedLambdaAtom {
    val fixedExpectedType = (csBuilder.buildCurrentSubstitutor() as NewTypeSubstitutor).safeSubstitute(expectedType)
    val resolvedLambdaAtom = preprocessLambdaArgument(csBuilder, atom, fixedExpectedType, forceResolution = true) as ResolvedLambdaAtom

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
        csBuilder.addConstraintFromLHS(lhsResult, expectedType)
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

private fun ConstraintSystemBuilder.addConstraintFromLHS(lhsResult: LHSResult.Type, expectedType: UnwrappedType) {
    if (!ReflectionTypes.isNumberedTypeWithOneOrMoreNumber(expectedType)) return

    val lhsType = lhsResult.unboundDetailedReceiver.stableType
    val expectedTypeProjectionForLHS = expectedType.arguments.first()
    val expectedTypeForLHS = expectedTypeProjectionForLHS.type
    val constraintPosition = LHSArgumentConstraintPosition(lhsResult.qualifier ?: lhsResult.unboundDetailedReceiver)

    when (expectedTypeProjectionForLHS.projectionKind) {
        Variance.INVARIANT -> addEqualityConstraint(lhsType, expectedTypeForLHS, constraintPosition)
        Variance.IN_VARIANCE -> addSubtypeConstraint(expectedType, lhsType, constraintPosition)
        Variance.OUT_VARIANCE -> addSubtypeConstraint(lhsType, expectedType, constraintPosition)
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