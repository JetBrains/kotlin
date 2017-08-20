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
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.components.FixationOrderCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver
import org.jetbrains.kotlin.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.LambdaArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.calls.inference.substituteAndApproximateCapturedTypes
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateStatus
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull

class KotlinCallCompleter(
        val resultTypeResolver: ResultTypeResolver,
        val fixationOrderCalculator: FixationOrderCalculator
) {
    interface Context {
        val innerCalls: List<ResolvedKotlinCall.OnlyResolvedKotlinCall>
        val hasContradiction: Boolean
        fun buildCurrentSubstitutor(): NewTypeSubstitutor
        fun buildResultingSubstitutor(): NewTypeSubstitutor
        val lambdaArguments: List<ResolvedLambdaArgument>
        val callableReferenceArguments: List<ResolvedCallableReferenceArgument>
        val collectionLiteralArguments: List<ResolvedCollectionLiteralArgument>

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
            resolveCallableReferenceArguments(topLevelCall)

            val c = candidate.lastCall.constraintSystem.asCallCompleterContext()

            topLevelCall.competeCall(c, resolutionCallbacks)
            return toCompletedBaseResolvedCall(c, candidate, resolutionCallbacks)
        }
        else {
            // todo I'm not sure that we should do this
            resolveCallableReferenceArguments(topLevelCall)
        }

        return ResolvedKotlinCall.OnlyResolvedKotlinCall(candidate)
    }

    private fun resolveCallableReferenceArguments(candidate: SimpleKotlinResolutionCandidate) {
        for (callableReferenceArgument in candidate.postponeCallableReferenceArguments) {
            CheckArguments.processCallableReferenceArgument(candidate.callContext, candidate.kotlinCall, candidate.csBuilder,
                                                            callableReferenceArgument.argument, callableReferenceArgument.expectedType)
        }
        candidate.postponeCallableReferenceArguments.clear()
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
        c.lambdaArguments.forEach {
            it.finalReturnType = currentSubstitutor.safeSubstitute(it.returnType)
        }
        c.callableReferenceArguments.forEach {
            resolutionCallbacks.completeCallableReference(it, it.myTypeVariables.map { currentSubstitutor.safeSubstitute(it.defaultType) })
        }
        c.collectionLiteralArguments.forEach {
            resolutionCallbacks.completeCollectionLiteralCalls(it)
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
        val smartCasts = reportSmartCasts(candidate, resultingDescriptor).takeIf { it.isNotEmpty() } ?: return candidate.status
        return ResolutionCandidateStatus(candidate.status.diagnostics + smartCasts)
    }

    private fun createSmartCastDiagnostic(argument: KotlinCallArgument, expectedResultType: UnwrappedType): SmartCastDiagnostic? {
        if (argument !is ExpressionKotlinCallArgument) return null
        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(argument.receiver.receiverValue.type, expectedResultType)) {
            return SmartCastDiagnostic(argument, expectedResultType.unwrap())
        }
        return null
    }

    private fun reportSmartCastOnReceiver(
            candidate: KotlinResolutionCandidate,
            receiver: SimpleKotlinCallArgument?,
            parameter: ReceiverParameterDescriptor?
    ): SmartCastDiagnostic? {
        if (receiver == null || parameter == null) return null
        val expectedType = parameter.type.unwrap().let { if (receiver.isSafeCall) it.makeNullableAsSpecified(true) else it }

        val smartCastDiagnostic = createSmartCastDiagnostic(receiver, expectedType) ?: return null

        // todo may be we have smart cast to Int?
        return smartCastDiagnostic.takeIf {
            candidate.status.diagnostics.filterIsInstance<UnsafeCallError>().none {
                it.receiver == receiver
            }
            &&
            candidate.status.diagnostics.filterIsInstance<UnstableSmartCast>().none {
                it.expressionArgument == receiver
            }
        }
    }


    private fun reportSmartCasts(candidate: SimpleKotlinResolutionCandidate, resultingDescriptor: CallableDescriptor): List<KotlinCallDiagnostic> = SmartList<KotlinCallDiagnostic>().apply {
        addIfNotNull(reportSmartCastOnReceiver(candidate, candidate.extensionReceiver, resultingDescriptor.extensionReceiverParameter))
        addIfNotNull(reportSmartCastOnReceiver(candidate, candidate.dispatchReceiverArgument, resultingDescriptor.dispatchReceiverParameter))

        for (parameter in resultingDescriptor.valueParameters) {
            for (argument in candidate.argumentMappingByOriginal[parameter.original]?.arguments ?: continue) {
                val smartCastDiagnostic = createSmartCastDiagnostic(argument, argument.getExpectedType(parameter)) ?: continue

                val thereIsUnstableSmartCastError = candidate.status.diagnostics.filterIsInstance<UnstableSmartCast>().any {
                    it.expressionArgument == argument
                }

                if (!thereIsUnstableSmartCastError) {
                    add(smartCastDiagnostic)
                }
            }
        }
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
        if (c.hasContradiction) return true

        val lambda = c.lambdaArguments.find { canWeAnalyzeIt(c, it) }
        if (lambda != null) {
            analyzeLambda(c, resolutionCallbacks, callContext, kotlinCall, lambda)
            return false
        }

        val completionOrder = fixationOrderCalculator.computeCompletionOrder(c.asFixationOrderCalculatorContext(), descriptorWithFreshTypes.returnTypeOrNothing)
        for ((variableWithConstraints, direction) in completionOrder) {
            if (c.hasContradiction) return true
            val variable = variableWithConstraints.typeVariable

            val resultType = resultTypeResolver.findResultType(c.asResultTypeResolverContext(), variableWithConstraints, direction)
            if (resultType == null) {
                c.addError(NotEnoughInformationForTypeParameter(variable))
                break
            }
            c.fixVariable(variable, resultType)
            return false
        }
        return true
    }

    private fun analyzeLambda(c: Context, resolutionCallbacks: KotlinResolutionCallbacks, topLevelCallContext: KotlinCallContext, topLevelCall: KotlinCall, lambda: ResolvedLambdaArgument) {
        val currentSubstitutor = c.buildCurrentSubstitutor()
        fun substitute(type: UnwrappedType) = currentSubstitutor.safeSubstitute(type)

        val receiver = lambda.receiver?.let(::substitute)
        val parameters = lambda.parameters.map(::substitute)
        val expectedType = lambda.returnType.takeIf { c.canBeProper(it) }?.let(::substitute)
        lambda.analyzed = true
        lambda.resultArguments = resolutionCallbacks.analyzeAndGetLambdaResultArguments(lambda.outerCall, lambda.argument, lambda.isSuspend, receiver, parameters, expectedType)

        for (innerCall in lambda.resultArguments) {
            // todo strange code -- why top-level kotlinCall? may be it isn't right outer call
            CheckArguments.checkArgument(topLevelCallContext, topLevelCall, c.getBuilder(), innerCall, lambda.returnType.let(::substitute))
        }

        if (lambda.resultArguments.isEmpty()) {
            val unitType = lambda.returnType.builtIns.unitType
            c.getBuilder().addSubtypeConstraint(lambda.returnType.let(::substitute), unitType, LambdaArgumentConstraintPosition(lambda))
        }
    }

    private fun canWeAnalyzeIt(c: Context, lambda: ResolvedLambdaArgument): Boolean {
        if (lambda.analyzed) return false
        lambda.receiver?.let {
            if (!c.canBeProper(it)) return false
        }
        return lambda.parameters.all { c.canBeProper(it) }
    }
}

class SmartCastDiagnostic(val expressionArgument: ExpressionKotlinCallArgument, val smartCastType: UnwrappedType): KotlinCallDiagnostic(ResolutionCandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(expressionArgument, this)
}