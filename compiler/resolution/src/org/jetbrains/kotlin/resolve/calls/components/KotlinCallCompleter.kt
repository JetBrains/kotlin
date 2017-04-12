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
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.components.FixationOrderCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver
import org.jetbrains.kotlin.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.LambdaTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateStatus
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
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

    fun transformWhenAmbiguity(candidate: KotlinResolutionCandidate): ResolvedKotlinCall =
            toCompletedBaseResolvedCall(candidate.lastCall.constraintSystem.asCallCompleterContext(), candidate)

    fun completeCallIfNecessary(
            candidate: KotlinResolutionCandidate,
            expectedType: UnwrappedType?,
            lambdaAnalyzer: LambdaAnalyzer
    ): ResolvedKotlinCall {
        lambdaAnalyzer.bindStubResolvedCallForCandidate(candidate)
        val topLevelCall =
                if (candidate is VariableAsFunctionKotlinResolutionCandidate) {
                    candidate.invokeCandidate
                }
                else {
                    candidate as SimpleKotlinResolutionCandidate
                }

        if (topLevelCall.prepareForCompletion(expectedType)) {
            val c = candidate.lastCall.constraintSystem.asCallCompleterContext()

            topLevelCall.competeCall(c, lambdaAnalyzer)
            return toCompletedBaseResolvedCall(c, candidate)
        }

        return ResolvedKotlinCall.OnlyResolvedKotlinCall(candidate)
    }

    private fun toCompletedBaseResolvedCall(
            c: Context,
            candidate: KotlinResolutionCandidate
    ): ResolvedKotlinCall.CompletedResolvedKotlinCall {
        val currentSubstitutor = c.buildResultingSubstitutor()
        val completedCall = candidate.toCompletedCall(currentSubstitutor)
        val competedCalls = c.innerCalls.map {
            it.candidate.toCompletedCall(currentSubstitutor)
        }
        return ResolvedKotlinCall.CompletedResolvedKotlinCall(completedCall, competedCalls)
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
        val resultingDescriptor = if (descriptorWithFreshTypes.typeParameters.isNotEmpty()) descriptorWithFreshTypes.substitute(substitutor)!! else descriptorWithFreshTypes

        val typeArguments = descriptorWithFreshTypes.typeParameters.map { substitutor.safeSubstitute(it.defaultType) }

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

    private fun SimpleKotlinResolutionCandidate.competeCall(c: Context, lambdaAnalyzer: LambdaAnalyzer) {
        while (!oneStepToEndOrLambda(c, lambdaAnalyzer)) {
            // do nothing -- be happy
        }
    }

    // true if it is the end (happy or not)
    private fun SimpleKotlinResolutionCandidate.oneStepToEndOrLambda(c: Context, lambdaAnalyzer: LambdaAnalyzer): Boolean {
        if (c.hasContradiction) return true

        val lambda = c.lambdaArguments.find { canWeAnalyzeIt(c, it) }
        if (lambda != null) {
            analyzeLambda(c, lambdaAnalyzer, callContext, kotlinCall, lambda)
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

            if (variable is LambdaTypeVariable) {
                val resolvedLambda = c.lambdaArguments.find { it.argument == variable.lambdaArgument } ?: return true
                if (canWeAnalyzeIt(c, resolvedLambda)) {
                    analyzeLambda(c, lambdaAnalyzer, callContext, kotlinCall, resolvedLambda)
                    return false
                }
            }
        }
        return true
    }

    private fun analyzeLambda(c: Context, lambdaAnalyzer: LambdaAnalyzer, topLevelCallContext: KotlinCallContext, topLevelCall: KotlinCall, lambda: ResolvedLambdaArgument) {
        val currentSubstitutor = c.buildCurrentSubstitutor()
        fun substitute(type: UnwrappedType) = currentSubstitutor.safeSubstitute(type)

        val receiver = lambda.receiver?.let(::substitute)
        val parameters = lambda.parameters.map(::substitute)
        val expectedType = lambda.returnType.takeIf { c.canBeProper(it) }?.let(::substitute)
        val callsFromLambda = lambdaAnalyzer.analyzeAndGetRelatedCalls(topLevelCall, lambda.argument, receiver, parameters, expectedType)
        lambda.analyzed = true

        for (innerCall in callsFromLambda) {
            // todo strange code -- why top-level kotlinCall? may be it isn't right outer call
            CheckArguments.checkArgument(topLevelCallContext, topLevelCall, c.getBuilder(), innerCall, lambda.returnType)
        }
//            when (innerCall) {
//                is ResolvedKotlinCall.CompletedResolvedKotlinCall -> {
//                    val returnType = innerCall.completedCall.lastCall.resultingDescriptor.returnTypeOrNothing
//                    constraintInjector.addInitialSubtypeConstraint(injectorContext, returnType, lambda.returnType, position)
//                }
//                is ResolvedKotlinCall.OnlyResolvedKotlinCall -> {
//                    // todo register call
//                    val returnType = innerCall.candidate.lastCall.descriptorWithFreshTypes.returnTypeOrNothing
//                    c.addInnerCall(innerCall)
//                    constraintInjector.addInitialSubtypeConstraint(injectorContext, returnType, lambda.returnType, position)
//                }
//            }
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