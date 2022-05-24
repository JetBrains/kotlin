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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceSupport
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl
import org.jetbrains.kotlin.resolve.calls.results.ResolutionResultsHandler
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.*
import org.jetbrains.kotlin.resolve.calls.util.*
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDynamicExtensionAnnotation
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.resolve.scopes.utils.canBeResolvedWithoutDeprecation
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.compactIfPossible
import org.jetbrains.kotlin.utils.sure

class NewResolutionOldInference(
    private val candidateResolver: CandidateResolver,
    private val towerResolver: TowerResolver,
    private val resolutionResultsHandler: ResolutionResultsHandler,
    private val dynamicCallableDescriptors: DynamicCallableDescriptors,
    private val syntheticScopes: SyntheticScopes,
    private val languageVersionSettings: LanguageVersionSettings,
    private val builderInferenceSupport: BuilderInferenceSupport,
    private val deprecationResolver: DeprecationResolver,
    private val typeApproximator: TypeApproximator,
    private val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter,
    private val callResolver: CallResolver,
    private val candidateInterceptor: CandidateInterceptor
) {
    sealed class ResolutionKind {
        abstract internal fun createTowerProcessor(
            outer: NewResolutionOldInference,
            name: Name,
            tracing: TracingStrategy,
            scopeTower: ImplicitScopeTower,
            explicitReceiver: DetailedReceiver?,
            context: BasicCallResolutionContext
        ): ScopeTowerProcessor<MyCandidate>

        object Function : ResolutionKind() {
            override fun createTowerProcessor(
                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
                return createFunctionProcessor(
                    scopeTower,
                    name,
                    functionFactory,
                    outer.CandidateFactoryProviderForInvokeImpl(functionFactory),
                    explicitReceiver
                )
            }
        }

        object Variable : ResolutionKind() {
            override fun createTowerProcessor(
                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val variableFactory = outer.CandidateFactoryImpl(name, context, tracing)
                return createVariableAndObjectProcessor(scopeTower, name, variableFactory, explicitReceiver)
            }
        }

        object CallableReference : ResolutionKind() {
            override fun createTowerProcessor(
                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
                val variableFactory = outer.CandidateFactoryImpl(name, context, tracing)
                return PrioritizedCompositeScopeTowerProcessor(
                    createSimpleFunctionProcessor(scopeTower, name, functionFactory, explicitReceiver, classValueReceiver = false),
                    createVariableProcessor(scopeTower, name, variableFactory, explicitReceiver, classValueReceiver = false)
                )
            }
        }

        object Invoke : ResolutionKind() {
            override fun createTowerProcessor(
                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
                // todo
                val call = (context.call as? CallTransformer.CallForImplicitInvoke).sure {
                    "Call should be CallForImplicitInvoke, but it is: ${context.call}"
                }
                return createProcessorWithReceiverValueOrEmpty(explicitReceiver) {
                    createCallTowerProcessorForExplicitInvoke(
                        scopeTower,
                        functionFactory,
                        context.transformToReceiverWithSmartCastInfo(call.dispatchReceiver),
                        it
                    )
                }
            }

        }

        class GivenCandidates : ResolutionKind() {
            override fun createTowerProcessor(
                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                throw IllegalStateException("Should be not called")
            }
        }
    }

    fun <D : CallableDescriptor> runResolution(
        context: BasicCallResolutionContext,
        name: Name,
        kind: ResolutionKind,
        tracing: TracingStrategy
    ): OverloadResolutionResultsImpl<D> {
        val explicitReceiver = context.call.explicitReceiver
        val detailedReceiver = if (explicitReceiver is QualifierReceiver?) {
            explicitReceiver
        } else {
            context.transformToReceiverWithSmartCastInfo(explicitReceiver as ReceiverValue)
        }

        val dynamicScope = dynamicCallableDescriptors.createDynamicDescriptorScope(context.call, context.scope.ownerDescriptor)
        val scopeTower = ImplicitScopeTowerImpl(
            context, dynamicScope, syntheticScopes, context.call.createLookupLocation(), typeApproximator, implicitsResolutionFilter, callResolver, candidateInterceptor
        )

        val shouldUseOperatorRem = languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)
        val isBinaryRemOperator = isBinaryRemOperator(context.call)
        val nameToResolve = if (isBinaryRemOperator && !shouldUseOperatorRem)
            OperatorConventions.REM_TO_MOD_OPERATION_NAMES[name]!!
        else
            name

        val processor = kind.createTowerProcessor(this, nameToResolve, tracing, scopeTower, detailedReceiver, context)

        if (context.collectAllCandidates) {
            return allCandidatesResult(towerResolver.collectAllCandidates(scopeTower, processor, nameToResolve))
        }

        var candidates =
            towerResolver.runResolve(scopeTower, processor, useOrder = kind != ResolutionKind.CallableReference, name = nameToResolve)

        // Temporary hack to resolve 'rem' as 'mod' if the first is do not present
        val emptyOrInapplicableCandidates = candidates.isEmpty() ||
                candidates.all { it.resultingApplicability.isInapplicable }
        if (isBinaryRemOperator && shouldUseOperatorRem && emptyOrInapplicableCandidates) {
            val deprecatedName = OperatorConventions.REM_TO_MOD_OPERATION_NAMES[name]
            val processorForDeprecatedName =
                kind.createTowerProcessor(this, deprecatedName!!, tracing, scopeTower, detailedReceiver, context)
            candidates = towerResolver.runResolve(
                scopeTower,
                processorForDeprecatedName,
                useOrder = kind != ResolutionKind.CallableReference,
                name = deprecatedName
            )
        }

        candidates = candidateInterceptor.interceptResolvedCandidates(candidates, context, candidateResolver, callResolver, name, kind, tracing)

        if (candidates.isEmpty()) {
            if (reportAdditionalDiagnosticIfNoCandidates(context, nameToResolve, kind, scopeTower, detailedReceiver)) {
                return OverloadResolutionResultsImpl.nameNotFound()
            }
        }

        val overloadResults = convertToOverloadResults<D>(candidates, tracing, context)
        builderInferenceSupport.checkBuilderInferenceCalls(context, tracing, overloadResults)
        return overloadResults
    }

    fun <D : CallableDescriptor> runResolutionForGivenCandidates(
        basicCallContext: BasicCallResolutionContext,
        tracing: TracingStrategy,
        candidates: Collection<OldResolutionCandidate<D>>
    ): OverloadResolutionResultsImpl<D> {
        val resolvedCandidates = candidates.map { candidate ->
            val candidateTrace = TemporaryBindingTrace.create(basicCallContext.trace, "Context for resolve candidate")
            val resolvedCall = ResolvedCallImpl.create(candidate, candidateTrace, tracing, basicCallContext.dataFlowInfoForArguments)

            if (deprecationResolver.isHiddenInResolution(
                    candidate.descriptor, basicCallContext.call, basicCallContext.trace.bindingContext, basicCallContext.isSuperCall
                )
            ) {
                return@map MyCandidate(listOf(HiddenDescriptor), resolvedCall)
            }

            val callCandidateResolutionContext = CallCandidateResolutionContext.create(
                resolvedCall, basicCallContext, candidateTrace, tracing, basicCallContext.call,
                CandidateResolveMode.EXIT_ON_FIRST_ERROR
            )
            candidateResolver.performResolutionForCandidateCall(callCandidateResolutionContext, basicCallContext.checkArguments) // todo

            val diagnostics = listOfNotNull(createPreviousResolveError(resolvedCall.status))
            MyCandidate(diagnostics, resolvedCall) {
                resolvedCall.performRemainingTasks()
                listOfNotNull(createPreviousResolveError(resolvedCall.status))
            }
        }
        if (basicCallContext.collectAllCandidates) {
            val allCandidates = towerResolver.runWithEmptyTowerData(
                KnownResultProcessor(resolvedCandidates),
                TowerResolver.AllCandidatesCollector(), useOrder = false
            )
            return allCandidatesResult(allCandidates)
        }

        val processedCandidates = towerResolver.runWithEmptyTowerData(
            KnownResultProcessor(resolvedCandidates),
            TowerResolver.SuccessfulResultCollector(), useOrder = true
        )

        return convertToOverloadResults(processedCandidates, tracing, basicCallContext)
    }

    private fun <D : CallableDescriptor> allCandidatesResult(allCandidates: Collection<MyCandidate>) =
        OverloadResolutionResultsImpl.nameNotFound<D>().apply {
            this.allCandidates = allCandidates.map {
                @Suppress("UNCHECKED_CAST")
                it.resolvedCall as MutableResolvedCall<D>
            }
        }

    private fun <D : CallableDescriptor> convertToOverloadResults(
        candidates: Collection<MyCandidate>,
        tracing: TracingStrategy,
        basicCallContext: BasicCallResolutionContext
    ): OverloadResolutionResultsImpl<D> {
        val resolvedCalls = candidates.map {
            val (diagnostics, resolvedCall) = it
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
                for (error in diagnostics) {
                    when (error) {
                        is UnsupportedInnerClassCall -> resolvedCall.trace.report(
                            Errors.UNSUPPORTED.on(
                                resolvedCall.call.callElement,
                                error.message
                            )
                        )

                        is NestedClassViaInstanceReference -> tracing.nestedClassAccessViaInstanceReference(
                            resolvedCall.trace,
                            error.classDescriptor,
                            resolvedCall.explicitReceiverKind
                        )

                        is ErrorDescriptorDiagnostic -> {
                            // todo
                            //  return@map null
                        }

                        is ResolvedUsingDeprecatedVisibility -> {
                            reportResolvedUsingDeprecatedVisibility(
                                resolvedCall.call, resolvedCall.candidateDescriptor,
                                resolvedCall.resultingDescriptor, error, resolvedCall.trace
                            )
                        }
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            resolvedCall as MutableResolvedCall<D>
        }

        return resolutionResultsHandler.computeResultAndReportErrors(basicCallContext, tracing, resolvedCalls, languageVersionSettings)
    }

    // true if we found something
    private fun reportAdditionalDiagnosticIfNoCandidates(
        context: BasicCallResolutionContext,
        name: Name,
        kind: ResolutionKind,
        scopeTower: ImplicitScopeTower,
        detailedReceiver: DetailedReceiver?
    ): Boolean {
        val reference = context.call.calleeExpression as? KtReferenceExpression ?: return false

        val errorCandidates = when (kind) {
            ResolutionKind.Function -> collectErrorCandidatesForFunction(scopeTower, name, detailedReceiver)
            ResolutionKind.Variable -> collectErrorCandidatesForVariable(scopeTower, name, detailedReceiver)
            else -> emptyList()
        }

        val candidate = errorCandidates.firstOrNull() as? ErrorCandidate.Classifier ?: return false

        context.trace.record(BindingContext.REFERENCE_TARGET, reference, candidate.descriptor)
        context.trace.report(Errors.RESOLUTION_TO_CLASSIFIER.on(reference, candidate.descriptor, candidate.kind, candidate.errorMessage))

        return true
    }

    public class ImplicitScopeTowerImpl(
        val resolutionContext: BasicCallResolutionContext,
        override val dynamicScope: MemberScope,
        override val syntheticScopes: SyntheticScopes,
        override val location: LookupLocation,
        override val typeApproximator: TypeApproximator,
        override val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter,
        val callResolver: CallResolver,
        val candidateInterceptor: CandidateInterceptor
    ) : ImplicitScopeTower {
        private val cache = HashMap<ReceiverValue, ReceiverValueWithSmartCastInfo>()

        override fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo? =
            scope.implicitReceiver?.value?.let {
                cache.getOrPut(it) { resolutionContext.transformToReceiverWithSmartCastInfo(it) }
            }

        override fun getContextReceivers(scope: LexicalScope): List<ReceiverValueWithSmartCastInfo> =
            scope.contextReceiversGroup.map { cache.getOrPut(it.value) { resolutionContext.transformToReceiverWithSmartCastInfo(it.value) } }

        override fun getNameForGivenImportAlias(name: Name): Name? =
            (resolutionContext.call.callElement.containingFile as? KtFile)?.getNameForGivenImportAlias(name)

        override val lexicalScope: LexicalScope get() = resolutionContext.scope

        override val isDebuggerContext: Boolean get() = resolutionContext.isDebuggerContext

        override val isNewInferenceEnabled: Boolean
            get() = resolutionContext.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)

        override val areContextReceiversEnabled: Boolean
            get() = resolutionContext.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)

        override val languageVersionSettings: LanguageVersionSettings
            get() = resolutionContext.languageVersionSettings

        override fun interceptFunctionCandidates(
            resolutionScope: ResolutionScope,
            name: Name,
            initialResults: Collection<FunctionDescriptor>,
            location: LookupLocation,
            dispatchReceiver: ReceiverValueWithSmartCastInfo?,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
        ): Collection<FunctionDescriptor> {
            return candidateInterceptor.interceptFunctionCandidates(initialResults, this, resolutionContext, resolutionScope, callResolver, name, location)
        }

        override fun interceptVariableCandidates(
            resolutionScope: ResolutionScope,
            name: Name,
            initialResults: Collection<VariableDescriptor>,
            location: LookupLocation,
            dispatchReceiver: ReceiverValueWithSmartCastInfo?,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
        ): Collection<VariableDescriptor> {
            return candidateInterceptor.interceptVariableCandidates(initialResults, this, resolutionContext, resolutionScope, callResolver, name, location)
        }
    }

    class MyCandidate(
        // Diagnostics that are already computed
        // if resultingApplicability is successful they must be the same as `diagnostics`,
        // otherwise they might be a bit different but result remains unsuccessful
        val eagerDiagnostics: List<KotlinCallDiagnostic>,
        val resolvedCall: MutableResolvedCall<*>,
        finalDiagnosticsComputation: (() -> List<KotlinCallDiagnostic>)? = null
    ) : Candidate {
        val diagnostics: List<KotlinCallDiagnostic> by lazy(LazyThreadSafetyMode.NONE) {
            finalDiagnosticsComputation?.invoke() ?: eagerDiagnostics
        }

        operator fun component1() = diagnostics
        operator fun component2() = resolvedCall

        override val resultingApplicability: CandidateApplicability by lazy(LazyThreadSafetyMode.NONE) {
            getResultApplicability(diagnostics)
        }

        override fun addCompatibilityWarning(other: Candidate) {
            // Only applicable for new inference
        }

        override val isSuccessful = getResultApplicability(eagerDiagnostics).isSuccess
    }

    private inner class CandidateFactoryImpl(
        val name: Name,
        val basicCallContext: BasicCallResolutionContext,
        val tracing: TracingStrategy
    ) : CandidateFactory<MyCandidate> {
        override fun createCandidate(
            towerCandidate: CandidateWithBoundDispatchReceiver,
            explicitReceiverKind: ExplicitReceiverKind,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
        ): MyCandidate {

            val candidateTrace = TemporaryBindingTrace.create(basicCallContext.trace, "Context for resolve candidate")
            val candidateCall = ResolvedCallImpl(
                basicCallContext.call, towerCandidate.descriptor,
                towerCandidate.dispatchReceiver?.receiverValue, extensionReceiver?.receiverValue,
                explicitReceiverKind, null, candidateTrace, tracing,
                basicCallContext.dataFlowInfoForArguments // todo may be we should create new mutable info for arguments
            )

            /**
             * See https://jetbrains.quip.com/qcTDAFcgFLEM
             *
             * For now we have only 2 functions with dynamic receivers: iterator() and unsafeCast()
             * Both this function are marked via @kotlin.internal.DynamicExtension.
             */
            if (extensionReceiver != null) {
                val parameterIsDynamic = towerCandidate.descriptor.extensionReceiverParameter!!.value.type.isDynamic()
                val argumentIsDynamic = extensionReceiver.receiverValue.type.isDynamic()

                if (parameterIsDynamic != argumentIsDynamic ||
                    (parameterIsDynamic && !towerCandidate.descriptor.hasDynamicExtensionAnnotation())
                ) {
                    return MyCandidate(listOf(HiddenExtensionRelatedToDynamicTypes), candidateCall)
                }
            }

            if (deprecationResolver.isHiddenInResolution(
                    towerCandidate.descriptor, basicCallContext.call, basicCallContext.trace.bindingContext, basicCallContext.isSuperCall
                )
            ) {
                return MyCandidate(listOf(HiddenDescriptor), candidateCall)
            }

            val callCandidateResolutionContext = CallCandidateResolutionContext.create(
                candidateCall, basicCallContext, candidateTrace, tracing, basicCallContext.call,
                CandidateResolveMode.EXIT_ON_FIRST_ERROR
            )
            candidateResolver.performResolutionForCandidateCall(callCandidateResolutionContext, basicCallContext.checkArguments) // todo

            val diagnostics = createDiagnosticsForCandidate(towerCandidate, candidateCall)
            return MyCandidate(diagnostics, candidateCall) {
                candidateCall.performRemainingTasks()
                createDiagnosticsForCandidate(towerCandidate, candidateCall)
            }
        }

        /**
         * The function is called only inside [NoExplicitReceiverScopeTowerProcessor] with [TowerData.BothTowerLevelAndContextReceiversGroup].
         * This case involves only [SimpleCandidateFactory].
         */
        override fun createCandidate(
            towerCandidate: CandidateWithBoundDispatchReceiver,
            explicitReceiverKind: ExplicitReceiverKind,
            extensionReceiverCandidates: List<ReceiverValueWithSmartCastInfo>
        ): MyCandidate = error("${this::class.simpleName} doesn't support candidates with multiple extension receiver candidates")

        override fun createErrorCandidate(): MyCandidate {
            throw IllegalStateException("Not supported creating error candidate for the old type inference candidate factory")
        }

        private fun createDiagnosticsForCandidate(
            towerCandidate: CandidateWithBoundDispatchReceiver,
            candidateCall: ResolvedCallImpl<CallableDescriptor>
        ): List<ResolutionDiagnostic> =
            mutableListOf<ResolutionDiagnostic>().apply {
                addAll(towerCandidate.diagnostics)
                addAll(checkInfixAndOperator(basicCallContext.call, towerCandidate.descriptor))
                addIfNotNull(createPreviousResolveError(candidateCall.status))
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
        val functionContext: CandidateFactoryImpl
    ) : CandidateFactoryProviderForInvoke<MyCandidate> {

        override fun transformCandidate(
            variable: MyCandidate,
            invoke: MyCandidate
        ): MyCandidate {
            @Suppress("UNCHECKED_CAST") val resolvedCallImpl = VariableAsFunctionResolvedCallImpl(
                invoke.resolvedCall as MutableResolvedCall<FunctionDescriptor>,
                variable.resolvedCall as MutableResolvedCall<VariableDescriptor>
            )
            assert(variable.resultingApplicability.isSuccess) {
                "Variable call must be success: $variable"
            }

            return MyCandidate(variable.eagerDiagnostics + invoke.eagerDiagnostics, resolvedCallImpl) {
                variable.diagnostics + invoke.diagnostics
            }
        }

        override fun factoryForVariable(stripExplicitReceiver: Boolean): CandidateFactory<MyCandidate> {
            val newCall = CallTransformer.stripCallArguments(functionContext.basicCallContext.call).let {
                if (stripExplicitReceiver) CallTransformer.stripReceiver(it) else it
            }
            return CandidateFactoryImpl(
                functionContext.name,
                functionContext.basicCallContext.replaceCall(newCall),
                functionContext.tracing
            )
        }

        override fun factoryForInvoke(
            variable: MyCandidate,
            useExplicitReceiver: Boolean
        ): Pair<ReceiverValueWithSmartCastInfo, CandidateFactory<MyCandidate>>? {
            assert(variable.resolvedCall.status.possibleTransformToSuccess()) {
                "Incorrect status: ${variable.resolvedCall.status} for variable call: ${variable.resolvedCall} " +
                        "and descriptor: ${variable.resolvedCall.candidateDescriptor}"
            }
            val calleeExpression = variable.resolvedCall.call.calleeExpression
            val variableDescriptor = variable.resolvedCall.resultingDescriptor as VariableDescriptor
            assert(variable.resolvedCall.status.possibleTransformToSuccess() && calleeExpression != null) {
                "Unexpected variable candidate: $variable"
            }
            val variableType = variableDescriptor.type

            if (variableType is DeferredType && variableType.isComputing) {
                return null // todo: create special check that there is no invoke on variable
            }
            val basicCallContext = functionContext.basicCallContext
            val variableReceiver = ExpressionReceiver.create(
                calleeExpression!!,
                variableType,
                basicCallContext.trace.bindingContext
            )
            // used for smartCasts, see: DataFlowValueFactory.getIdForSimpleNameExpression
            functionContext.tracing.bindReference(variable.resolvedCall.trace, variable.resolvedCall)
            // todo hacks
            val functionCall = CallTransformer.CallForImplicitInvoke(
                basicCallContext.call.explicitReceiver?.takeIf { useExplicitReceiver },
                variableReceiver, basicCallContext.call, true
            )
            val tracingForInvoke = TracingStrategyForInvoke(calleeExpression, functionCall, variableReceiver.type)
            val basicCallResolutionContext = basicCallContext.replaceBindingTrace(variable.resolvedCall.trace)
                .replaceCall(functionCall)
                .replaceContextDependency(ContextDependency.DEPENDENT) // todo

            val newContext = CandidateFactoryImpl(OperatorNameConventions.INVOKE, basicCallResolutionContext, tracingForInvoke)

            return basicCallResolutionContext.transformToReceiverWithSmartCastInfo(variableReceiver) to newContext
        }

    }

}

