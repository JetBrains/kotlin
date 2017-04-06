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

import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.LambdaTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.captureFromExpression
import org.jetbrains.kotlin.types.checker.hasSupertypeWithGivenTypeConstructor
import org.jetbrains.kotlin.types.checker.intersectWrappedTypes
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull
import java.lang.UnsupportedOperationException

internal object CheckArguments : ResolutionPart {
    override fun SimpleKotlinResolutionCandidate.process(): List<KotlinCallDiagnostic> {
        val diagnostics = SmartList<KotlinCallDiagnostic>()
        for (parameterDescriptor in descriptorWithFreshTypes.valueParameters) {
            // error was reported in ArgumentsToParametersMapper
            val resolvedCallArgument = argumentMappingByOriginal[parameterDescriptor.original] ?: continue
            for (argument in resolvedCallArgument.arguments) {

                val diagnostic = checkArgument(callContext, kotlinCall, csBuilder, argument, argument.getExpectedType(parameterDescriptor))
                diagnostics.addIfNotNull(diagnostic)

                if (diagnostic != null && !diagnostic.candidateApplicability.isSuccess) break
            }
        }
        return diagnostics
    }

    fun checkArgument(
            callContext: KotlinCallContext,
            kotlinCall: KotlinCall,
            csBuilder: ConstraintSystemBuilder,
            argument: KotlinCallArgument,
            expectedType: UnwrappedType
    ): KotlinCallDiagnostic? {
        return when (argument) {
            is ExpressionKotlinCallArgument -> checkExpressionArgument(csBuilder, argument, expectedType, isReceiver = false)
            is SubKotlinCallArgument -> checkSubCallArgument(csBuilder, argument, expectedType, isReceiver = false)
            is LambdaKotlinCallArgument -> processLambdaArgument(kotlinCall, csBuilder, argument, expectedType)
            is CallableReferenceKotlinCallArgument -> processCallableReferenceArgument(callContext, kotlinCall, csBuilder, argument, expectedType)
            else -> error("Incorrect argument type: $argument, ${argument.javaClass.canonicalName}.")
        }
    }

    inline fun computeParameterTypes(
            argument: LambdaKotlinCallArgument,
            expectedType: UnwrappedType,
            createFreshType: () -> UnwrappedType
    ): List<UnwrappedType> {
        argument.parametersTypes?.map { it ?: createFreshType() } ?.let { return it }

        if (expectedType.isFunctionType) {
            return expectedType.getValueParameterTypesFromFunctionType().map { createFreshType() }
        }

        // if expected type is non-functional type and there is no declared parameters
        return emptyList()
    }

    inline fun computeReceiver(
            argument: LambdaKotlinCallArgument,
            expectedType: UnwrappedType,
            createFreshType: () -> UnwrappedType
    ) : UnwrappedType? {
        if (argument is FunctionExpression) return argument.receiverType

        if (expectedType.isExtensionFunctionType) return createFreshType()

        return null
    }

    inline fun computeReturnType(
            argument: LambdaKotlinCallArgument,
            createFreshType: () -> UnwrappedType
    ) : UnwrappedType {
        if (argument is FunctionExpression) return argument.receiverType ?: createFreshType()

        return createFreshType()
    }

