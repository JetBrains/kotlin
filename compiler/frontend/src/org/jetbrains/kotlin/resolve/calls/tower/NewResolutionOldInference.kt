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

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isConventionCall
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isInfixCall
import org.jetbrains.kotlin.resolve.calls.callUtil.createLookupLocation
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl
import org.jetbrains.kotlin.resolve.calls.results.ResolutionResultsHandler
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.*
import org.jetbrains.kotlin.resolve.isHiddenInResolution
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticConstructorsProvider
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.sure
import java.lang.IllegalStateException
import java.util.*

class NewResolutionOldInference(
        private val candidateResolver: CandidateResolver,
        private val towerResolver: TowerResolver,
        private val resolutionResultsHandler: ResolutionResultsHandler,
        private val dynamicCallableDescriptors: DynamicCallableDescriptors,
        private val syntheticScopes: SyntheticScopes,
        private val syntheticConstructorsProvider: SyntheticConstructorsProvider,
        private val languageVersionSettings: LanguageVersionSettings
) {
    sealed class ResolutionKind<D : CallableDescriptor> {
        abstract internal fun createTowerProcessor(
                outer: NewResolutionOldInference,
                name: Name,
                tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower,
                explicitReceiver: DetailedReceiver?,
                context: BasicCallResolutionContext
        ): ScopeTowerProcessor<MyCandidate<D>>

        object Function : ResolutionKind<FunctionDescriptor>() {
            override fun createTowerProcessor(
                    outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
                    scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate<FunctionDescriptor>> {
                val functionFactory = outer.CandidateFactoryImpl<FunctionDescriptor>(name, context, tracing)
                return createFunctionProcessor(scopeTower, name, functionFactory, outer.CandidateFactoryProviderForInvokeImpl(functionFactory), explicitReceiver)
            }
        }

        object Variable : ResolutionKind<VariableDescriptor>() {
            override fun createTowerProcessor(
                    outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
                    scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate<VariableDescriptor>> {
                val variableFactory = outer.CandidateFactoryImpl<VariableDescriptor>(name, context, tracing)
                return createVariableAndObjectProcessor(scopeTower, name, variableFactory, explicitReceiver)
            }
        }

        object CallableReference : ResolutionKind<CallableDescriptor>() {
            override fun createTowerProcessor(
                    outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
                    scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate<CallableDescriptor>> {
                val functionFactory = outer.CandidateFactoryImpl<FunctionDescriptor>(name, context, tracing)
                val variableFactory = outer.CandidateFactoryImpl<VariableDescriptor>(name, context, tracing)
                return CompositeScopeTowerProcessor(
                        createSimpleFunctionProcessor(scopeTower, name, functionFactory, explicitReceiver, classValueReceiver = false),
                        createVariableProcessor(scopeTower, name, variableFactory, explicitReceiver, classValueReceiver = false)
                )
            }
        }

        object Invoke : ResolutionKind<FunctionDescriptor>() {
            override fun createTowerProcessor(
                    outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
                    scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate<FunctionDescriptor>> {
                val functionFactory = outer.CandidateFactoryImpl<FunctionDescriptor>(name, context, tracing)
                // todo
                val call = (context.call as? CallTransformer.CallForImplicitInvoke).sure {
                    "Call should be CallForImplicitInvoke, but it is: ${context.call}"
                }
                return createProcessorWithReceiverValueOrEmpty(explicitReceiver) {
                    createCallTowerProcessorForExplicitInvoke(scopeTower, functionFactory, context.transformToReceiverWithSmartCastInfo(call.dispatchReceiver), it)
                }
            }

        }

        class GivenCandidates<D : CallableDescriptor> : ResolutionKind<D>() {
            override fun createTowerProcessor(
                    outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
                    scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate<D>> {
                throw IllegalStateException("Should be not called")
            }
        }
    }

    fun <D : CallableDescriptor> runResolution(
            context: BasicCallResolutionContext,
            name: Name,
            kind: ResolutionKind<D>,
            tracing: TracingStrategy
    ): OverloadResolutionResultsImpl<D> {
        val explicitReceiver = context.call.explicitReceiver
        val detailedReceiver = if (explicitReceiver is QualifierReceiver?) {
            explicitReceiver
        }
        else {
            context.transformToReceiverWithSmartCastInfo(explicitReceiver as ReceiverValue)
        }

        val dynamicScope = dynamicCallableDescriptors.createDynamicDescriptorScope(context.call, context.scope.ownerDescriptor)
        val scopeTower = ImplicitScopeTowerImpl(context, dynamicScope, syntheticScopes, syntheticConstructorsProvider, context.call.createLookupLocation())

        val processor = kind.createTowerProcessor(this, name, tracing, scopeTower, detailedReceiver, context)

        if (context.collectAllCandidates) {
            return allCandidatesResult(towerResolver.collectAllCandidates(scopeTower, processor))
        }

        val candidates = towerResolver.runResolve(scopeTower, processor, useOrder = kind != ResolutionKind.CallableReference)

        if (candidates.isEmpty()) {
            if (reportAdditionalDiagnosticIfNoCandidates(context, name, kind, scopeTower, detailedReceiver)) {
                return OverloadResolutionResultsImpl.nameNotFound()
            }
        }

        return convertToOverloadResults(candidates, tracing, context)
    }

    fun <D : CallableDescriptor> runResolutionForGivenCandidates(
            basicCallContext: BasicCallResolutionContext,
            tracing: TracingStrategy,
            candidates: Collection<ResolutionCandidate<D>>
    ): OverloadResolutionResultsImpl<D> {
        val resolvedCandidates = candidates.mapNotNull { candidate ->
            val candidateTrace = TemporaryBindingTrace.create(basicCallContext.trace, "Context for resolve candidate")
            val resolvedCall = ResolvedCallImpl.create(candidate, candidateTrace, tracing, basicCallContext.dataFlowInfoForArguments)

            if (candidate.descriptor.isHiddenInResolution(languageVersionSettings, basicCallContext.isSuperCall)) {
                return@mapNotNull MyCandidate(ResolutionCandidateStatus(listOf(HiddenDescriptor)), resolvedCall)
            }

            val callCandidateResolutionContext = CallCandidateResolutionContext.create(
                    resolvedCall, basicCallContext, candidateTrace, tracing, basicCallContext.call,
                    CandidateResolveMode.FULLY // todo
            )
            candidateResolver.performResolutionForCandidateCall(callCandidateResolutionContext, basicCallContext.checkArguments) // todo

            val diagnostics = listOfNotNull(SynthesizedDescriptorDiagnostic.check { candidate.descriptor.isSynthesized },
                                            createPreviousResolveError(resolvedCall.status))
            MyCandidate(ResolutionCandidateStatus(diagnostics), resolvedCall)
        }
        if (basicCallContext.collectAllCandidates) {
            val allCandidates = towerResolver.runWithEmptyTowerData(KnownResultProcessor(resolvedCandidates),
                                                  TowerResolver.AllCandidatesCollector { it.candidateStatus }, useOrder = false)
            return allCandidatesResult(allCandidates)
        }

        val processedCandidates = towerResolver.runWithEmptyTowerData(KnownResultProcessor(resolvedCandidates),
                                                    TowerResolver.SuccessfulResultCollector { it.candidateStatus }, useOrder = true)

        return convertToOverloadResults(processedCandidates, tracing, basicCallContext)
    }

    private fun <D: CallableDescriptor> allCandidatesResult(allCandidates: Collection<MyCandidate<D>>)
            = OverloadResolutionResultsImpl.nameNotFound<D>().apply {
        this.allCandidates = allCandidates.map { it.resolvedCall }
    }

    private fun <D : CallableDescriptor> convertToOverloadResults(
            candidates: Collection<MyCandidate<D>>,
            tracing: TracingStrategy,
            basicCallContext: BasicCallResolutionContext
    ): OverloadResolutionResultsImpl<D> {
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

        return resolutionResultsHandler.computeResultAndReportErrors(basicCallContext, tracing, resolvedCalls)
    }

    // true if we found something
    private fun reportAdditionalDiagnosticIfNoCandidates(
            context: BasicCallResolutionContext,
            name: Name,
            kind: ResolutionKind<*>,
            scopeTower: ImplicitScopeTower,
            detailedReceiver: DetailedReceiver?
    ): Boolean {
        val reference = context.call.calleeExpression as? KtReferenceExpression ?: return false

        val errorCadidates = when (kind) {
            ResolutionKind.Function -> collectErrorCandidatesForFunction(scopeTower, name, detailedReceiver)
            ResolutionKind.Variable -> collectErrorCandidatesForVariable(scopeTower, name, detailedReceiver)
            else -> emptyList()
        }

        for (candidate in errorCadidates) {
            if (candidate is ErrorCandidate.Classifier) {
                context.trace.record(BindingContext.REFERENCE_TARGET, reference, candidate.descriptor)
                context.trace.report(Errors.RESOLUTION_TO_CLASSIFIER.on(reference, candidate.descriptor, candidate.kind, candidate.errorMessage))
                return true
            }
        }
        return false
    }

    private class ImplicitScopeTowerImpl(
            val resolutionContext: ResolutionContext<*>,
            override val dynamicScope: MemberScope,
            override val syntheticScopes: SyntheticScopes,
            override val syntheticConstructorsProvider: SyntheticConstructorsProvider,
            override val location: LookupLocation
    ): ImplicitScopeTower {
        private val cache = HashMap<ReceiverValue, ReceiverValueWithSmartCastInfo>()

        override fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo? =
                scope.implicitReceiver?.value?.let {
                    cache.getOrPut(it) { resolutionContext.transformToReceiverWithSmartCastInfo(it) }
                }

        override val lexicalScope: LexicalScope get() = resolutionContext.scope

        override val isDebuggerContext: Boolean get() = resolutionContext.isDebuggerContext
    }

    internal data class MyCandidate<out D: CallableDescriptor>(
            val candidateStatus: ResolutionCandidateStatus,
            val resolvedCall: MutableResolvedCall<@UnsafeVariance D>
    ) : Candidate<D> {
        override val descriptor: D
            get() = resolvedCall.candidateDescriptor

        override val isSuccessful: Boolean
            get() = candidateStatus.resultingApplicability.isSuccess
        override val status: ResolutionCandidateStatus
            get() = candidateStatus
    }

    private inner class CandidateFactoryImpl<D : CallableDescriptor>(
            val name: Name,
            val basicCallContext: BasicCallResolutionContext,
            val tracing: TracingStrategy
    ) : CandidateFactory<D, MyCandidate<D>> {
        override fun createCandidate(
                towerCandidate: CandidateWithBoundDispatchReceiver<D>,
                explicitReceiverKind: ExplicitReceiverKind,
                extensionReceiver: ReceiverValueWithSmartCastInfo?
        ): MyCandidate<D> {

            val candidateTrace = TemporaryBindingTrace.create(basicCallContext.trace, "Context for resolve candidate")
            val candidateCall = ResolvedCallImpl(
                    basicCallContext.call, towerCandidate.descriptor,
                    towerCandidate.dispatchReceiver?.receiverValue, extensionReceiver?.receiverValue,
                    explicitReceiverKind, null, candidateTrace, tracing,
                    basicCallContext.dataFlowInfoForArguments // todo may be we should create new mutable info for arguments
            )

            // see spec-docs/dynamic-types.md
            if (extensionReceiver != null && extensionReceiver.receiverValue.type.isDynamic()
                && !towerCandidate.descriptor.extensionReceiverParameter!!.value.type.isDynamic()) {
                return MyCandidate(ResolutionCandidateStatus(listOf(ExtensionWithStaticTypeWithDynamicReceiver)), candidateCall)
            }

            if (towerCandidate.descriptor.isHiddenInResolution(languageVersionSettings, basicCallContext.isSuperCall)) {
                return MyCandidate(ResolutionCandidateStatus(listOf(HiddenDescriptor)), candidateCall)
            }

            val callCandidateResolutionContext = CallCandidateResolutionContext.create(
                    candidateCall, basicCallContext, candidateTrace, tracing, basicCallContext.call,
                    CandidateResolveMode.FULLY // todo
            )
            candidateResolver.performResolutionForCandidateCall(callCandidateResolutionContext, basicCallContext.checkArguments) // todo

            val diagnostics = (towerCandidate.diagnostics +
                               checkInfixAndOperator(basicCallContext.call, towerCandidate.descriptor) +
                               createPreviousResolveError(candidateCall.status)).filterNotNull() // todo
            return MyCandidate(ResolutionCandidateStatus(diagnostics), candidateCall)
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

    }

    private inner class CandidateFactoryProviderForInvokeImpl(
            val functionContext: CandidateFactoryImpl<FunctionDescriptor>
    ) : CandidateFactoryProviderForInvoke<MyCandidate<FunctionDescriptor>, MyCandidate<VariableDescriptor>> {

        override fun transformCandidate(
                variable: MyCandidate<VariableDescriptor>,
                invoke: MyCandidate<FunctionDescriptor>
        ): MyCandidate<FunctionDescriptor> {
            val resolvedCallImpl = VariableAsFunctionResolvedCallImpl(
                    invoke.resolvedCall,
                    variable.resolvedCall
            )
            assert(variable.candidateStatus.resultingApplicability.isSuccess) {
                "Variable call must be success: $variable"
            }

            return MyCandidate(ResolutionCandidateStatus(variable.candidateStatus.diagnostics + invoke.candidateStatus.diagnostics), resolvedCallImpl)
        }

        override fun factoryForVariable(stripExplicitReceiver: Boolean): CandidateFactory<VariableDescriptor, MyCandidate<VariableDescriptor>> {
            val newCall = CallTransformer.stripCallArguments(functionContext.basicCallContext.call).let {
                if (stripExplicitReceiver) CallTransformer.stripReceiver(it) else it
            }
            return CandidateFactoryImpl(functionContext.name, functionContext.basicCallContext.replaceCall(newCall), functionContext.tracing)
        }

        override fun factoryForInvoke(
                variable: MyCandidate<VariableDescriptor>,
                useExplicitReceiver: Boolean
        ): Pair<ReceiverValueWithSmartCastInfo, CandidateFactory<FunctionDescriptor, MyCandidate<FunctionDescriptor>>>? {
            assert(variable.resolvedCall.status.possibleTransformToSuccess()) {
                "Incorrect status: ${variable.resolvedCall.status} for variable call: ${variable.resolvedCall} " +
                "and descriptor: ${variable.resolvedCall.candidateDescriptor}"
            }
            val calleeExpression = variable.resolvedCall.call.calleeExpression
            val variableDescriptor = variable.resolvedCall.resultingDescriptor
            assert(variable.resolvedCall.status.possibleTransformToSuccess() && calleeExpression != null) {
                "Unexpected variable candidate: $variable"
            }
            val variableType = variableDescriptor.type

            if (variableType is DeferredType && variableType.isComputing) {
                return null // todo: create special check that there is no invoke on variable
            }
            val basicCallContext = functionContext.basicCallContext
            val variableReceiver = ExpressionReceiver.create(calleeExpression!!,
                                                             variableType,
                                                             basicCallContext.trace.bindingContext)
            // used for smartCasts, see: DataFlowValueFactory.getIdForSimpleNameExpression
            functionContext.tracing.bindReference(variable.resolvedCall.trace, variable.resolvedCall)
            // todo hacks
            val functionCall = CallTransformer.CallForImplicitInvoke(
                    basicCallContext.call.explicitReceiver?.check { useExplicitReceiver },
                    variableReceiver, basicCallContext.call, true)
            val tracingForInvoke = TracingStrategyForInvoke(calleeExpression, functionCall, variableReceiver.type)
            val basicCallResolutionContext = basicCallContext.replaceBindingTrace(variable.resolvedCall.trace)
                    .replaceCall(functionCall)
                    .replaceContextDependency(ContextDependency.DEPENDENT) // todo

            val newContext = CandidateFactoryImpl<FunctionDescriptor>(OperatorNameConventions.INVOKE, basicCallResolutionContext, tracingForInvoke)

            return basicCallResolutionContext.transformToReceiverWithSmartCastInfo(variableReceiver) to newContext
        }

    }

}

private fun ResolutionContext<*>.transformToReceiverWithSmartCastInfo(receiver: ReceiverValue): ReceiverValueWithSmartCastInfo {
    val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver, this)
    return ReceiverValueWithSmartCastInfo(receiver, dataFlowInfo.getCollectedTypes(dataFlowValue), dataFlowValue.isStable)
}

@Deprecated("Temporary error")
internal class PreviousResolutionError(candidateLevel: ResolutionCandidateApplicability): ResolutionDiagnostic(candidateLevel)

@Deprecated("Temporary error")
internal fun createPreviousResolveError(status: ResolutionStatus): PreviousResolutionError? {
    val level = when (status) {
        ResolutionStatus.SUCCESS, ResolutionStatus.INCOMPLETE_TYPE_INFERENCE -> return null
        ResolutionStatus.UNSAFE_CALL_ERROR -> ResolutionCandidateApplicability.MAY_THROW_RUNTIME_ERROR
        else -> ResolutionCandidateApplicability.INAPPLICABLE
    }
    return PreviousResolutionError(level)
}

private val BasicCallResolutionContext.isSuperCall: Boolean get() = call.explicitReceiver is SuperCallReceiverValue
