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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.LambdaArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.calls.inference.substituteAndApproximateCapturedTypes
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateStatus
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.contains

class KotlinCallCompleter(
        private val fixationOrderCalculator: FixationOrderCalculator,
        private val additionalDiagnosticReporter: AdditionalDiagnosticReporter,
        private val inferenceStepResolver: InferenceStepResolver
) {
    interface Context {
        val innerCalls: List<ResolvedKotlinCall.OnlyResolvedKotlinCall>
        val hasContradiction: Boolean
        fun buildCurrentSubstitutor(): NewTypeSubstitutor
        fun buildResultingSubstitutor(): NewTypeSubstitutor
        val postponedArguments: List<PostponedKotlinCallArgument>
        val lambdaArguments: List<PostponedLambdaArgument>

        // type can be proper if it not contains not fixed type variables
        fun canBeProper(type: UnwrappedType): Boolean
        fun asFixationOrderCalculatorContext(): FixationOrderCalculator.Context
        fun asResultTypeResolverContext(): ResultTypeResolver.Context

        // mutable operations
        fun asConstraintInjectorContext(): ConstraintInjector.Context
        fun addError(error: KotlinCallDiagnostic)
        fun fixVariable(variable: NewTypeVariable, resultType: UnwrappedType)
        fun getBuilder(): ConstraintSystemBuilder
    }

    fun transformWhenAmbiguity(candidate: KotlinResolutionCandidate, resolutionCallbacks: KotlinResolutionCallbacks): ResolvedKotlinCall =
            toCompletedBaseResolvedCall(candidate.lastCall.constraintSystem.asCallCompleterContext(), candidate, resolutionCallbacks)

    // todo investigate variable+function calls
    fun completeCallIfNecessary(
            candidate: KotlinResolutionCandidate,
            expectedType: UnwrappedType?,
            resolutionCallbacks: KotlinResolutionCallbacks
    ): ResolvedKotlinCall {
        resolutionCallbacks.bindStubResolvedCallForCandidate(candidate)
        val topLevelCall =
                when (candidate) {
                    is VariableAsFunctionKotlinResolutionCandidate -> candidate.invokeCandidate
                    else -> candidate as SimpleKotlinResolutionCandidate
                }

        if (topLevelCall.prepareForCompletion(expectedType)) {
            val c = candidate.lastCall.constraintSystem.asCallCompleterContext()

            resolveCallableReferenceArguments(c, candidate.lastCall)

            topLevelCall.competeCall(c, resolutionCallbacks)
            return toCompletedBaseResolvedCall(c, candidate, resolutionCallbacks)
        }

        return ResolvedKotlinCall.OnlyResolvedKotlinCall(candidate)
    }

    // todo do not use topLevelCall
    private fun resolveCallableReferenceArguments(c: Context, topLevelCall: SimpleKotlinResolutionCandidate) {
        for (callableReferenceArgument in c.postponedArguments) {
            if (callableReferenceArgument !is PostponedCallableReferenceArgument) continue
            processCallableReferenceArgument(topLevelCall.callContext, c.getBuilder(), callableReferenceArgument)
        }
    }

    private fun toCompletedBaseResolvedCall(
            c: Context,
            candidate: KotlinResolutionCandidate,
            resolutionCallbacks: KotlinResolutionCallbacks
    ): ResolvedKotlinCall.CompletedResolvedKotlinCall {
        val currentSubstitutor = c.buildResultingSubstitutor()
        val completedCall = candidate.toCompletedCall(currentSubstitutor)
        val competedCalls = c.innerCalls.map {
            it.candidate.toCompletedCall(currentSubstitutor)
        }
        for (postponedArgument in c.postponedArguments) {
            when (postponedArgument) {
                is PostponedLambdaArgument -> {
                    postponedArgument.finalReturnType = currentSubstitutor.safeSubstitute(postponedArgument.returnType)
                }
                is PostponedCallableReferenceArgument -> {
                    val resultTypeParameters = postponedArgument.myTypeVariables.map { currentSubstitutor.safeSubstitute(it.defaultType) }
                    resolutionCallbacks.completeCallableReference(postponedArgument, resultTypeParameters)
                }
                is PostponedCollectionLiteralArgument -> {
                    resolutionCallbacks.completeCollectionLiteralCalls(postponedArgument)
                }
            }
        }
        return ResolvedKotlinCall.CompletedResolvedKotlinCall(completedCall, competedCalls, c.lambdaArguments)
    }

    private fun KotlinResolutionCandidate.toCompletedCall(substitutor: NewTypeSubstitutor): CompletedKotlinCall {
        if (this is VariableAsFunctionKotlinResolutionCandidate) {
            val variable = resolvedVariable.toCompletedCall(substitutor)
            val invoke = invokeCandidate.toCompletedCall(substitutor)

            return CompletedKotlinCall.VariableAsFunction(kotlinCall, variable, invoke)
        }
        return (this as SimpleKotlinResolutionCandidate).toCompletedCall(substitutor)
    }

    private fun SimpleKotlinResolutionCandidate.toCompletedCall(substitutor: NewTypeSubstitutor): CompletedKotlinCall.Simple {
        val containsCapturedTypes = descriptorWithFreshTypes.returnType?.contains { it is NewCapturedType } ?: false
        val resultingDescriptor = when {
            descriptorWithFreshTypes is FunctionDescriptor ||
            (descriptorWithFreshTypes is PropertyDescriptor && (descriptorWithFreshTypes.typeParameters.isNotEmpty() || containsCapturedTypes)) ->
                // this code is very suspicious. Now it is very useful for BE, because they cannot do nothing with captured types,
                // but it seems like temporary solution.
                descriptorWithFreshTypes.substituteAndApproximateCapturedTypes(substitutor)
            else ->
                descriptorWithFreshTypes
        }

        val typeArguments = descriptorWithFreshTypes.typeParameters.map {
            val substituted = substitutor.safeSubstitute(typeVariablesForFreshTypeParameters[it.index].defaultType)
            TypeApproximator().approximateToSuperType(substituted, TypeApproximatorConfiguration.CapturedTypesApproximation) ?: substituted
        }

        val status = computeStatus(this, resultingDescriptor)
        return CompletedKotlinCall.Simple(kotlinCall, candidateDescriptor, resultingDescriptor, status, explicitReceiverKind,
                                          dispatchReceiverArgument?.receiver, extensionReceiver?.receiver, typeArguments, argumentMappingByOriginal)
    }

    private fun computeStatus(candidate: SimpleKotlinResolutionCandidate, resultingDescriptor: CallableDescriptor): ResolutionCandidateStatus {
        val smartCasts = additionalDiagnosticReporter.createAdditionalDiagnostics(candidate, resultingDescriptor).takeIf { it.isNotEmpty() } ?:
                         return candidate.status
        return ResolutionCandidateStatus(candidate.status.diagnostics + smartCasts)
    }

    // true if we should complete this call
    private fun SimpleKotlinResolutionCandidate.prepareForCompletion(expectedType: UnwrappedType?): Boolean {
        val returnType = descriptorWithFreshTypes.returnType?.unwrap() ?: return false
        if (expectedType != null && !TypeUtils.noExpectedType(expectedType)) {
            csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPosition(kotlinCall))
        }

        return expectedType != null || csBuilder.isProperType(returnType)
    }

    private fun SimpleKotlinResolutionCandidate.competeCall(c: Context, resolutionCallbacks: KotlinResolutionCallbacks) {
        while (!oneStepToEndOrLambda(c, resolutionCallbacks)) {
            // do nothing -- be happy
        }
    }

    // true if it is the end (happy or not)
    // every step we fix type variable or analyzeLambda
    private fun SimpleKotlinResolutionCandidate.oneStepToEndOrLambda(c: Context, resolutionCallbacks: KotlinResolutionCallbacks): Boolean {
        val lambda = c.lambdaArguments.find { canWeAnalyzeIt(c, it) }
        if (lambda != null) {
            analyzeLambda(c, resolutionCallbacks, lambda)
            return false
        }

        val completionOrder = fixationOrderCalculator.computeCompletionOrder(c.asFixationOrderCalculatorContext(), descriptorWithFreshTypes.returnTypeOrNothing)
        return inferenceStepResolver.resolveVariables(c, completionOrder)
    }

    private fun analyzeLambda(c: Context, resolutionCallbacks: KotlinResolutionCallbacks, lambda: PostponedLambdaArgument) {
        val currentSubstitutor = c.buildCurrentSubstitutor()
        fun substitute(type: UnwrappedType) = currentSubstitutor.safeSubstitute(type)

        val receiver = lambda.receiver?.let(::substitute)
        val parameters = lambda.parameters.map(::substitute)
        val expectedType = lambda.returnType.takeIf { c.canBeProper(it) }?.let(::substitute)
        lambda.analyzed = true
        lambda.resultArguments = resolutionCallbacks.analyzeAndGetLambdaResultArguments(lambda.outerCall, lambda.argument, lambda.isSuspend, receiver, parameters, expectedType)

        for (resultLambdaArgument in lambda.resultArguments) {
            checkSimpleArgument(c.getBuilder(), resultLambdaArgument, lambda.returnType.let(::substitute))
        }

        if (lambda.resultArguments.isEmpty()) {
            val unitType = lambda.returnType.builtIns.unitType
            c.getBuilder().addSubtypeConstraint(lambda.returnType.let(::substitute), unitType, LambdaArgumentConstraintPosition(lambda))
        }
    }

    private fun canWeAnalyzeIt(c: Context, lambda: PostponedLambdaArgument): Boolean {
        if (lambda.analyzed) return false

        if (c.hasContradiction) return true // to record info about lambda and avoid exceptions

        lambda.receiver?.let {
            if (!c.canBeProper(it)) return false
        }
        return lambda.parameters.all { c.canBeProper(it) }
    }
}