    fun processLambdaArgument(
            kotlinCall: KotlinCall,
            csBuilder: ConstraintSystemBuilder,
            argument: LambdaKotlinCallArgument,
            expectedType: UnwrappedType
    ): KotlinCallDiagnostic? {
        // initial checks
        if (expectedType.isFunctionType) {
            val expectedParameterCount = expectedType.getValueParameterTypesFromFunctionType().size

            argument.parametersTypes?.size?.let {
                if (expectedParameterCount != it) return ExpectedLambdaParametersCountMismatch(argument, expectedParameterCount, it)
            }

            if (argument is FunctionExpression) {
                if (argument.receiverType != null && !expectedType.isExtensionFunctionType) return UnexpectedReceiver(argument)
                if (argument.receiverType == null && expectedType.isExtensionFunctionType) return MissingReceiver(argument)
            }
        }

        val builtIns = expectedType.builtIns
        val freshVariables = SmartList<LambdaTypeVariable>()
        val receiver = computeReceiver(argument, expectedType) {
            LambdaTypeVariable(argument, LambdaTypeVariable.Kind.RECEIVER, builtIns).apply { freshVariables.add(this) }.defaultType
        }

        val parameters = computeParameterTypes(argument, expectedType) {
            LambdaTypeVariable(argument, LambdaTypeVariable.Kind.PARAMETER, builtIns).apply { freshVariables.add(this) }.defaultType
        }

        val returnType = computeReturnType(argument) {
            LambdaTypeVariable(argument, LambdaTypeVariable.Kind.RETURN_TYPE, builtIns).apply { freshVariables.add(this) }.defaultType
        }

        val resolvedArgument = ResolvedLambdaArgument(kotlinCall, argument, freshVariables, receiver, parameters, returnType)

        freshVariables.forEach(csBuilder::registerVariable)
        csBuilder.addSubtypeConstraint(resolvedArgument.type, expectedType, ArgumentConstraintPosition(argument))
        csBuilder.addLambdaArgument(resolvedArgument)

        return null
    }

    fun processCallableReferenceArgument(
            callContext: KotlinCallContext,
            kotlinCall: KotlinCall,
            csBuilder: ConstraintSystemBuilder,
            argument: CallableReferenceKotlinCallArgument,
            expectedType: UnwrappedType
    ): KotlinCallDiagnostic? {
        val position = ArgumentConstraintPosition(argument)

        if (argument !is ChosenCallableReferenceDescriptor) {
            val lhsType = argument.lhsType
            if (lhsType != null) {
                // todo: case with two receivers
                val expectedReceiverType = expectedType.supertypes().firstOrNull { it.isFunctionType }?.arguments?.first()?.type?.unwrap()
                if (expectedReceiverType != null) {
                    // (lhsType) -> .. <: (expectedReceiverType) -> ... => expectedReceiverType <: lhsType
                    csBuilder.addSubtypeConstraint(expectedReceiverType, lhsType, position)
                }
            }

            return null
        }

        val descriptor = argument.candidate.descriptor
        when (descriptor) {
            is FunctionDescriptor -> {
                // todo store resolved
                val resolvedFunctionReference = callContext.callableReferenceResolver.resolveFunctionReference(
                        argument, kotlinCall, expectedType)

                csBuilder.addSubtypeConstraint(resolvedFunctionReference.reflectionType, expectedType, position)
                return resolvedFunctionReference.argumentsMapping?.diagnostics?.let {
                    ErrorCallableMapping(resolvedFunctionReference)
                }
            }
            is PropertyDescriptor -> {

                // todo store resolved
                val resolvedPropertyReference = callContext.callableReferenceResolver.resolvePropertyReference(descriptor,
                                                                                                                 argument, kotlinCall, callContext.scopeTower.lexicalScope.ownerDescriptor)
                csBuilder.addSubtypeConstraint(resolvedPropertyReference.reflectionType, expectedType, position)
            }
            else -> throw UnsupportedOperationException("Callable reference resolved to an unsupported descriptor: $descriptor")
        }
        return null
    }
}

internal fun checkExpressionArgument(
        csBuilder: ConstraintSystemBuilder,
        expressionArgument: ExpressionKotlinCallArgument,
        expectedType: UnwrappedType,
        isReceiver: Boolean
): KotlinCallDiagnostic? {
    // todo run this approximation only once for call
    val argumentType = captureFromTypeParameterUpperBoundIfNeeded(expressionArgument.stableType, expectedType)

    fun unstableSmartCastOrSubtypeError(
            unstableType: UnwrappedType?, expectedType: UnwrappedType, position: ArgumentConstraintPosition
    ): KotlinCallDiagnostic? {
        if (unstableType != null) {
            if (csBuilder.addSubtypeConstraintIfCompatible(unstableType, expectedType, position)) {
                return UnstableSmartCast(expressionArgument, unstableType)
            }
        }
        csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
        return null
    }

    val expectedNullableType = expectedType.makeNullableAsSpecified(true)
    val position = ArgumentConstraintPosition(expressionArgument)
    if (expressionArgument.isSafeCall) {
        if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedNullableType, position)) {
            return unstableSmartCastOrSubtypeError(expressionArgument.unstableType, expectedNullableType, position)?.let { return it }
        }
        return null
    }

    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        if (!isReceiver) {
            return unstableSmartCastOrSubtypeError(expressionArgument.unstableType, expectedType, position)?.let { return it }
        }

        val unstableType = expressionArgument.unstableType
        if (unstableType != null && csBuilder.addSubtypeConstraintIfCompatible(unstableType, expectedType, position)) {
            return UnstableSmartCast(expressionArgument, unstableType)
        }
        else if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedNullableType, position)) {
            return UnsafeCallError(expressionArgument)
        }
        else {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            return null
        }
    }

    return null
}

