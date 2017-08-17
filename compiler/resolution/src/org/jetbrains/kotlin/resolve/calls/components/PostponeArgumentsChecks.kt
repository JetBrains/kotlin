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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableForLambdaReturnType
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun createPostponedArgumentAndPerformInitialChecks(
        csBuilder: ConstraintSystemBuilder,
        argument: PostponableKotlinCallArgument,
        expectedType: UnwrappedType,
        parameterName: Name? = null
): KotlinCallDiagnostic? {
    val (postponedArgument, diagnostic) =  when (argument) {
        is LambdaKotlinCallArgument -> preprocessLambdaArgument(csBuilder, argument, expectedType, parameterName)
        is CallableReferenceKotlinCallArgument -> preprocessCallableReference(csBuilder, argument, expectedType)
        is CollectionLiteralKotlinCallArgument -> preprocessCollectionLiteralArgument(csBuilder, argument, expectedType)
        else -> unexpectedArgument(argument)
    }
    csBuilder.addPostponedArgument(postponedArgument)

    return diagnostic
}

// if expected type isn't function type, then may be it is Function<R>, Any or just `T`
private fun preprocessLambdaArgument(
        csBuilder: ConstraintSystemBuilder,
        argument: LambdaKotlinCallArgument,
        expectedType: UnwrappedType,
        parameterName: Name? = null
): Pair<PostponedLambdaArgument, KotlinCallDiagnostic?> {
    val newExpectedType = csBuilder.getProperSubTypeBounds(expectedType).singleOrNull() ?: expectedType

    val resolvedArgument = extractLambdaInfoFromFunctionalType(newExpectedType, argument) ?: extraLambdaInfo(newExpectedType, argument, csBuilder)

    csBuilder.addSubtypeConstraint(resolvedArgument.type, newExpectedType, ArgumentConstraintPosition(argument, parameterName))

    return resolvedArgument to null
}

private fun extraLambdaInfo(expectedType: UnwrappedType, argument: LambdaKotlinCallArgument, csBuilder: ConstraintSystemBuilder): PostponedLambdaArgument {
    val isFunctionSupertype = KotlinBuiltIns.isNotNullOrNullableFunctionSupertype(expectedType)
    val argumentAsFunctionExpression = argument.safeAs<FunctionExpression>()

    val receiverType = argumentAsFunctionExpression?.receiverType
    val returnType = argumentAsFunctionExpression?.returnType ?:
                     expectedType.arguments.singleOrNull()?.type?.unwrap()?.takeIf { isFunctionSupertype } ?:
                     createFreshTypeVariableForLambdaReturnType(csBuilder, argument, expectedType.builtIns)

    val parameters = argument.parametersTypes?.map { it ?: expectedType.builtIns.nothingType } ?: emptyList()

    return PostponedLambdaArgument(argument, expectedType.isSuspendFunctionType, receiverType, parameters, returnType)
}

private fun extractLambdaInfoFromFunctionalType(expectedType: UnwrappedType, argument: LambdaKotlinCallArgument): PostponedLambdaArgument? {
    if (!expectedType.isBuiltinFunctionalType) return null
    val parameters = extractLambdaParameters(expectedType, argument)

    val argumentAsFunctionExpression = argument.safeAs<FunctionExpression>()
    val receiverType = argumentAsFunctionExpression?.receiverType ?: expectedType.getReceiverTypeFromFunctionType()?.unwrap()
    val returnType = argumentAsFunctionExpression?.returnType ?: expectedType.getReturnTypeFromFunctionType().unwrap()

    return PostponedLambdaArgument(argument, expectedType.isSuspendFunctionType, receiverType, parameters, returnType)
}

private fun extractLambdaParameters(expectedType: UnwrappedType, argument: LambdaKotlinCallArgument): List<UnwrappedType> {
    val parametersTypes = argument.parametersTypes
    val expectedParameters = expectedType.getValueParameterTypesFromFunctionType()
    if (parametersTypes == null) {
        return expectedParameters.map { it.type.unwrap() }
    }

    return parametersTypes.mapIndexed { index, type ->
        type ?: expectedParameters.getOrNull(index)?.type?.unwrap() ?: expectedType.builtIns.nullableAnyType
    }
}

private fun createFreshTypeVariableForLambdaReturnType(
        csBuilder: ConstraintSystemBuilder,
        argument: LambdaKotlinCallArgument,
        builtIns: KotlinBuiltIns
): UnwrappedType {
    val typeVariable = TypeVariableForLambdaReturnType(argument, builtIns, "_L")
    csBuilder.registerVariable(typeVariable)
    return typeVariable.defaultType
}

private fun preprocessCallableReference(
        csBuilder: ConstraintSystemBuilder,
        argument: CallableReferenceKotlinCallArgument,
        expectedType: UnwrappedType
): Pair<PostponedCallableReferenceArgument, KotlinCallDiagnostic?> {
    val notCallableTypeConstructor = csBuilder.getProperSuperTypeConstructors(expectedType).firstOrNull { !ReflectionTypes.isPossibleExpectedCallableType(it) }
    val diagnostic = notCallableTypeConstructor?.let { NotCallableExpectedType(argument, expectedType, notCallableTypeConstructor) }
    return PostponedCallableReferenceArgument(argument, expectedType) to diagnostic
}

private fun preprocessCollectionLiteralArgument(
        csBuilder: ConstraintSystemBuilder,
        collectionLiteralArgument: CollectionLiteralKotlinCallArgument,
        expectedType: UnwrappedType
): Pair<PostponedCollectionLiteralArgument, KotlinCallDiagnostic?> {
    // todo add some checks about expected type
    return PostponedCollectionLiteralArgument(collectionLiteralArgument, expectedType) to null
}
