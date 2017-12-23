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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.EffectSystem
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.KotlinCallResolver
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isBinaryRemOperator
import org.jetbrains.kotlin.resolve.calls.callUtil.createLookupLocation
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.buildResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallableDescriptors
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.*
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
        private val constantExpressionEvaluator: ConstantExpressionEvaluator
) {
    private val GIVEN_CANDIDATES_NAME = Name.special("<given candidates>")

    fun <D : CallableDescriptor> runResolutionAndInference(
            context: BasicCallResolutionContext,
            name: Name,
            resolutionKind: NewResolutionOldInference.ResolutionKind<D>,
            tracingStrategy: TracingStrategy
    ) : OverloadResolutionResults<D> {
        val isBinaryRemOperator = isBinaryRemOperator(context.call)
        val refinedName = refineNameForRemOperator(isBinaryRemOperator, name)

        val kotlinCall = toKotlinCall(context, resolutionKind.kotlinCallKind, context.call, refinedName, tracingStrategy)
        val scopeTower = ASTScopeTower(context)
        val resolutionCallbacks = createResolutionCallbacks(context)

        val factoryProviderForInvoke = FactoryProviderForInvoke(context, scopeTower, kotlinCall)

        val expectedType = calculateExpectedType(context)
        var result = kotlinCallResolver.resolveCall(
                scopeTower, resolutionCallbacks, kotlinCall, expectedType, factoryProviderForInvoke, context.collectAllCandidates)

        val shouldUseOperatorRem = languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)
        if (isBinaryRemOperator && shouldUseOperatorRem && (result.isEmpty() || result.areAllInapplicable())) {
            result = resolveToDeprecatedMod(name, context, resolutionKind, tracingStrategy, scopeTower, resolutionCallbacks, expectedType)
        }

        if (result.isEmpty() && reportAdditionalDiagnosticIfNoCandidates(context, scopeTower, resolutionKind.kotlinCallKind, kotlinCall)) {
            return OverloadResolutionResultsImpl.nameNotFound()
        }

        return convertToOverloadResolutionResults(context, result, tracingStrategy)
    }

    // actually, `D` is at least FunctionDescriptor, but right now because of CallResolver it isn't possible change upper bound for `D`
    fun <D : CallableDescriptor> runResolutionAndInferenceForGivenCandidates(
            context: BasicCallResolutionContext,
            resolutionCandidates: Collection<ResolutionCandidate<D>>,
            tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        val dispatchReceiver = resolutionCandidates.firstNotNullResult { it.dispatchReceiver }

        val kotlinCall = toKotlinCall(context, KotlinCallKind.FUNCTION, context.call, GIVEN_CANDIDATES_NAME, tracingStrategy, dispatchReceiver)
        val scopeTower = ASTScopeTower(context)
        val resolutionCallbacks = createResolutionCallbacks(context)

        val givenCandidates = resolutionCandidates.map {
            GivenCandidate(it.descriptor as FunctionDescriptor,
                           it.dispatchReceiver?.let { context.transformToReceiverWithSmartCastInfo(it) },
                           it.knownTypeParametersResultingSubstitutor)
        }

        val result = kotlinCallResolver.resolveGivenCandidates(
                scopeTower, resolutionCallbacks, kotlinCall, calculateExpectedType(context), givenCandidates, context.collectAllCandidates)
        return convertToOverloadResolutionResults(context, result, tracingStrategy)

    }

    private fun <D : CallableDescriptor> resolveToDeprecatedMod(
            remOperatorName: Name,
            context: BasicCallResolutionContext,
            resolutionKind: NewResolutionOldInference.ResolutionKind<D>,
            tracingStrategy: TracingStrategy,
            scopeTower: ImplicitScopeTower,
            resolutionCallbacks: KotlinResolutionCallbacksImpl,
            expectedType: UnwrappedType?
    ): CallResolutionResult {
        val deprecatedName = OperatorConventions.REM_TO_MOD_OPERATION_NAMES[remOperatorName]!!
        val callWithDeprecatedName = toKotlinCall(context, resolutionKind.kotlinCallKind, context.call, deprecatedName, tracingStrategy)
        val refinedProviderForInvokeFactory = FactoryProviderForInvoke(context, scopeTower, callWithDeprecatedName)
        return kotlinCallResolver.resolveCall(scopeTower, resolutionCallbacks, callWithDeprecatedName, expectedType,
                                              refinedProviderForInvokeFactory, context.collectAllCandidates)
    }

    private fun refineNameForRemOperator(isBinaryRemOperator: Boolean, name: Name): Name {
        val shouldUseOperatorRem = languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)
        return if (isBinaryRemOperator && !shouldUseOperatorRem) OperatorConventions.REM_TO_MOD_OPERATION_NAMES[name]!! else name
    }

    private fun createResolutionCallbacks(context: BasicCallResolutionContext) =
            KotlinResolutionCallbacksImpl(context, expressionTypingServices, typeApproximator,
                                          argumentTypeResolver, languageVersionSettings, kotlinToResolvedCallTransformer,
                                          constantExpressionEvaluator)

    private fun calculateExpectedType(context: BasicCallResolutionContext): UnwrappedType? {
        val expectedType = context.expectedType.unwrap()

        return if (context.contextDependency == ContextDependency.DEPENDENT) {
            assert(TypeUtils.noExpectedType(expectedType)) {
                "Should have no expected type, got: $expectedType"
            }
            null
        }
        else {
            if (expectedType.isError) TypeUtils.NO_EXPECTED_TYPE else expectedType
        }
    }

    private fun <D : CallableDescriptor> convertToOverloadResolutionResults(
            context: BasicCallResolutionContext,
            result: CallResolutionResult,
            tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        if (result.type == CallResolutionResult.Type.ALL_CANDIDATES) {
            val resolvedCalls = result.allCandidates?.map {
                val resultingSubstitutor = it.getSystem().asReadOnlyStorage().buildResultingSubstitutor()
                kotlinToResolvedCallTransformer.transformToResolvedCall<D>(it.resolvedCall, null, resultingSubstitutor, result.diagnostics)
            }

            return AllCandidates(resolvedCalls ?: emptyList())
        }

        val trace = context.trace

        result.diagnostics.firstIsInstanceOrNull<NoneCandidatesCallDiagnostic>()?.let {
            tracingStrategy.unresolvedReference(trace)
            return OverloadResolutionResultsImpl.nameNotFound()
        }

        result.diagnostics.firstIsInstanceOrNull<ManyCandidatesCallDiagnostic>()?.let {
            val resolvedCalls = it.candidates.map { kotlinToResolvedCallTransformer.onlyTransform<D>(it.resolvedCall, emptyList()) }
            if (it.candidates.areAllFailed()) {
                tracingStrategy.noneApplicable(trace, resolvedCalls)
                tracingStrategy.recordAmbiguity(trace, resolvedCalls)
            }
            else {
                tracingStrategy.recordAmbiguity(trace, resolvedCalls)
                if (resolvedCalls.first().status == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE) {
                    tracingStrategy.cannotCompleteResolve(trace, resolvedCalls)
                }
                else {
                    tracingStrategy.ambiguity(trace, resolvedCalls)
                }
            }
            return ManyCandidates(resolvedCalls)
        }

        val isInapplicableReceiver = getResultApplicability(result.diagnostics) == ResolutionCandidateApplicability.INAPPLICABLE_WRONG_RECEIVER

        val resolvedCall = if (isInapplicableReceiver) {
            val singleCandidate = result.resultCallAtom ?: error("Should be not null for result: $result")
            kotlinToResolvedCallTransformer.onlyTransform<D>(singleCandidate, result.diagnostics).also {
                tracingStrategy.unresolvedReferenceWrongReceiver(trace, listOf(it))
            }
        }
        else {
            kotlinToResolvedCallTransformer.transformAndReport<D>(result, context)
        }

        // NB. Be careful with moving this invocation, as effect system expects resolution results to be written in trace
        // (see EffectSystem for details)
        resolvedCall.recordEffects(trace)

        return SingleOverloadResolutionResult(resolvedCall)
    }

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
                context.trace.report(Errors.RESOLUTION_TO_CLASSIFIER.on(reference, candidate.descriptor, candidate.kind, candidate.errorMessage))
                return true
            }
        }
        return false
    }


    private inner class ASTScopeTower(
            val context: BasicCallResolutionContext
    ): ImplicitScopeTower {
        // todo may be for invoke for case variable + invoke we should create separate dynamicScope(by newCall for invoke)
        override val dynamicScope: MemberScope = dynamicCallableDescriptors.createDynamicDescriptorScope(context.call, context.scope.ownerDescriptor)
        // same for location
        override val location: LookupLocation = context.call.createLookupLocation()

        override val syntheticScopes: SyntheticScopes get() = this@PSICallResolver.syntheticScopes
        override val isDebuggerContext: Boolean get() = context.isDebuggerContext
        override val isNewInferenceEnabled: Boolean get() = context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
        override val lexicalScope: LexicalScope get() = context.scope
        private val cache = HashMap<ReceiverParameterDescriptor, ReceiverValueWithSmartCastInfo>()

        override fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo? {
            val implicitReceiver = scope.implicitReceiver ?: return null

            return cache.getOrPut(implicitReceiver) {
                context.transformToReceiverWithSmartCastInfo(implicitReceiver.value)
            }
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
            return SimpleCandidateFactory(callComponents, scopeTower, variableCall)
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
            }
            else {
                PSIKotlinCallForInvoke(kotlinCall, variable, variableCallArgument, null)
            }

            return variableCallArgument.receiver to SimpleCandidateFactory(callComponents, scopeTower, callForInvoke)
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
                return ReceiverExpressionKotlinCallArgument(createReceiverValueWithSmartCastInfo(variable), isVariableReceiverForInvoke = true)
            }

            val psiKotlinCall = variable.resolvedCall.atom.psiKotlinCall

            val variableResult = CallResolutionResult(CallResolutionResult.Type.PARTIAL, variable.resolvedCall, listOf(), variable.getSystem().asReadOnlyStorage())
            return SubKotlinCallArgumentImpl(CallMaker.makeExternalValueArgument((variableReceiver.receiverValue as ExpressionReceiver).expression),
                                      psiKotlinCall.resultDataFlowInfo, psiKotlinCall.resultDataFlowInfo, variableReceiver,
                                      variableResult)
        }

        // todo: decrease hacks count
        private fun createReceiverValueWithSmartCastInfo(variable: KotlinResolutionCandidate): ReceiverValueWithSmartCastInfo {
            val callForVariable = variable.resolvedCall.atom as PSIKotlinCallForVariable
            val calleeExpression = callForVariable.baseCall.psiCall.calleeExpression as? KtReferenceExpression ?:
                                   error("Unexpected call : ${callForVariable.baseCall.psiCall}")

            val temporaryTrace = TemporaryBindingTrace.create(context.trace, "Context for resolve candidate")

            val type = variable.resolvedCall.freshReturnType!!
            val variableReceiver = ExpressionReceiver.create(calleeExpression, type, temporaryTrace.bindingContext)

            temporaryTrace.record(BindingContext.REFERENCE_TARGET, calleeExpression, variable.resolvedCall.candidateDescriptor)
            val dataFlowValue = DataFlowValueFactory.createDataFlowValue(variableReceiver, temporaryTrace.bindingContext, context.scope.ownerDescriptor)
            return ReceiverValueWithSmartCastInfo(
                    variableReceiver,
                    context.dataFlowInfo.getCollectedTypes(dataFlowValue, context.languageVersionSettings),
                    dataFlowValue.isStable
            )
        }
    }


    private fun toKotlinCall(
            context: BasicCallResolutionContext,
            kotlinCallKind: KotlinCallKind,
            oldCall: Call,
            name: Name,
            tracingStrategy: TracingStrategy,
            forcedExplicitReceiver: Receiver? = null
    ): PSIKotlinCallImpl {
        val resolvedExplicitReceiver = resolveExplicitReceiver(context, forcedExplicitReceiver?: oldCall.explicitReceiver, oldCall.isSafeCall())
        val resolvedTypeArguments = resolveTypeArguments(context, oldCall.typeArguments)

        val argumentsInParenthesis = if (oldCall.callType != Call.CallType.ARRAY_SET_METHOD && oldCall.functionLiteralArguments.isEmpty()) {
            oldCall.valueArguments
        }
        else {
            oldCall.valueArguments.dropLast(1)
        }

        val externalLambdaArguments = oldCall.functionLiteralArguments
        val resolvedArgumentsInParenthesis = resolveArgumentsInParenthesis(context, argumentsInParenthesis)

        val externalArgument = if (oldCall.callType == Call.CallType.ARRAY_SET_METHOD) {
            assert(externalLambdaArguments.isEmpty()) {
                "Unexpected lambda parameters for call $oldCall"
            }
            oldCall.valueArguments.last()
        }
        else {
            if (externalLambdaArguments.size > 2) {
                externalLambdaArguments.drop(1).forEach {
                    context.trace.report(Errors.MANY_LAMBDA_EXPRESSION_ARGUMENTS.on(it.getLambdaExpression()))
                }
            }

            externalLambdaArguments.firstOrNull()
        }

        val dataFlowInfoAfterArgumentsInParenthesis =
                if (externalArgument != null && resolvedArgumentsInParenthesis.isNotEmpty())
                    resolvedArgumentsInParenthesis.last().psiCallArgument.dataFlowInfoAfterThisArgument
                else
                    context.dataFlowInfoForArguments.resultInfo

        val astExternalArgument = externalArgument?.let { resolveValueArgument(context, dataFlowInfoAfterArgumentsInParenthesis, it) }
        val resultDataFlowInfo = astExternalArgument?.dataFlowInfoAfterThisArgument ?: dataFlowInfoAfterArgumentsInParenthesis

        resolvedArgumentsInParenthesis.forEach { it.setResultDataFlowInfoIfRelevant(resultDataFlowInfo) }
        astExternalArgument?.setResultDataFlowInfoIfRelevant(resultDataFlowInfo)

        return PSIKotlinCallImpl(kotlinCallKind, oldCall, tracingStrategy, resolvedExplicitReceiver, name, resolvedTypeArguments, resolvedArgumentsInParenthesis,
                                 astExternalArgument, context.dataFlowInfo, resultDataFlowInfo, context.dataFlowInfoForArguments)
    }

    private fun resolveExplicitReceiver(context: BasicCallResolutionContext, oldReceiver: Receiver?, isSafeCall: Boolean): ReceiverKotlinCallArgument? =
            when(oldReceiver) {
                null -> null
                is QualifierReceiver -> QualifierReceiverKotlinCallArgument(oldReceiver) // todo report warning if isSafeCall
                is ReceiverValue -> {
                    val detailedReceiver = context.transformToReceiverWithSmartCastInfo(oldReceiver)

                    var subCallArgument: ReceiverKotlinCallArgument? = null
                    if (oldReceiver is ExpressionReceiver) {
                        val ktExpression = KtPsiUtil.getLastElementDeparenthesized(oldReceiver.expression, context.statementFilter)

                        val bindingContext = context.trace.bindingContext
                        val call = bindingContext[BindingContext.DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL, ktExpression]
                                   ?: ktExpression?.getCall(bindingContext)

                        val onlyResolvedCall = call?.let {
                            bindingContext.get(BindingContext.ONLY_RESOLVED_CALL, it)
                        }
                        if (onlyResolvedCall != null) {
                            subCallArgument = SubKotlinCallArgumentImpl(CallMaker.makeExternalValueArgument(oldReceiver.expression),
                                                      context.dataFlowInfo, context.dataFlowInfo, detailedReceiver, onlyResolvedCall)

                        }
                    }

                    subCallArgument ?: ReceiverExpressionKotlinCallArgument(detailedReceiver, isSafeCall)
                }
                else -> error("Incorrect receiver: $oldReceiver")
            }

    private fun resolveType(context: BasicCallResolutionContext, typeReference: KtTypeReference?): UnwrappedType? {
        if (typeReference == null) return null

        val type = typeResolver.resolveType(context.scope, typeReference, context.trace, checkBounds = true)
        ForceResolveUtil.forceResolveAllContents(type)
        return type.unwrap()
    }

    private fun resolveTypeArguments(context: BasicCallResolutionContext, typeArguments: List<KtTypeProjection>): List<TypeArgument> =
            typeArguments.map { projection ->
                if (projection.projectionKind != KtProjectionKind.NONE) {
                    context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection))
                }
                ModifierCheckerCore.check(projection, context.trace, null, languageVersionSettings)

                resolveType(context, projection.typeReference)?.let { SimpleTypeArgumentImpl(projection.typeReference!!, it) }  ?: TypeArgumentPlaceholder
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
        val parseErrorArgument = ParseErrorKotlinCallArgument(valueArgument, startDataFlowInfo, builtIns)
        val argumentExpression = valueArgument.getArgumentExpression() ?: return parseErrorArgument

        val ktExpression = KtPsiUtil.deparenthesize(argumentExpression) ?: parseErrorArgument

        val argumentName = valueArgument.getArgumentName()?.asName

        processFunctionalExpression(outerCallContext, argumentExpression, startDataFlowInfo, valueArgument, argumentName, builtIns)?.let {
            return it
        }

        if (ktExpression is KtCollectionLiteralExpression) {
            return CollectionLiteralKotlinCallArgumentImpl(
                    valueArgument, argumentName, startDataFlowInfo, startDataFlowInfo, ktExpression, outerCallContext)
        }

        val context = outerCallContext.replaceContextDependency(ContextDependency.DEPENDENT)
                .replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceDataFlowInfo(startDataFlowInfo)

        if (ktExpression is KtCallableReferenceExpression) {
            checkNoSpread(outerCallContext, valueArgument)

            val expressionTypingContext = ExpressionTypingContext.newContext(context)
            val lhsResult = if (ktExpression.isEmptyLHS) null else doubleColonExpressionResolver.resolveDoubleColonLHS(ktExpression, expressionTypingContext)
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
                        }
                        else {
                            LHSResult.Error
                        }
                    }
                    else {
                        val fakeArgument = FakeValueArgumentForLeftCallableReference(ktExpression)

                        val kotlinCallArgument = createSimplePSICallArgument(context, fakeArgument, lhsResult.typeInfo)
                        kotlinCallArgument?.let { LHSResult.Expression(it as SimpleKotlinCallArgument) } ?: LHSResult.Error
                    }
                }
                is DoubleColonLHS.Type -> {
                    val qualifiedExpression = ktExpression.receiverExpression!!.let { it.referenceExpression() ?: it }
                    val qualifier = expressionTypingContext.trace.get(BindingContext.QUALIFIER, qualifiedExpression)
                    if (qualifier is ClassQualifier) {
                        LHSResult.Type(qualifier, lhsResult.type.unwrap())
                    }
                    else {
                        LHSResult.Error
                    }
                }
            }

            return CallableReferenceKotlinCallArgumentImpl(ASTScopeTower(context), valueArgument, startDataFlowInfo, newDataFlowInfo,
                                                           ktExpression, argumentName, lhsNewResult, name)
        }

        // argumentExpression instead of ktExpression is hack -- type info should be stored also for parenthesized expression
        val typeInfo = expressionTypingServices.getTypeInfo(argumentExpression, context)
        return createSimplePSICallArgument(context, valueArgument, typeInfo) ?: parseErrorArgument
    }

    private fun processFunctionalExpression(
            outerCallContext: BasicCallResolutionContext,
            argumentExpression: KtExpression,
            startDataFlowInfo: DataFlowInfo,
            valueArgument: ValueArgument,
            argumentName: Name?,
            builtIns: KotlinBuiltIns
    ): PSIKotlinCallArgument? {
        val expression = ArgumentTypeResolver.getFunctionLiteralArgumentIfAny(argumentExpression, outerCallContext) ?: return null
        val postponedExpression = if (expression is KtFunctionLiteral) expression.getParentOfType<KtLambdaExpression>(true) else expression

        val lambdaArgument: PSIKotlinCallArgument? = when (postponedExpression) {
            is KtLambdaExpression ->
                LambdaKotlinCallArgumentImpl(outerCallContext, valueArgument, startDataFlowInfo, argumentName, postponedExpression,
                                             argumentExpression, resolveParametersTypes(outerCallContext, postponedExpression.functionLiteral))

            is KtNamedFunction -> {
                val receiverType = resolveType(outerCallContext, postponedExpression.receiverTypeReference)
                val parametersTypes = resolveParametersTypes(outerCallContext, postponedExpression) ?: emptyArray()
                val returnType = resolveType(outerCallContext, postponedExpression.typeReference) ?:
                                 if (postponedExpression.hasBlockBody()) builtIns.unitType else null

                FunctionExpressionImpl(outerCallContext, valueArgument, startDataFlowInfo, argumentName,
                                       argumentExpression, postponedExpression, receiverType, parametersTypes, returnType)
            }

            else -> return null
        }

        checkNoSpread(outerCallContext, valueArgument)

        return lambdaArgument
    }

    private fun checkNoSpread(context: BasicCallResolutionContext, valueArgument: ValueArgument) {
        valueArgument.getSpreadElement()?.let {
            context.trace.report(Errors.SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE.on(it))
        }
    }

    private fun resolveParametersTypes(context: BasicCallResolutionContext, ktFunction: KtFunction): Array<UnwrappedType?>? {
        val parameterList = ktFunction.valueParameterList ?: return null

        return Array(parameterList.parameters.size) {
            parameterList.parameters[it]?.typeReference?.let { resolveType(context, it) }
        }
    }
}
