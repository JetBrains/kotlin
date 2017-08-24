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
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableForLambdaReturnType
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun resolveKtPrimitive(
        csBuilder: ConstraintSystemBuilder,
        argument: KotlinCallArgument,
        expectedType: UnwrappedType?,
        diagnosticsHolder: KotlinDiagnosticsHolder,
        isReceiver: Boolean
): ResolvedAtom = when (argument) {
    is SimpleKotlinCallArgument -> checkSimpleArgument(csBuilder, argument, expectedType, diagnosticsHolder, isReceiver)
    is LambdaKotlinCallArgument -> preprocessLambdaArgument(csBuilder, argument, expectedType)
    is CallableReferenceKotlinCallArgument -> preprocessCallableReference(csBuilder, argument, expectedType, diagnosticsHolder)
    is CollectionLiteralKotlinCallArgument -> preprocessCollectionLiteralArgument(argument, expectedType)
    else -> unexpectedArgument(argument)
}


// if expected type isn't function type, then may be it is Function<R>, Any or just `T`
private fun preprocessLambdaArgument(
        csBuilder: ConstraintSystemBuilder,
        argument: LambdaKotlinCallArgument,
        expectedType: UnwrappedType?
): ResolvedAtom {
    val builtIns = csBuilder.builtIns
    val isSuspend = expectedType?.isSuspendFunctionType ?: false

    val receiverType: UnwrappedType? // null means that there is no receiver
    val parameters: List<UnwrappedType>
    val returnType: UnwrappedType
    val typeVariable = TypeVariableForLambdaReturnType(argument, builtIns, "_L")

    if (expectedType?.isBuiltinFunctionalType == true) {
        receiverType = if (argument is FunctionExpression) argument.receiverType else expectedType.getReceiverTypeFromFunctionType()?.unwrap()

        val expectedParameters = expectedType.getValueParameterTypesFromFunctionType()
        parameters = if (argument.parametersTypes != null) {
            argument.parametersTypes!!.mapIndexed {
                index, type ->
                type ?: expectedParameters.getOrNull(index)?.type?.unwrap() ?: builtIns.nullableAnyType
            }
        }
        else {
            // lambda without explicit parameters: { }
            expectedParameters.map { it.type.unwrap() }
        }
        returnType = argument.safeAs<FunctionExpression>()?.returnType ?: expectedType.getReturnTypeFromFunctionType().unwrap()
    }
    else {
        val isFunctionSupertype = expectedType != null && KotlinBuiltIns.isNotNullOrNullableFunctionSupertype(expectedType)
        receiverType = argument.safeAs<FunctionExpression>()?.receiverType
        parameters = argument.parametersTypes?.map { it ?: builtIns.nothingType } ?: emptyList()
        returnType = argument.safeAs<FunctionExpression>()?.returnType ?:
                     expectedType?.arguments?.singleOrNull()?.type?.unwrap()?.takeIf { isFunctionSupertype } ?:
                     typeVariable.defaultType

        // what about case where expected type is type variable? In old TY such cases was not supported. => do nothing for now. todo design
    }

    val newTypeVariableUsed = returnType == typeVariable.defaultType
    if (newTypeVariableUsed) csBuilder.registerVariable(typeVariable)

    if (expectedType != null) {
        val lambdaType = createFunctionType(returnType.builtIns, Annotations.EMPTY, receiverType, parameters, null, returnType, isSuspend)
        csBuilder.addSubtypeConstraint(lambdaType, expectedType, ArgumentConstraintPosition(argument))
    }

    return ResolvedLambdaAtom(argument, isSuspend, receiverType, parameters, returnType, typeVariable.takeIf { newTypeVariableUsed })
}

private fun preprocessCallableReference(
        csBuilder: ConstraintSystemBuilder,
        argument: CallableReferenceKotlinCallArgument,
        expectedType: UnwrappedType?,
        diagnosticsHolder: KotlinDiagnosticsHolder
): ResolvedAtom {
    val result = ResolvedCallableReferenceAtom(argument, expectedType)
    if (expectedType == null) return result

    val notCallableTypeConstructor = csBuilder.getProperSuperTypeConstructors(expectedType).firstOrNull { !ReflectionTypes.isPossibleExpectedCallableType(it) }
    if (notCallableTypeConstructor != null) {
        diagnosticsHolder.addDiagnostic(NotCallableExpectedType(argument, expectedType, notCallableTypeConstructor))
    }
    return result
}

private fun preprocessCollectionLiteralArgument(
        collectionLiteralArgument: CollectionLiteralKotlinCallArgument,
        expectedType: UnwrappedType?
): ResolvedAtom {
    // todo add some checks about expected type
    return ResolvedCollectionLiteralAtom(collectionLiteralArgument, expectedType)
}
