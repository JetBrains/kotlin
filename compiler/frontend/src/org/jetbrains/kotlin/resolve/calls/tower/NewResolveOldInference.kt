/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getUnaryPlusOrMinusOperatorFunctionName
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isConventionCall
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isInfixCall
import org.jetbrains.kotlin.resolve.calls.callUtil.createLookupLocation
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CandidateResolveMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl
import org.jetbrains.kotlin.resolve.calls.results.ResolutionResultsHandler
import org.jetbrains.kotlin.resolve.calls.tasks.*
import org.jetbrains.kotlin.resolve.isHiddenInResolution
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.sure

class NewResolveOldInference(
        private val candidateResolver: CandidateResolver,
        private val towerResolver: TowerResolver,
        private val resolutionResultsHandler: ResolutionResultsHandler,
        private val dynamicCallableDescriptors: DynamicCallableDescriptors,
        private val syntheticScopes: SyntheticScopes
) {

    fun runResolve(
            context: BasicCallResolutionContext,
            name: Name,
            kind: CallResolver.ResolveKind,
            tracing: TracingStrategy
    ): OverloadResolutionResultsImpl<*> {
        val explicitReceiver = context.call.explicitReceiver

        val dynamicScope = dynamicCallableDescriptors.createDynamicDescriptorScope(context.call, context.scope.ownerDescriptor)
        val scopeTower = ScopeTowerImpl(context, dynamicScope, syntheticScopes, context.call.createLookupLocation())

        val baseContext = Context(scopeTower, name, context, tracing)

        var processor = createResolveProcessor(kind, explicitReceiver, context, baseContext)

        if (context.collectAllCandidates) {
            return allCandidatesResult(towerResolver.collectAllCandidates(baseContext, processor))
        }
        // Temporary fix for code migration (unaryPlus()/unaryMinus())
        val unaryConventionName = getUnaryPlusOrMinusOperatorFunctionName(context.call)
        if (unaryConventionName != null) {
            val deprecatedName = if (name == OperatorNameConventions.UNARY_PLUS)
                OperatorNameConventions.PLUS
            else
                OperatorNameConventions.MINUS
            val otherBaseContext = Context(scopeTower, deprecatedName, context, tracing)
            processor = CompositeScopeTowerProcessor(processor, createResolveProcessor(kind, explicitReceiver, context, otherBaseContext))
        }

        val candidates = towerResolver.runResolve(baseContext, processor, useOrder = kind != CallResolver.ResolveKind.CALLABLE_REFERENCE)
        return convertToOverloadResults(candidates, tracing, context)
    }

    fun <D : CallableDescriptor> runResolveForGivenCandidates(
            basicCallContext: BasicCallResolutionContext,
            tracing: TracingStrategy,
            candidates: Collection<ResolutionCandidate<D>>
    ): OverloadResolutionResultsImpl<D> {
        val resolvedCandidates = candidates.mapNotNull { candidate ->
            val candidateTrace = TemporaryBindingTrace.create(basicCallContext.trace, "Context for resolve candidate")
            val resolvedCall = ResolvedCallImpl.create(candidate, candidateTrace, tracing, basicCallContext.dataFlowInfoForArguments)

            if (candidate.descriptor.isHiddenInResolution()) {
                return@mapNotNull Candidate(ResolutionCandidateStatus(listOf(HiddenDescriptor)), resolvedCall)
            }

            val callCandidateResolutionContext = CallCandidateResolutionContext.create(
                    resolvedCall, basicCallContext, candidateTrace, tracing, basicCallContext.call,
                    CandidateResolveMode.FULLY // todo
            )
            candidateResolver.performResolutionForCandidateCall(callCandidateResolutionContext, basicCallContext.checkArguments) // todo

            val diagnostics = listOfNotNull(SynthesizedDescriptorDiagnostic.check { candidate.descriptor.isSynthesized },
                                            createPreviousResolveError(resolvedCall.status))
            Candidate(ResolutionCandidateStatus(diagnostics), resolvedCall)
        }
        if (basicCallContext.collectAllCandidates) {
            val allCandidates = towerResolver.run(listOf(TowerData.Empty), KnownResultProcessor(resolvedCandidates),
                                                  TowerResolver.AllCandidatesCollector { it.candidateStatus }, useOrder = false)
            return allCandidatesResult(allCandidates) as OverloadResolutionResultsImpl<D>
        }

        val processedCandidates = towerResolver.run(listOf(TowerData.Empty), KnownResultProcessor(resolvedCandidates),
                                                    TowerResolver.SuccessfulResultCollector { it.candidateStatus }, useOrder = true)

        return convertToOverloadResults(processedCandidates, tracing, basicCallContext) as OverloadResolutionResultsImpl<D>
    }

    private fun allCandidatesResult(allCandidates: Collection<Candidate>)
            = OverloadResolutionResultsImpl.nameNotFound<CallableDescriptor>().apply {
        this.allCandidates = allCandidates.map { it.resolvedCall as MutableResolvedCall<CallableDescriptor> }
    }

    private fun createResolveProcessor(
            kind: CallResolver.ResolveKind,
            explicitReceiver : Receiver?,
            context: BasicCallResolutionContext,
            baseContext: Context)
            = when (kind) {
        CallResolver.ResolveKind.VARIABLE -> createVariableProcessor(baseContext, explicitReceiver)
        CallResolver.ResolveKind.FUNCTION -> createFunctionTowerProcessor(baseContext, explicitReceiver)
        CallResolver.ResolveKind.CALLABLE_REFERENCE -> CompositeScopeTowerProcessor(
                createFunctionProcessor(baseContext, explicitReceiver),
                createVariableProcessor(baseContext, explicitReceiver)
        )
        CallResolver.ResolveKind.INVOKE -> {
            // todo
            val call = (context.call as? CallTransformer.CallForImplicitInvoke).sure {
                "Call should be CallForImplicitInvoke, but it is: ${context.call}"
            }
            createProcessorWithReceiverValueOrEmpty(explicitReceiver) {
                createCallTowerProcessorForExplicitInvoke(baseContext, call.dispatchReceiver, it)
            }
        }
        CallResolver.ResolveKind.GIVEN_CANDIDATES -> {
            throw UnsupportedOperationException("Kind $kind unsupported yet")
        }
    }

    private fun createProcessorWithReceiverValueOrEmpty(
            explicitReceiver: Receiver?,
            create: (ReceiverValue?) -> ScopeTowerProcessor<Candidate>
    ): ScopeTowerProcessor<Candidate> {
        return if (explicitReceiver is QualifierReceiver) {
            (explicitReceiver as? ClassQualifier)?.classValueReceiver?.let(create)
            ?: KnownResultProcessor<Candidate>(listOf())
        }
        else {
            create(explicitReceiver as ReceiverValue?)
        }
    }

    private fun createFunctionTowerProcessor(baseContext: Context, explicitReceiver: Receiver?): CompositeScopeTowerProcessor<Candidate> {
        // a.foo() -- simple function call
        val simpleFunction = createFunctionProcessor(baseContext, explicitReceiver)

        // a.foo() -- property a.foo + foo.invoke()
        val invokeProcessor = InvokeTowerProcessor(baseContext, explicitReceiver)

        // a.foo() -- property foo is extension function with receiver a -- a.invoke()
        val invokeExtensionProcessor = createProcessorWithReceiverValueOrEmpty(explicitReceiver) { InvokeExtensionTowerProcessor(baseContext, it) }

        return CompositeScopeTowerProcessor(simpleFunction, invokeProcessor, invokeExtensionProcessor)
    }


    private fun convertToOverloadResults(
            candidates: Collection<Candidate>,
            tracing: TracingStrategy,
            basicCallContext: BasicCallResolutionContext
    ): OverloadResolutionResultsImpl<*> {
        val resolvedCalls = candidates.mapNotNull {
            val (status, resolvedCall) = it
            if (resolvedCall is VariableAsFunctionResolvedCallImpl) {
                // todo hacks
                tracing.bindReference(resolvedCall.variableCall.trace, resolvedCall.variableCall)
                tracing.bindResolvedCall(resolvedCall.variableCall.trace, resolvedCall)

                resolvedCall.variableCall.trace.addOwnDataTo(resolvedCall.functionCall.trace)

                resolvedCall.functionCall.tracingStrategy.bindReference(resolvedCall.functionCall.trace, resolvedCall.functionCall)
                //                resolvedCall.hackInvokeTracing.bindResolvedCall(resolvedCall.functionCall.trace, resolvedCall)
            } else {
                tracing.bindReference(resolvedCall.trace, resolvedCall)
                tracing.bindResolvedCall(resolvedCall.trace, resolvedCall)
            }

            if (resolvedCall.status.possibleTransformToSuccess()) {
                for (error in status.diagnostics) {
                    if (error is UnsupportedInnerClassCall) {
                        resolvedCall.trace.report(Errors.UNSUPPORTED.on(resolvedCall.call.callElement, error.message))
                    }
                    else if (error is NestedClassViaInstanceReference) {
                        tracing.nestedClassAccessViaInstanceReference(resolvedCall.trace, error.classDescriptor, resolvedCall.explicitReceiverKind)
                    }
                    else if (error is ErrorDescriptorDiagnostic) {
                        // todo
                        //  return@map null
                    }
                }
            }

            resolvedCall
        }

        return resolutionResultsHandler.computeResultAndReportErrors<CallableDescriptor>(basicCallContext, tracing, resolvedCalls as List<MutableResolvedCall<CallableDescriptor>>)
    }

    private data class Candidate(val candidateStatus: ResolutionCandidateStatus, val resolvedCall: MutableResolvedCall<*>)

    private inner class Context(
            override val scopeTower: ScopeTower,
            override val name: Name,
            private val basicCallContext: BasicCallResolutionContext,
            private val tracing: TracingStrategy
    ) : TowerContext<Candidate> {

        override fun createCandidate(
                towerCandidate: CandidateWithBoundDispatchReceiver<*>,
                explicitReceiverKind: ExplicitReceiverKind,
                extensionReceiver: ReceiverValue?
        ): Candidate {
            val candidateTrace = TemporaryBindingTrace.create(basicCallContext.trace, "Context for resolve candidate")
            val candidateCall = ResolvedCallImpl(
                    basicCallContext.call, towerCandidate.descriptor,
                    towerCandidate.dispatchReceiver, extensionReceiver,
                    explicitReceiverKind, null, candidateTrace, tracing,
                    basicCallContext.dataFlowInfoForArguments // todo may be we should create new mutable info for arguments
            )

            // see spec-docs/dynamic-types.md
            if (extensionReceiver != null && extensionReceiver.type.isDynamic()
                && !towerCandidate.descriptor.extensionReceiverParameter!!.value.type.isDynamic()) {
                return Candidate(ResolutionCandidateStatus(listOf(ExtensionWithStaticTypeWithDynamicReceiver)), candidateCall)
            }

            if (towerCandidate.descriptor.isHiddenInResolution()) {
                return Candidate(ResolutionCandidateStatus(listOf(HiddenDescriptor)), candidateCall)
            }

            val callCandidateResolutionContext = CallCandidateResolutionContext.create(
                    candidateCall, basicCallContext, candidateTrace, tracing, basicCallContext.call,
                    CandidateResolveMode.FULLY // todo
            )
            candidateResolver.performResolutionForCandidateCall(callCandidateResolutionContext, basicCallContext.checkArguments) // todo

            val diagnostics = (towerCandidate.diagnostics +
                               checkInfixAndOperator(basicCallContext.call, towerCandidate.descriptor) +
                               createPreviousResolveError(candidateCall.status)).filterNotNull() // todo
            return Candidate(ResolutionCandidateStatus(diagnostics), candidateCall)
        }

        private fun checkInfixAndOperator(call: Call, descriptor: CallableDescriptor): List<ResolutionDiagnostic> {
            if (descriptor !is FunctionDescriptor || ErrorUtils.isError(descriptor)) return emptyList()
            if (descriptor.name != name && (name == OperatorNameConventions.UNARY_PLUS || name == OperatorNameConventions.UNARY_MINUS)) {
                return listOf(DeprecatedUnaryPlusAsPlus)
            }

            val conventionError = if (isConventionCall(call) && !descriptor.isOperator) InvokeConventionCallNoOperatorModifier else null
            val infixError = if (isInfixCall(call) && !descriptor.isInfix) InfixCallNoInfixModifier else null
            return listOfNotNull(conventionError, infixError)
        }

        override fun getStatus(candidate: Candidate): ResolutionCandidateStatus = candidate.candidateStatus

        override fun transformCandidate(variable: Candidate, invoke: Candidate): Candidate {
            val resolvedCallImpl = VariableAsFunctionResolvedCallImpl(
                    invoke.resolvedCall as MutableResolvedCall<FunctionDescriptor>,
                    variable.resolvedCall as MutableResolvedCall<VariableDescriptor>
            )
            assert(variable.candidateStatus.resultingApplicability.isSuccess) {
                "Variable call must be success: $variable"
            }

            return Candidate(ResolutionCandidateStatus(variable.candidateStatus.diagnostics + invoke.candidateStatus.diagnostics), resolvedCallImpl)
        }

        override fun contextForVariable(stripExplicitReceiver: Boolean): TowerContext<Candidate> {
            val newCall = CallTransformer.stripCallArguments(basicCallContext.call).let {
                if (stripExplicitReceiver) CallTransformer.stripReceiver(it) else it
            }
            return Context(scopeTower, name, basicCallContext.replaceCall(newCall), tracing)
        }

        override fun contextForInvoke(variable: Candidate, useExplicitReceiver: Boolean): Pair<ReceiverValue, TowerContext<Candidate>>? {
            assert(variable.resolvedCall.status.possibleTransformToSuccess()) {
                "Incorrect status: ${variable.resolvedCall.status} for variable call: ${variable.resolvedCall} " +
                "and descriptor: ${variable.resolvedCall.candidateDescriptor}"
            }
            val calleeExpression = variable.resolvedCall.call.calleeExpression
            val variableDescriptor = variable.resolvedCall.resultingDescriptor
            assert(variable.resolvedCall.status.possibleTransformToSuccess() && calleeExpression != null && variableDescriptor is VariableDescriptor) {
                "Unexpected varialbe candidate: $variable"
            }
            val variableType = (variableDescriptor as VariableDescriptor).type

            if (variableType is DeferredType && variableType.isComputing) {
                return null // todo: create special check that there is no invoke on variable
            }
            val variableReceiver = ExpressionReceiver.create(calleeExpression!!,
                                                             variableType,
                                                             basicCallContext.trace.bindingContext)
            // used for smartCasts, see: DataFlowValueFactory.getIdForSimpleNameExpression
            tracing.bindReference(variable.resolvedCall.trace, variable.resolvedCall)
            // todo hacks
            val functionCall = CallTransformer.CallForImplicitInvoke(
                    basicCallContext.call.explicitReceiver?.check { useExplicitReceiver },
                    variableReceiver, basicCallContext.call)
            val tracingForInvoke = TracingStrategyForInvoke(calleeExpression, functionCall, variableReceiver.type)
            val basicCallResolutionContext = basicCallContext.replaceBindingTrace(variable.resolvedCall.trace)
                    .replaceCall(functionCall)
                    .replaceContextDependency(ContextDependency.DEPENDENT) // todo

            val newScopeTower = ScopeTowerImpl(basicCallResolutionContext, scopeTower.dynamicScope, scopeTower.syntheticScopes, scopeTower.location)
            val newContext = Context(newScopeTower, OperatorNameConventions.INVOKE, basicCallResolutionContext, tracingForInvoke)

            return variableReceiver to newContext
        }

    }

}