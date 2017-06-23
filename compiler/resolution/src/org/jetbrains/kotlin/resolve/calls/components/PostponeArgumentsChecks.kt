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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableForLambdaReturnType
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns

fun createPostponedArgumentAndPerformInitialChecks(
        kotlinCall: KotlinCall,
        csBuilder: ConstraintSystemBuilder,
        argument: PostponableKotlinCallArgument,
        parameterDescriptor: ValueParameterDescriptor
): KotlinCallDiagnostic? {
    val expectedType = argument.getExpectedType(parameterDescriptor)
    val (postponedArgument, diagnostic) =  when (argument) {
        is LambdaKotlinCallArgument -> preprocessLambdaArgument(kotlinCall, csBuilder, argument, expectedType)
        is CallableReferenceKotlinCallArgument -> preprocessCallableReference(csBuilder, argument, expectedType)
        is CollectionLiteralKotlinCallArgument -> preprocessCollectionLiteralArgument(csBuilder, argument, expectedType)
        else -> unexpectedArgument(argument)
    }
    csBuilder.addPostponedArgument(postponedArgument)

    return diagnostic
}

// if expected type isn't function type, then may be it is Function<R>, Any or just `T`
private fun preprocessLambdaArgument(
        kotlinCall: KotlinCall,
        csBuilder: ConstraintSystemBuilder,
        argument: LambdaKotlinCallArgument,
        expectedType: UnwrappedType
): Pair<PostponedLambdaArgument, KotlinCallDiagnostic?> {
    val builtIns = expectedType.builtIns
    val isSuspend = expectedType.isSuspendFunctionType

    val receiverType: UnwrappedType? // null means that there is no receiver
    val parameters: List<UnwrappedType>
    val returnType: UnwrappedType

    if (expectedType.isBuiltinFunctionalType) {
        receiverType = if (argument is FunctionExpression) argument.receiverType else expectedType.getReceiverTypeFromFunctionType()?.unwrap()

        val expectedParameters = expectedType.getValueParameterTypesFromFunctionType()
        if (argument.parametersTypes != null) {
            parameters = argument.parametersTypes!!.mapIndexed {
                index, type ->
                type ?: expectedParameters.getOrNull(index)?.type?.unwrap() ?: builtIns.anyType
            }
        }
        else {
            // lambda without explicit parameters: { }
            parameters = expectedParameters.map { it.type.unwrap() }
        }
        returnType = (argument as? FunctionExpression)?.returnType ?: expectedType.getReturnTypeFromFunctionType().unwrap()
    }
    else {
        val isFunctionSupertype = KotlinBuiltIns.isNotNullOrNullableFunctionSupertype(expectedType)
        receiverType = (argument as? FunctionExpression)?.receiverType
        parameters = argument.parametersTypes?.map { it ?: builtIns.nothingType } ?: emptyList()
        returnType = (argument as? FunctionExpression)?.returnType ?:
                     expectedType.arguments.singleOrNull()?.type?.unwrap()?.takeIf { isFunctionSupertype } ?:
                     createFreshTypeVariableForLambdaReturnType(csBuilder, argument, builtIns)

        // what about case where expected type is type variable? In old TY such cases was not supported. => do nothing for now. todo design
    }

    val resolvedArgument = PostponedLambdaArgument(kotlinCall, argument, isSuspend, receiverType, parameters, returnType)

    csBuilder.addSubtypeConstraint(resolvedArgument.type, expectedType, ArgumentConstraintPosition(argument))

    return resolvedArgument to null
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
