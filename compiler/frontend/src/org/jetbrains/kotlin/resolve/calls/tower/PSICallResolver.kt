/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.EffectSystem
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.KotlinCallResolver
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isBinaryRemOperator
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.components.CallableReferenceResolver
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.buildResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallableDescriptors
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.*
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.util.*

class PSICallResolver(
    private val typeResolver: TypeResolver,
    private val expressionTypingServices: ExpressionTypingServices,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dynamicCallableDescriptors: DynamicCallableDescriptors,
    private val syntheticScopes: SyntheticScopes,
    private val callComponents: KotlinCallComponents,
    private val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer,
    private val kotlinCallResolver: KotlinCallResolver,
    private val typeApproximator: TypeApproximator,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val effectSystem: EffectSystem,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val callableReferenceResolver: CallableReferenceResolver,
    private val candidateInterceptor: CandidateInterceptor,
    private val missingSupertypesResolver: MissingSupertypesResolver
) {
    private val givenCandidatesName = Name.special("<given candidates>")

    val defaultResolutionKinds = setOf(
        NewResolutionOldInference.ResolutionKind.Function,
        NewResolutionOldInference.ResolutionKind.Variable,
        NewResolutionOldInference.ResolutionKind.Invoke
    )

    fun <D : CallableDescriptor> runResolutionAndInference(
        context: BasicCallResolutionContext,
        name: Name,
        resolutionKind: NewResolutionOldInference.ResolutionKind,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        val isBinaryRemOperator = isBinaryRemOperator(context.call)
        val refinedName = refineNameForRemOperator(isBinaryRemOperator, name)

        val kotlinCallKind = resolutionKind.toKotlinCallKind()
        val kotlinCall = toKotlinCall(context, kotlinCallKind, context.call, refinedName, tracingStrategy)
        val scopeTower = ASTScopeTower(context)
        val resolutionCallbacks = createResolutionCallbacks(context)

        val expectedType = calculateExpectedType(context)
        var result =
            kotlinCallResolver.resolveCall(scopeTower, resolutionCallbacks, kotlinCall, expectedType, context.collectAllCandidates) {
                FactoryProviderForInvoke(context, scopeTower, kotlinCall)
            }

        val shouldUseOperatorRem = languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)
        if (isBinaryRemOperator && shouldUseOperatorRem && (result.isEmpty() || result.areAllInapplicable())) {
            result = resolveToDeprecatedMod(name, context, kotlinCallKind, tracingStrategy, scopeTower, resolutionCallbacks, expectedType)
        }

        if (result.isEmpty() && reportAdditionalDiagnosticIfNoCandidates(context, scopeTower, kotlinCallKind, kotlinCall)) {
            return OverloadResolutionResultsImpl.nameNotFound()
        }

        val overloadResolutionResults = convertToOverloadResolutionResults<D>(context, result, tracingStrategy)
        return overloadResolutionResults.also {
            clearCacheForApproximationResults()
        }
    }

    // actually, `D` is at least FunctionDescriptor, but right now because of CallResolver it isn't possible change upper bound for `D`
    fun <D : CallableDescriptor> runResolutionAndInferenceForGivenCandidates(
        context: BasicCallResolutionContext,
        resolutionCandidates: Collection<ResolutionCandidate<D>>,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        val dispatchReceiver = resolutionCandidates.firstNotNullResult { it.dispatchReceiver }

        val kotlinCall =
            toKotlinCall(context, KotlinCallKind.FUNCTION, context.call, givenCandidatesName, tracingStrategy, dispatchReceiver)
        val scopeTower = ASTScopeTower(context)
        val resolutionCallbacks = createResolutionCallbacks(context)

        val givenCandidates = resolutionCandidates.map {
            GivenCandidate(
                it.descriptor as FunctionDescriptor,
                it.dispatchReceiver?.let { context.transformToReceiverWithSmartCastInfo(it) },
                it.knownTypeParametersResultingSubstitutor
            )
        }

        val result = kotlinCallResolver.resolveGivenCandidates(
            scopeTower, resolutionCallbacks, kotlinCall, calculateExpectedType(context), givenCandidates, context.collectAllCandidates
        )
        val overloadResolutionResults = convertToOverloadResolutionResults<D>(context, result, tracingStrategy)
        return overloadResolutionResults.also {
            clearCacheForApproximationResults()
        }
    }

    private fun clearCacheForApproximationResults() {
        // Mostly, we approximate captured or some other internal types that don't live longer than resolve for a call,
        // so it's quite useless to preserve cache for longer time
        typeApproximator.clearCache()
    }

    private fun resolveToDeprecatedMod(
        remOperatorName: Name,
        context: BasicCallResolutionContext,
        kotlinCallKind: KotlinCallKind,
        tracingStrategy: TracingStrategy,
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacksImpl,
        expectedType: UnwrappedType?
    ): CallResolutionResult {
        val deprecatedName = OperatorConventions.REM_TO_MOD_OPERATION_NAMES[remOperatorName]!!
        val callWithDeprecatedName = toKotlinCall(context, kotlinCallKind, context.call, deprecatedName, tracingStrategy)
        return kotlinCallResolver.resolveCall(
            scopeTower, resolutionCallbacks, callWithDeprecatedName, expectedType, context.collectAllCandidates
        ) {
            FactoryProviderForInvoke(context, scopeTower, callWithDeprecatedName)
        }
    }

    private fun refineNameForRemOperator(isBinaryRemOperator: Boolean, name: Name): Name {
        val shouldUseOperatorRem = languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)
        return if (isBinaryRemOperator && !shouldUseOperatorRem) OperatorConventions.REM_TO_MOD_OPERATION_NAMES[name]!! else name
    }

    private fun createResolutionCallbacks(context: BasicCallResolutionContext) =
        createResolutionCallbacks(context.trace, context.inferenceSession, context)

    fun createResolutionCallbacks(trace: BindingTrace, inferenceSession: InferenceSession, context: BasicCallResolutionContext?) =
        KotlinResolutionCallbacksImpl(
            trace, expressionTypingServices, typeApproximator,
            argumentTypeResolver, languageVersionSettings, kotlinToResolvedCallTransformer,
            dataFlowValueFactory, inferenceSession, constantExpressionEvaluator, typeResolver,
            this, postponedArgumentsAnalyzer, kotlinConstraintSystemCompleter, callComponents,
            doubleColonExpressionResolver, deprecationResolver, moduleDescriptor, context, missingSupertypesResolver
        )

    private fun calculateExpectedType(context: BasicCallResolutionContext): UnwrappedType? {
        val expectedType = context.expectedType.unwrap()

        return if (context.contextDependency == ContextDependency.DEPENDENT) {
            assert(TypeUtils.noExpectedType(expectedType)) {
                "Should have no expected type, got: $expectedType"
            }
            null
        } else {
            if (expectedType.isError) TypeUtils.NO_EXPECTED_TYPE else expectedType
        }
    }

    fun <D : CallableDescriptor> convertToOverloadResolutionResults(
        context: BasicCallResolutionContext,
        result: CallResolutionResult,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        if (result is AllCandidatesResolutionResult) {
            val resolvedCalls = result.allCandidates.map { (candidate, diagnostics) ->
                val system = candidate.getSystem()
                val resultingSubstitutor =
                    system.asReadOnlyStorage().buildResultingSubstitutor(system as TypeSystemInferenceExtensionContext)

                kotlinToResolvedCallTransformer.transformToResolvedCall<D>(
                    candidate.resolvedCall, null, resultingSubstitutor, diagnostics
                )
            }

            return AllCandidates(resolvedCalls)
        }

        val trace = context.trace

        handleErrorResolutionResult<D>(context, trace, result, tracingStrategy)?.let { errorResult ->
            context.inferenceSession.addErrorCallInfo(PSIErrorCallInfo(result, errorResult))
            return errorResult
        }

        val resolvedCall = kotlinToResolvedCallTransformer.transformAndReport<D>(result, context, tracingStrategy)

        // NB. Be careful with moving this invocation, as effect system expects resolution results to be written in trace
        // (see EffectSystem for details)
        resolvedCall.recordEffects(trace)

        return SingleOverloadResolutionResult(resolvedCall)
    }

    private fun <D : CallableDescriptor> handleErrorResolutionResult(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        result: CallResolutionResult,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D>? {
        val diagnostics = result.diagnostics

        diagnostics.firstIsInstanceOrNull<NoneCandidatesCallDiagnostic>()?.let {
            kotlinToResolvedCallTransformer.transformAndReport<D>(result, context, tracingStrategy)

            tracingStrategy.unresolvedReference(trace)
            return OverloadResolutionResultsImpl.nameNotFound()
        }

        diagnostics.firstIsInstanceOrNull<ManyCandidatesCallDiagnostic>()?.let {
            kotlinToResolvedCallTransformer.transformAndReport<D>(result, context, tracingStrategy)

            return transformManyCandidatesAndRecordTrace(it, tracingStrategy, trace, context)
        }

        if (getResultApplicability(diagnostics) == ResolutionCandidateApplicability.INAPPLICABLE_WRONG_RECEIVER) {
            val singleCandidate = result.resultCallAtom() ?: error("Should be not null for result: $result")
            val resolvedCall = kotlinToResolvedCallTransformer.onlyTransform<D>(singleCandidate, diagnostics).also {
                tracingStrategy.unresolvedReferenceWrongReceiver(trace, listOf(it))
            }

            return SingleOverloadResolutionResult(resolvedCall)
        }

        return null
    }

    private fun <D : CallableDescriptor> transformManyCandidatesAndRecordTrace(
        diagnostic: ManyCandidatesCallDiagnostic,
        tracingStrategy: TracingStrategy,
        trace: BindingTrace,
        context: BasicCallResolutionContext
    ): ManyCandidates<D> {
        val resolvedCalls = diagnostic.candidates.map {
            kotlinToResolvedCallTransformer.onlyTransform<D>(
                it.resolvedCall, it.diagnosticsFromResolutionParts + it.getSystem().diagnostics
            )
        }

        if (diagnostic.candidates.areAllFailed()) {
            if (diagnostic.candidates.areAllFailedWithInapplicableWrongReceiver()) {
                tracingStrategy.unresolvedReferenceWrongReceiver(trace, resolvedCalls)
            } else {
                tracingStrategy.noneApplicable(trace, resolvedCalls)
                tracingStrategy.recordAmbiguity(trace, resolvedCalls)
            }
        } else {
            tracingStrategy.recordAmbiguity(trace, resolvedCalls)
            if (!context.call.hasUnresolvedArguments(context)) {
                if (resolvedCalls.allIncomplete) {
                    tracingStrategy.cannotCompleteResolve(trace, resolvedCalls)
                } else {
                    tracingStrategy.ambiguity(trace, resolvedCalls)
                }
            }
        }
        return ManyCandidates(resolvedCalls)
    }

    private val List<ResolvedCall<*>>.allIncomplete: Boolean get() = all { it.status == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE }

    private fun ResolvedCall<*>.recordEffects(trace: BindingTrace) {
        val moduleDescriptor = DescriptorUtils.getContainingModule(this.resultingDescriptor?.containingDeclaration ?: return)
        recordLambdasInvocations(trace, moduleDescriptor)
        recordResultInfo(trace, moduleDescriptor)
    }

    private fun ResolvedCall<*>.recordResultInfo(trace: BindingTrace, moduleDescriptor: ModuleDescriptor) {
        if (this !is NewResolvedCallImpl) return
        val resultDFIfromES = effectSystem.getDataFlowInfoForFinishedCall(this, trace, moduleDescriptor)
        this.updateResultingDataFlowInfo(resultDFIfromES)
    }

    private fun ResolvedCall<*>.recordLambdasInvocations(trace: BindingTrace, moduleDescriptor: ModuleDescriptor) {
        effectSystem.recordDefiniteInvocations(this, trace, moduleDescriptor)
    }

    private fun CallResolutionResult.isEmpty(): Boolean =
        diagnostics.firstIsInstanceOrNull<NoneCandidatesCallDiagnostic>() != null

    private fun Collection<KotlinResolutionCandidate>.areAllFailed() =
        all {
            !it.resultingApplicability.isSuccess
        }

    private fun Collection<KotlinResolutionCandidate>.areAllFailedWithInapplicableWrongReceiver() =
        all {
            it.resultingApplicability == ResolutionCandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
        }

    private fun CallResolutionResult.areAllInapplicable(): Boolean {
        val manyCandidates = diagnostics.firstIsInstanceOrNull<ManyCandidatesCallDiagnostic>()?.candidates
        if (manyCandidates != null) {
            return manyCandidates.areAllFailed()
        }

        val applicability = getResultApplicability(diagnostics)
        return applicability == ResolutionCandidateApplicability.INAPPLICABLE ||
                applicability == ResolutionCandidateApplicability.INAPPLICABLE_WRONG_RECEIVER ||
                applicability == ResolutionCandidateApplicability.HIDDEN
    }

    // true if we found something
    private fun reportAdditionalDiagnosticIfNoCandidates(
        context: BasicCallResolutionContext,
        scopeTower: ImplicitScopeTower,
        kind: KotlinCallKind,
        kotlinCall: KotlinCall
    ): Boolean {
        val reference = context.call.calleeExpression as? KtReferenceExpression ?: return false

        val errorCandidates = when (kind) {
            KotlinCallKind.FUNCTION ->
                collectErrorCandidatesForFunction(scopeTower, kotlinCall.name, kotlinCall.explicitReceiver?.receiver)
            KotlinCallKind.VARIABLE ->
                collectErrorCandidatesForVariable(scopeTower, kotlinCall.name, kotlinCall.explicitReceiver?.receiver)
            else -> emptyList()
        }

        for (candidate in errorCandidates) {
            if (candidate is ErrorCandidate.Classifier) {
                context.trace.record(BindingContext.REFERENCE_TARGET, reference, candidate.descriptor)
                context.trace.report(
                    Errors.RESOLUTION_TO_CLASSIFIER.on(
                        reference,
                        candidate.descriptor,
                        candidate.kind,
                        candidate.errorMessage
                    )
                )
                return true
            }
        }
        return false
    }


    private inner class ASTScopeTower(
        val context: BasicCallResolutionContext
    ) : ImplicitScopeTower {
        // todo may be for invoke for case variable + invoke we should create separate dynamicScope(by newCall for invoke)
        override val dynamicScope: MemberScope =
            dynamicCallableDescriptors.createDynamicDescriptorScope(context.call, context.scope.ownerDescriptor)

        // same for location
        override val location: LookupLocation = context.call.createLookupLocation()

        override val syntheticScopes: SyntheticScopes get() = this@PSICallResolver.syntheticScopes
        override val isDebuggerContext: Boolean get() = context.isDebuggerContext
        override val isNewInferenceEnabled: Boolean get() = context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
        override val lexicalScope: LexicalScope get() = context.scope
        override val typeApproximator: TypeApproximator get() = this@PSICallResolver.typeApproximator
        private val cache = HashMap<ReceiverParameterDescriptor, ReceiverValueWithSmartCastInfo>()

        override fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo? {
            val implicitReceiver = scope.implicitReceiver ?: return null

            return cache.getOrPut(implicitReceiver) {
                context.transformToReceiverWithSmartCastInfo(implicitReceiver.value)
            }
        }

        override fun interceptCandidates(
            resolutionScope: ResolutionScope,
            name: Name,
            initialResults: Collection<FunctionDescriptor>,
            location: LookupLocation
        ): Collection<FunctionDescriptor> {
            return candidateInterceptor.interceptCandidates(initialResults, this, context, resolutionScope, null, name, location)
        }
    }

    private inner class FactoryProviderForInvoke(
        val context: BasicCallResolutionContext,
        val scopeTower: ImplicitScopeTower,
        val kotlinCall: PSIKotlinCallImpl
    ) : CandidateFactoryProviderForInvoke<KotlinResolutionCandidate> {

        init {
            assert(kotlinCall.dispatchReceiverForInvokeExtension == null) { kotlinCall }
        }

        override fun transformCandidate(
            variable: KotlinResolutionCandidate,
            invoke: KotlinResolutionCandidate
        ) = invoke

        override fun factoryForVariable(stripExplicitReceiver: Boolean): CandidateFactory<KotlinResolutionCandidate> {
            val explicitReceiver = if (stripExplicitReceiver) null else kotlinCall.explicitReceiver
            val variableCall = PSIKotlinCallForVariable(kotlinCall, explicitReceiver, kotlinCall.name)
            return SimpleCandidateFactory(
                callComponents, scopeTower, variableCall, createResolutionCallbacks(context), callableReferenceResolver
            )
        }

        override fun factoryForInvoke(variable: KotlinResolutionCandidate, useExplicitReceiver: Boolean):
                Pair<ReceiverValueWithSmartCastInfo, CandidateFactory<KotlinResolutionCandidate>>? {
            if (isRecursiveVariableResolution(variable)) return null

            assert(variable.isSuccessful) {
                "Variable call should be successful: $variable " +
                        "Descriptor: ${variable.resolvedCall.candidateDescriptor}"
            }
            val variableCallArgument = createReceiverCallArgument(variable)

            val explicitReceiver = kotlinCall.explicitReceiver
            val callForInvoke = if (useExplicitReceiver && explicitReceiver != null) {
                PSIKotlinCallForInvoke(kotlinCall, variable, explicitReceiver, variableCallArgument)
            } else {
                PSIKotlinCallForInvoke(kotlinCall, variable, variableCallArgument, null)
            }

            return variableCallArgument.receiver to SimpleCandidateFactory(
                callComponents, scopeTower, callForInvoke, createResolutionCallbacks(context), callableReferenceResolver
            )
        }

        // todo: create special check that there is no invoke on variable
        private fun isRecursiveVariableResolution(variable: KotlinResolutionCandidate): Boolean {
            val variableType = variable.resolvedCall.candidateDescriptor.returnType
            return variableType is DeferredType && variableType.isComputing
        }

        // todo: review
        private fun createReceiverCallArgument(variable: KotlinResolutionCandidate): SimpleKotlinCallArgument {
            variable.forceResolution()
            val variableReceiver = createReceiverValueWithSmartCastInfo(variable)
            if (variableReceiver.possibleTypes.isNotEmpty()) {
                return ReceiverExpressionKotlinCallArgument(
                    createReceiverValueWithSmartCastInfo(variable),
                    isForImplicitInvoke = true
                )
            }

            val psiKotlinCall = variable.resolvedCall.atom.psiKotlinCall

            val variableResult = PartialCallResolutionResult(variable.resolvedCall, listOf(), variable.getSystem().asReadOnlyStorage())

            return SubKotlinCallArgumentImpl(
                CallMaker.makeExternalValueArgument((variableReceiver.receiverValue as ExpressionReceiver).expression),
                psiKotlinCall.resultDataFlowInfo, psiKotlinCall.resultDataFlowInfo, variableReceiver,
                variableResult
            )
        }

        // todo: decrease hacks count
        private fun createReceiverValueWithSmartCastInfo(variable: KotlinResolutionCandidate): ReceiverValueWithSmartCastInfo {
            val callForVariable = variable.resolvedCall.atom as PSIKotlinCallForVariable
            val calleeExpression = callForVariable.baseCall.psiCall.calleeExpression as? KtReferenceExpression
                ?: error("Unexpected call : ${callForVariable.baseCall.psiCall}")

            val temporaryTrace = TemporaryBindingTrace.create(context.trace, "Context for resolve candidate")

            val type = variable.resolvedCall.freshReturnType!!
            val variableReceiver = ExpressionReceiver.create(calleeExpression, type, temporaryTrace.bindingContext)

            temporaryTrace.record(BindingContext.REFERENCE_TARGET, calleeExpression, variable.resolvedCall.candidateDescriptor)
            val dataFlowValue =
                dataFlowValueFactory.createDataFlowValue(variableReceiver, temporaryTrace.bindingContext, context.scope.ownerDescriptor)
            return ReceiverValueWithSmartCastInfo(
                variableReceiver,
                context.dataFlowInfo.getCollectedTypes(dataFlowValue, context.languageVersionSettings),
                dataFlowValue.isStable
            ).prepareReceiverRegardingCaptureTypes()
        }
    }

    private fun NewResolutionOldInference.ResolutionKind.toKotlinCallKind(): KotlinCallKind =
        when (this) {
            is NewResolutionOldInference.ResolutionKind.Function -> KotlinCallKind.FUNCTION
            is NewResolutionOldInference.ResolutionKind.Variable -> KotlinCallKind.VARIABLE
            is NewResolutionOldInference.ResolutionKind.Invoke -> KotlinCallKind.INVOKE
            is NewResolutionOldInference.ResolutionKind.CallableReference -> KotlinCallKind.UNSUPPORTED
            is NewResolutionOldInference.ResolutionKind.GivenCandidates -> KotlinCallKind.UNSUPPORTED
        }

    private fun toKotlinCall(
        context: BasicCallResolutionContext,
        kotlinCallKind: KotlinCallKind,
        oldCall: Call,
        name: Name,
        tracingStrategy: TracingStrategy,
        forcedExplicitReceiver: Receiver? = null
    ): PSIKotlinCallImpl {
        val resolvedExplicitReceiver = resolveReceiver(
            context, forcedExplicitReceiver ?: oldCall.explicitReceiver, oldCall.isSafeCall(), isForImplicitInvoke = false
        )
        val dispatchReceiverForInvoke = resolveDispatchReceiverForInvoke(context, kotlinCallKind, oldCall)

        val resolvedTypeArguments = resolveTypeArguments(context, oldCall.typeArguments)

        val lambdasOutsideParenthesis = oldCall.functionLiteralArguments.size
        val extraArgumentsNumber = if (oldCall.callType == Call.CallType.ARRAY_SET_METHOD) 1 else lambdasOutsideParenthesis

        val allValueArguments = oldCall.valueArguments
        val argumentsInParenthesis = if (extraArgumentsNumber == 0) allValueArguments else allValueArguments.dropLast(extraArgumentsNumber)

        val externalLambdaArguments = oldCall.functionLiteralArguments
        val resolvedArgumentsInParenthesis = resolveArgumentsInParenthesis(context, argumentsInParenthesis)

        val externalArgument = if (oldCall.callType == Call.CallType.ARRAY_SET_METHOD) {
            assert(externalLambdaArguments.isEmpty()) {
                "Unexpected lambda parameters for call $oldCall"
            }
            allValueArguments.last()
        } else {
            if (externalLambdaArguments.size > 1) {
                for (i in externalLambdaArguments.indices) {
                    if (i == 0) continue
                    val lambdaExpression = externalLambdaArguments[i].getLambdaExpression() ?: continue

                    if (lambdaExpression.isTrailingLambdaOnNewLIne) {
                        context.trace.report(Errors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.on(lambdaExpression))
                    }
                    context.trace.report(Errors.MANY_LAMBDA_EXPRESSION_ARGUMENTS.on(lambdaExpression))
                }
            }

            externalLambdaArguments.firstOrNull()
        }

        val dataFlowInfoAfterArgumentsInParenthesis =
            if (externalArgument != null && resolvedArgumentsInParenthesis.isNotEmpty())
                resolvedArgumentsInParenthesis.last().psiCallArgument.dataFlowInfoAfterThisArgument
            else
                context.dataFlowInfoForArguments.resultInfo

        val resolvedExternalArgument = externalArgument?.let { resolveValueArgument(context, dataFlowInfoAfterArgumentsInParenthesis, it) }
        val resultDataFlowInfo = resolvedExternalArgument?.dataFlowInfoAfterThisArgument ?: dataFlowInfoAfterArgumentsInParenthesis

        resolvedArgumentsInParenthesis.forEach { it.setResultDataFlowInfoIfRelevant(resultDataFlowInfo) }
        resolvedExternalArgument?.setResultDataFlowInfoIfRelevant(resultDataFlowInfo)

        val isForImplicitInvoke = oldCall is CallTransformer.CallForImplicitInvoke

        return PSIKotlinCallImpl(
            kotlinCallKind, oldCall, tracingStrategy, resolvedExplicitReceiver, dispatchReceiverForInvoke, name,
            resolvedTypeArguments, resolvedArgumentsInParenthesis, resolvedExternalArgument, context.dataFlowInfo, resultDataFlowInfo,
            context.dataFlowInfoForArguments, isForImplicitInvoke
        )
    }

    private fun resolveDispatchReceiverForInvoke(
        context: BasicCallResolutionContext,
        kotlinCallKind: KotlinCallKind,
        oldCall: Call
    ): ReceiverKotlinCallArgument? {
        if (kotlinCallKind != KotlinCallKind.INVOKE) return null

        require(oldCall is CallTransformer.CallForImplicitInvoke) { "Call should be CallForImplicitInvoke, but it is: $oldCall" }

        return resolveReceiver(context, oldCall.dispatchReceiver, isSafeCall = false, isForImplicitInvoke = true)
    }

    private fun resolveReceiver(
        context: BasicCallResolutionContext,
        oldReceiver: Receiver?,
        isSafeCall: Boolean,
        isForImplicitInvoke: Boolean
    ): ReceiverKotlinCallArgument? {
        return when (oldReceiver) {
            null -> null

            is QualifierReceiver -> QualifierReceiverKotlinCallArgument(oldReceiver) // todo report warning if isSafeCall

            is ReceiverValue -> {
                if (oldReceiver is ExpressionReceiver) {
                    val ktExpression = KtPsiUtil.getLastElementDeparenthesized(oldReceiver.expression, context.statementFilter)

                    val bindingContext = context.trace.bindingContext
                    val call =
                        bindingContext[BindingContext.DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL, ktExpression]
                            ?: ktExpression?.getCall(bindingContext)

                    val partiallyResolvedCall = call?.let { bindingContext.get(BindingContext.ONLY_RESOLVED_CALL, it)?.result }

                    if (partiallyResolvedCall != null) {
                        val receiver = ReceiverValueWithSmartCastInfo(oldReceiver, emptySet(), isStable = true)
                        return SubKotlinCallArgumentImpl(
                            CallMaker.makeExternalValueArgument(oldReceiver.expression),
                            context.dataFlowInfo, context.dataFlowInfo, receiver, partiallyResolvedCall
                        )
                    }
                }

                ReceiverExpressionKotlinCallArgument(
                    context.transformToReceiverWithSmartCastInfo(oldReceiver),
                    isSafeCall,
                    isForImplicitInvoke
                )
            }

            else -> error("Incorrect receiver: $oldReceiver")
        }
    }

    private fun resolveTypeArguments(context: BasicCallResolutionContext, typeArguments: List<KtTypeProjection>): List<TypeArgument> =
        typeArguments.map { projection ->
            if (projection.projectionKind != KtProjectionKind.NONE) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection))
            }
            ModifierCheckerCore.check(projection, context.trace, null, languageVersionSettings)

            resolveType(context, projection.typeReference, typeResolver)?.let { SimpleTypeArgumentImpl(projection.typeReference!!, it) }
                ?: TypeArgumentPlaceholder
        }

    private fun resolveArgumentsInParenthesis(
        context: BasicCallResolutionContext,
        arguments: List<ValueArgument>
    ): List<KotlinCallArgument> {
        val dataFlowInfoForArguments = context.dataFlowInfoForArguments
        return arguments.map { argument ->
            resolveValueArgument(context, dataFlowInfoForArguments.getInfo(argument), argument).also { resolvedArgument ->
                dataFlowInfoForArguments.updateInfo(argument, resolvedArgument.dataFlowInfoAfterThisArgument)
            }
        }
    }

    private fun resolveValueArgument(
        outerCallContext: BasicCallResolutionContext,
        startDataFlowInfo: DataFlowInfo,
        valueArgument: ValueArgument
    ): PSIKotlinCallArgument {
        val builtIns = outerCallContext.scope.ownerDescriptor.builtIns

        fun createParseErrorElement() = ParseErrorKotlinCallArgument(valueArgument, startDataFlowInfo, builtIns)

        val argumentExpression = valueArgument.getArgumentExpression() ?: return createParseErrorElement()
        val ktExpression = KtPsiUtil.deparenthesize(argumentExpression) ?: createParseErrorElement()

        val argumentName = valueArgument.getArgumentName()?.asName

        processFunctionalExpression(
            outerCallContext, argumentExpression, startDataFlowInfo,
            valueArgument, argumentName, builtIns, typeResolver
        )?.let {
            return it
        }

        if (ktExpression is KtCollectionLiteralExpression) {
            return CollectionLiteralKotlinCallArgumentImpl(
                valueArgument, argumentName, startDataFlowInfo, startDataFlowInfo, ktExpression, outerCallContext
            )
        }

        val context = outerCallContext.replaceContextDependency(ContextDependency.DEPENDENT)
            .replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceDataFlowInfo(startDataFlowInfo)
            .expandContextForCatchClause(ktExpression)

        if (ktExpression is KtCallableReferenceExpression) {
            return createCallableReferenceKotlinCallArgument(
                context, ktExpression, startDataFlowInfo, valueArgument, argumentName,
                outerCallContext
            )
        }

        // argumentExpression instead of ktExpression is hack -- type info should be stored also for parenthesized expression
        val typeInfo = expressionTypingServices.getTypeInfo(argumentExpression, context)
        return createSimplePSICallArgument(context, valueArgument, typeInfo) ?: createParseErrorElement()
    }

    fun createCallableReferenceKotlinCallArgument(
        context: BasicCallResolutionContext,
        ktExpression: KtCallableReferenceExpression,
        startDataFlowInfo: DataFlowInfo,
        valueArgument: ValueArgument,
        argumentName: Name?,
        outerCallContext: BasicCallResolutionContext
    ): CallableReferenceKotlinCallArgumentImpl {
        checkNoSpread(outerCallContext, valueArgument)

        val expressionTypingContext = ExpressionTypingContext.newContext(context)
        val lhsResult = if (ktExpression.isEmptyLHS) null else doubleColonExpressionResolver.resolveDoubleColonLHS(
            ktExpression,
            expressionTypingContext
        )
        val newDataFlowInfo = (lhsResult as? DoubleColonLHS.Expression)?.dataFlowInfo ?: startDataFlowInfo
        val name = ktExpression.callableReference.getReferencedNameAsName()

        val lhsNewResult = when (lhsResult) {
            null -> LHSResult.Empty
            is DoubleColonLHS.Expression -> {
                if (lhsResult.isObjectQualifier) {
                    val classifier = lhsResult.type.constructor.declarationDescriptor
                    val calleeExpression = ktExpression.receiverExpression?.getCalleeExpressionIfAny()
                    if (calleeExpression is KtSimpleNameExpression && classifier is ClassDescriptor) {
                        LHSResult.Object(ClassQualifier(calleeExpression, classifier))
                    } else {
                        LHSResult.Error
                    }
                } else {
                    val fakeArgument = FakeValueArgumentForLeftCallableReference(ktExpression)

                    val kotlinCallArgument = createSimplePSICallArgument(context, fakeArgument, lhsResult.typeInfo)
                    kotlinCallArgument?.let { LHSResult.Expression(it as SimpleKotlinCallArgument) } ?: LHSResult.Error
                }
            }
            is DoubleColonLHS.Type -> {
                val qualifiedExpression = ktExpression.receiverExpression!!
                val qualifier = expressionTypingContext.trace.get(BindingContext.QUALIFIER, qualifiedExpression)
                LHSResult.Type(qualifier, lhsResult.type.unwrap())
            }
        }

        return CallableReferenceKotlinCallArgumentImpl(
            ASTScopeTower(context), valueArgument, startDataFlowInfo, newDataFlowInfo,
            ktExpression, argumentName, lhsNewResult, name
        )
    }


    private fun BasicCallResolutionContext.expandContextForCatchClause(ktExpression: Any): BasicCallResolutionContext {
        if (ktExpression !is KtExpression) return this

        val variableDescriptorHolder = trace.bindingContext[NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER, ktExpression] ?: return this
        val variableDescriptor = variableDescriptorHolder.get() ?: return this
        variableDescriptorHolder.set(null)

        val redeclarationChecker = expressionTypingServices.createLocalRedeclarationChecker(trace)

        val catchScope = with(scope) {
            LexicalWritableScope(this, ownerDescriptor, false, redeclarationChecker, LexicalScopeKind.CATCH)
        }
        catchScope.addVariableDescriptor(variableDescriptor)
        return replaceScope(catchScope)
    }
}