/**
 * interface Inv<T>
 * fun <Y> bar(l: Inv<Y>): Y = ...
 *
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val xr = bar(x)
 * }
 * Here we try to capture from upper bound from type parameter.
 * We replace type of `x` to `Inv<out Int>`(we chose supertype which contains supertype with expectedTypeConstructor) and capture from this type.
 * It is correct, because it is like this code:
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val inv: Inv<out Int> = x
 *      val xr = bar(inv)
 * }
 *
 */
private fun captureFromTypeParameterUpperBoundIfNeeded(argumentType: UnwrappedType, expectedType: UnwrappedType): UnwrappedType {
    val expectedTypeConstructor = expectedType.upperIfFlexible().constructor

    if (argumentType.lowerIfFlexible().constructor.declarationDescriptor is TypeParameterDescriptor) {
        val chosenSupertype = argumentType.lowerIfFlexible().supertypes().singleOrNull {
            it.constructor.declarationDescriptor is ClassifierDescriptorWithTypeParameters &&
            it.unwrap().hasSupertypeWithGivenTypeConstructor(expectedTypeConstructor)
        }
        if (chosenSupertype != null) {
            return captureFromExpression(chosenSupertype.unwrap()) ?: argumentType
        }
    }

    return argumentType
}

// if expression is not stable and has smart casts, then we create this type
private val ExpressionKotlinCallArgument.unstableType: UnwrappedType?
    get() {
        if (receiver.isStable || receiver.possibleTypes.isEmpty()) return null
        return intersectWrappedTypes(receiver.possibleTypes + receiver.receiverValue.type)
    }

// with all smart casts if stable
internal val ExpressionKotlinCallArgument.stableType: UnwrappedType
    get() {
        if (!receiver.isStable || receiver.possibleTypes.isEmpty()) return receiver.receiverValue.type.unwrap()
        return intersectWrappedTypes(receiver.possibleTypes + receiver.receiverValue.type)
    }


internal fun checkSubCallArgument(
        csBuilder: ConstraintSystemBuilder,
        subCallArgument: SubKotlinCallArgument,
        expectedType: UnwrappedType,
        isReceiver: Boolean
): KotlinCallDiagnostic? {
    val resolvedCall = subCallArgument.resolvedCall
    val expectedNullableType = expectedType.makeNullableAsSpecified(true)
    val position = ArgumentConstraintPosition(subCallArgument)

    csBuilder.addInnerCall(resolvedCall)

    // subArgument cannot has stable smartcast
    val currentReturnType = subCallArgument.receiver.receiverValue.type.unwrap()
    if (subCallArgument.isSafeCall) {
        csBuilder.addSubtypeConstraint(currentReturnType, expectedNullableType, position)
        return null
    }

    if (isReceiver && !csBuilder.addSubtypeConstraintIfCompatible(currentReturnType, expectedType, position) &&
        csBuilder.addSubtypeConstraintIfCompatible(currentReturnType, expectedNullableType, position)
    ) {
        return UnsafeCallError(subCallArgument)
    }

    csBuilder.addSubtypeConstraint(currentReturnType, expectedType, position)
    return null
}

internal fun KotlinCallArgument.getExpectedType(parameter: ValueParameterDescriptor) =
        if (this.isSpread) {
            parameter.type.unwrap()
        }
        else {
            parameter.varargElementType?.unwrap() ?: parameter.type.unwrap()
        }