fun ResolutionContext<*>.transformToReceiverWithSmartCastInfo(receiver: ReceiverValue) =
    transformToReceiverWithSmartCastInfo(scope.ownerDescriptor, trace.bindingContext, dataFlowInfo, receiver, languageVersionSettings, dataFlowValueFactory)

fun transformToReceiverWithSmartCastInfo(
    containingDescriptor: DeclarationDescriptor,
    bindingContext: BindingContext,
    dataFlowInfo: DataFlowInfo,
    receiver: ReceiverValue,
    languageVersionSettings: LanguageVersionSettings,
    dataFlowValueFactory: DataFlowValueFactory
): ReceiverValueWithSmartCastInfo {
    val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiver, bindingContext, containingDescriptor)
    return ReceiverValueWithSmartCastInfo(
        receiver,
        dataFlowInfo.getCollectedTypes(dataFlowValue, languageVersionSettings).compactIfPossible(),
        dataFlowValue.isStable
    )
}

internal class PreviousResolutionError(candidateLevel: CandidateApplicability) : ResolutionDiagnostic(candidateLevel)

internal fun createPreviousResolveError(status: ResolutionStatus): PreviousResolutionError? {
    val level = when (status) {
        ResolutionStatus.SUCCESS, ResolutionStatus.INCOMPLETE_TYPE_INFERENCE -> return null
        ResolutionStatus.UNSAFE_CALL_ERROR -> CandidateApplicability.UNSAFE_CALL
        ResolutionStatus.ARGUMENTS_MAPPING_ERROR -> CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR
        ResolutionStatus.RECEIVER_TYPE_ERROR -> CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
        else -> CandidateApplicability.INAPPLICABLE
    }
    return PreviousResolutionError(level)
}

