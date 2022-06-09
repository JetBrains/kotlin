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

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.model.*
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
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.compactIfPossible
import org.jetbrains.kotlin.utils.sure

class NewResolutionOldInference(
    private val deprecationResolver: DeprecationResolver,
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
        ): MyCandidate = TODO() // to be removed

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
        ): MyCandidate = TODO() // to be removed

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
