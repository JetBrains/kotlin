/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
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
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallableDescriptors
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyForInvoke
import org.jetbrains.kotlin.resolve.isAnnotatedAsHidden
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.sure

class NewResolveOldInference(
        val candidateResolver: CandidateResolver,
        val towerResolver: TowerResolver,
        val resolutionResultsHandler: ResolutionResultsHandler,
        val dynamicCallableDescriptors: DynamicCallableDescriptors
) {

    fun runResolve(
            context: BasicCallResolutionContext,
            name: Name,
            kind: CallResolver.ResolveKind,
            tracing: TracingStrategy
    ): OverloadResolutionResultsImpl<*> {
        val explicitReceiver = context.call.explicitReceiver.check { it.exists() }

        val dynamicScope = dynamicCallableDescriptors.createDynamicDescriptorScope(context.call, context.scope.ownerDescriptor)
        val scopeTower = ScopeTowerImpl(context, dynamicScope, explicitReceiver, context.call.createLookupLocation())

        val baseContext = Context(scopeTower, name, context, tracing)

        val processor =
            when (kind) {
                CallResolver.ResolveKind.VARIABLE -> createVariableProcessor(baseContext, explicitReceiver)
                CallResolver.ResolveKind.FUNCTION -> createFunctionTowerProcessor(baseContext, explicitReceiver)
                CallResolver.ResolveKind.CALLABLE_REFERENCE -> CompositeScopeTowerProcessor(
                        createFunctionTowerProcessor(baseContext, explicitReceiver),
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

        if (context.collectAllCandidates) {
            val allCandidates = towerResolver.collectAllCandidates(baseContext, processor)
            val result = OverloadResolutionResultsImpl.nameNotFound<CallableDescriptor>()
            result.allCandidates = allCandidates.map { it.resolvedCall as MutableResolvedCall<CallableDescriptor> }
            return result
        }

        val candidates = towerResolver.runResolve(baseContext, processor, useOrder = kind != CallResolver.ResolveKind.CALLABLE_REFERENCE)
        return convertToOverloadResults(candidates, tracing, context)
    }

    private fun createProcessorWithReceiverValueOrEmpty(
            explicitReceiver: Receiver?,
            create: (ReceiverValue?) -> ScopeTowerProcessor<Candidate>
    ): ScopeTowerProcessor<Candidate> {
        return if (explicitReceiver is Qualifier) {
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
                    towerCandidate.dispatchReceiver ?: ReceiverValue.NO_RECEIVER, extensionReceiver ?: ReceiverValue.NO_RECEIVER,
                    explicitReceiverKind, null, candidateTrace, tracing,
                    basicCallContext.dataFlowInfoForArguments // todo may be we should create new mutable info for arguments
            )

            // see spec-docs/dynamic-types.md
            if (extensionReceiver != null && extensionReceiver.type.isDynamic()
                && !towerCandidate.descriptor.extensionReceiverParameter!!.value.type.isDynamic()) {
                return Candidate(ResolutionCandidateStatus(listOf(ExtensionWithStaticTypeWithDynamicReceiver)), candidateCall)
            }

            if (towerCandidate.descriptor.isAnnotatedAsHidden()) {
                return Candidate(ResolutionCandidateStatus(listOf(HiddenDescriptor)), candidateCall)
            }

            val callCandidateResolutionContext = CallCandidateResolutionContext.create(
                    candidateCall, basicCallContext, candidateTrace, tracing, basicCallContext.call,
                    ReceiverValue.NO_RECEIVER, CandidateResolveMode.FULLY // todo
            )
            candidateResolver.performResolutionForCandidateCall(callCandidateResolutionContext, basicCallContext.checkArguments) // todo

            val diagnostics = (towerCandidate.diagnostics + createPreviousResolveError(candidateCall.status)).filterNotNull() // todo
            return Candidate(ResolutionCandidateStatus(diagnostics), candidateCall)
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
            val basicCallResolutionContext = basicCallContext.replaceCall(CallTransformer.stripCallArguments(basicCallContext.call))
            return Context(scopeTower, name, basicCallResolutionContext, tracing)
        }

        override fun contextForInvoke(variable: Candidate, useExplicitReceiver: Boolean): Pair<ReceiverValue, TowerContext<Candidate>> {
            assert(variable.resolvedCall.status.possibleTransformToSuccess()) {
                "Incorrect status: ${variable.resolvedCall.status} for variable call: ${variable.resolvedCall} " +
                "and descriptor: ${variable.resolvedCall.candidateDescriptor}"
            }
            val calleeExpression = variable.resolvedCall.call.calleeExpression
            val variableDescriptor = variable.resolvedCall.resultingDescriptor
            assert(variable.resolvedCall.status.possibleTransformToSuccess() && calleeExpression != null && variableDescriptor is VariableDescriptor) {
                "Unexpected varialbe candidate: $variable"
            }
            val variableReceiver = ExpressionReceiver.create(calleeExpression!!,
                                                             (variableDescriptor as VariableDescriptor).type,
                                                             basicCallContext.trace.bindingContext)
            // used for smartCasts, see: DataFlowValueFactory.getIdForSimpleNameExpression
            tracing.bindReference(variable.resolvedCall.trace, variable.resolvedCall)
            // todo hacks
            val functionCall = CallTransformer.CallForImplicitInvoke(
                    basicCallContext.call.explicitReceiver.check { useExplicitReceiver } ?: ReceiverValue.NO_RECEIVER,
                    variableReceiver, basicCallContext.call)
            val tracingForInvoke = TracingStrategyForInvoke(calleeExpression, functionCall, variableReceiver.type)
            val basicCallResolutionContext = basicCallContext.replaceBindingTrace(variable.resolvedCall.trace)
                    .replaceContextDependency(ContextDependency.DEPENDENT) // todo
            val newContext = Context(scopeTower, OperatorNameConventions.INVOKE, basicCallResolutionContext, tracingForInvoke)

            return variableReceiver to newContext
        }

    }

}