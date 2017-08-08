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
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompleter.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.calls.inference.substituteAndApproximateCapturedTypes
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.typeUtil.contains

class KotlinCallCompleter(
        private val additionalDiagnosticReporter: AdditionalDiagnosticReporter,
        private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
        private val constraintSystemCompleter: ConstraintSystemCompleter
) {

    interface Context {
        val innerCalls: List<ResolvedKotlinCall.OnlyResolvedKotlinCall>
        fun buildResultingSubstitutor(): NewTypeSubstitutor
        val postponedArguments: List<PostponedKotlinCallArgument>
        val lambdaArguments: List<PostponedLambdaArgument>
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

        var completionType = topLevelCall.prepareForCompletion(expectedType)
        val lastCall = candidate.lastCall
        lastCall.runCompletion(completionType, resolutionCallbacks)

        if (lastCall.constraintSystem.asConstraintSystemCompleterContext().canBeProper(lastCall.descriptorWithFreshTypes.returnTypeOrNothing)) {
            completionType = ConstraintSystemCompletionMode.FULL
            lastCall.runCompletion(completionType, resolutionCallbacks)
        }

        return when (completionType) {
            ConstraintSystemCompletionMode.FULL -> toCompletedBaseResolvedCall(lastCall.constraintSystem.asCallCompleterContext(), candidate, resolutionCallbacks)
            ConstraintSystemCompletionMode.PARTIAL -> ResolvedKotlinCall.OnlyResolvedKotlinCall(candidate)
        }
    }

    private fun SimpleKotlinResolutionCandidate.runCompletion(completionMode: ConstraintSystemCompletionMode, resolutionCallbacks: KotlinResolutionCallbacks) {
        constraintSystemCompleter.runCompletion(
                constraintSystem.asConstraintSystemCompleterContext(), completionMode, descriptorWithFreshTypes.returnTypeOrNothing
        ) {
            postponedArgumentsAnalyzer.analyze(constraintSystem.asPostponedArgumentsAnalyzerContext(), resolutionCallbacks, it)
        }
    }

    private fun toCompletedBaseResolvedCall(
            c: Context,
            candidate: KotlinResolutionCandidate,
            resolutionCallbacks: KotlinResolutionCallbacks
    ): ResolvedKotlinCall.CompletedResolvedKotlinCall {
        val currentSubstitutor = c.buildResultingSubstitutor()
        val completedCall = candidate.toCompletedCall(currentSubstitutor, isTopLevel = true)
        val competedCalls = c.innerCalls.map {
            it.candidate.toCompletedCall(currentSubstitutor, isTopLevel = false)
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

    private fun KotlinResolutionCandidate.toCompletedCall(substitutor: NewTypeSubstitutor, isTopLevel: Boolean): CompletedKotlinCall {
        if (this is VariableAsFunctionKotlinResolutionCandidate) {
            val variable = resolvedVariable.toCompletedCall(substitutor, isTopLevel = false)
            val invoke = invokeCandidate.toCompletedCall(substitutor, isTopLevel)

            return CompletedKotlinCall.VariableAsFunction(kotlinCall, variable, invoke)
        }
        return (this as SimpleKotlinResolutionCandidate).toCompletedCall(substitutor, isTopLevel)
    }

    private fun SimpleKotlinResolutionCandidate.toCompletedCall(substitutor: NewTypeSubstitutor, isTopLevel: Boolean): CompletedKotlinCall.Simple {
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

        val diagnostics = computeDiagnostics(this, resultingDescriptor, isTopLevel)
        return CompletedKotlinCall.Simple(kotlinCall, candidateDescriptor, resultingDescriptor, diagnostics, explicitReceiverKind,
                                          dispatchReceiverArgument?.receiver, extensionReceiver?.receiver, typeArguments, argumentMappingByOriginal)
    }

    private fun computeDiagnostics(
            candidate: SimpleKotlinResolutionCandidate,
            resultingDescriptor: CallableDescriptor,
            isTopLevel: Boolean
    ): List<KotlinCallDiagnostic> {
        var diagnostics = candidate.getCandidateDiagnostics()
        if (isTopLevel) {
            diagnostics += candidate.constraintSystem.diagnostics
        }
        diagnostics += additionalDiagnosticReporter.createAdditionalDiagnostics(candidate, resultingDescriptor)
        return diagnostics
    }

    // true if we should complete this call
    private fun SimpleKotlinResolutionCandidate.prepareForCompletion(expectedType: UnwrappedType?): ConstraintSystemCompletionMode {
        val returnType = descriptorWithFreshTypes.returnType?.unwrap() ?: return ConstraintSystemCompletionMode.PARTIAL
        if (expectedType != null && !TypeUtils.noExpectedType(expectedType)) {
            csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPosition(kotlinCall))
        }

        return if (expectedType != null || csBuilder.isProperType(returnType)) {
            ConstraintSystemCompletionMode.FULL
        }
        else {
            ConstraintSystemCompletionMode.PARTIAL
        }
    }
}