internal fun Call.isCallWithSuperReceiver(): Boolean = explicitReceiver is SuperCallReceiverValue 
private val BasicCallResolutionContext.isSuperCall: Boolean get() = call.isCallWithSuperReceiver()

internal fun reportResolvedUsingDeprecatedVisibility(
    call: Call,
    candidateDescriptor: CallableDescriptor,
    resultingDescriptor : CallableDescriptor,
    diagnostic: ResolvedUsingDeprecatedVisibility,
    trace: BindingTrace
) {
    trace.record(
        BindingContext.DEPRECATED_SHORT_NAME_ACCESS,
        call.calleeExpression
    )

    val descriptorToLookup: DeclarationDescriptor = when (candidateDescriptor) {
        is ClassConstructorDescriptor -> candidateDescriptor.containingDeclaration
        is FakeCallableDescriptorForObject -> candidateDescriptor.classDescriptor
        is SyntheticMemberDescriptor<*> -> candidateDescriptor.baseDescriptorForSynthetic
        is PropertyDescriptor, is FunctionDescriptor -> candidateDescriptor
        else -> error(
            "Unexpected candidate descriptor of resolved call with " +
                    "ResolvedUsingDeprecatedVisibility-diagnostic: $candidateDescriptor\n" +
                    "Call context: ${call.callElement.parent?.text}"
        )
    }

    // If this descriptor was resolved from HierarchicalScope, then there can be another, non-deprecated path
    // in parents of base scope
    val sourceScope = diagnostic.baseSourceScope
    val canBeResolvedWithoutDeprecation = if (sourceScope is HierarchicalScope) {
        descriptorToLookup.canBeResolvedWithoutDeprecation(
            sourceScope,
            diagnostic.lookupLocation
        )
    } else {
        // Normally, that should be unreachable, but instead of asserting that, we will report diagnostic
        false
    }

    if (!canBeResolvedWithoutDeprecation) {
        trace.report(
            Errors.DEPRECATED_ACCESS_BY_SHORT_NAME.on(call.callElement, resultingDescriptor)
        )
    }

}
