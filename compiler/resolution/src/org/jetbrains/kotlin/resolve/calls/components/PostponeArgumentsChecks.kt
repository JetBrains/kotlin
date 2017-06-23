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
        parameterDescriptor: ValueParameterDescriptor,
        postponeCallableReferenceArguments: MutableList<PostponeCallableReferenceArgument>
): KotlinCallDiagnostic? {
    val expectedType = argument.getExpectedType(parameterDescriptor)
    return when (argument) {
        is LambdaKotlinCallArgument -> processLambdaArgument(kotlinCall, csBuilder, argument, expectedType)
        is CallableReferenceKotlinCallArgument -> {
            // callable reference resolution will be run after choosing single descriptor
            postponeCallableReferenceArguments.add(PostponeCallableReferenceArgument(argument, expectedType))
            checkCallableExpectedType(csBuilder, argument, expectedType)
        }
        is CollectionLiteralKotlinCallArgument -> processCollectionLiteralArgument(kotlinCall, csBuilder, argument, expectedType)
        else -> unexpectedArgument(argument)
    }
}

// if expected type isn't function type, then may be it is Function<R>, Any or just `T`
private fun processLambdaArgument(
        kotlinCall: KotlinCall,
        csBuilder: ConstraintSystemBuilder,
        argument: LambdaKotlinCallArgument,
        expectedType: UnwrappedType
): KotlinCallDiagnostic? {
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

    val resolvedArgument = ResolvedLambdaArgument(kotlinCall, argument, isSuspend, receiverType, parameters, returnType)

    csBuilder.addSubtypeConstraint(resolvedArgument.type, expectedType, ArgumentConstraintPosition(argument))
    csBuilder.addLambdaArgument(resolvedArgument)

    return null
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

private fun checkCallableExpectedType(
        csBuilder: ConstraintSystemBuilder,
        argument: CallableReferenceKotlinCallArgument,
        expectedType: UnwrappedType
): KotlinCallDiagnostic? {
    val notCallableTypeConstructor = csBuilder.getProperSuperTypeConstructors(expectedType).firstOrNull { !ReflectionTypes.isPossibleExpectedCallableType(it) }
    return notCallableTypeConstructor?.let { NotCallableExpectedType(argument, expectedType, notCallableTypeConstructor) }
}

fun processCallableReferenceArgument(
        callContext: KotlinCallContext,
        kotlinCall: KotlinCall,
        csBuilder: ConstraintSystemBuilder,
        argument: CallableReferenceKotlinCallArgument,
        expectedType: UnwrappedType
): KotlinCallDiagnostic? {
    val subLHSCall = ((argument.lhsResult as? LHSResult.Expression)?.lshCallArgument as? SubKotlinCallArgument)
    if (subLHSCall != null) {
        csBuilder.addInnerCall(subLHSCall.resolvedCall)
    }
    val candidates = callContext.callableReferenceResolver.runRLSResolution(callContext, argument, expectedType) { checkCallableReference ->
        csBuilder.runTransaction { checkCallableReference(this); false }
    }
    val chosenCandidate = when (candidates.size) {
        0 -> return NoneCallableReferenceCandidates(argument)
        1 -> candidates.single()
        else -> return CallableReferenceCandidatesAmbiguity(argument, candidates)
    }
    val (toFreshSubstitutor, diagnostic) = with(chosenCandidate) {
        csBuilder.checkCallableReference(argument, dispatchReceiver, extensionReceiver, candidate,
                                         reflectionCandidateType, expectedType, callContext.scopeTower.lexicalScope.ownerDescriptor)
    }

    val resolvedCallableReference = ResolvedCallableReferenceArgument(
            kotlinCall, argument, toFreshSubstitutor.freshVariables,
            chosenCandidate, toFreshSubstitutor.safeSubstitute(chosenCandidate.reflectionCandidateType))

    csBuilder.addCallableReferenceArgument(resolvedCallableReference)

    return diagnostic
}

fun processCollectionLiteralArgument(
        kotlinCall: KotlinCall,
        csBuilder: ConstraintSystemBuilder,
        collectionLiteralArgument: CollectionLiteralKotlinCallArgument,
        expectedType: UnwrappedType
): KotlinCallDiagnostic? {
    csBuilder.addCollectionLiteralArgument(ResolvedCollectionLiteralArgument(kotlinCall, collectionLiteralArgument, expectedType))
    return null
}
