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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.*
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.callUtil.isFakeElement
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.expressions.DataFlowAnalyzer
import java.util.*
import kotlin.collections.HashMap


class KotlinToResolvedCallTransformer(
        private val callCheckers: Iterable<CallChecker>,
        private val languageFeatureSettings: LanguageVersionSettings,
        private val dataFlowAnalyzer: DataFlowAnalyzer,
        private val argumentTypeResolver: ArgumentTypeResolver
) {

    fun <D : CallableDescriptor> transformAndReport(
            baseResolvedCall: ResolvedKotlinCall,
            context: BasicCallResolutionContext,
            trace: BindingTrace? // if trace is not null then all information will be reported to this trace
    ): ResolvedCall<D> {
        if (baseResolvedCall is ResolvedKotlinCall.CompletedResolvedKotlinCall) {
            baseResolvedCall.allInnerCalls.forEach { transformAndReportCompletedCall<D>(it, context, trace) }
            return transformAndReportCompletedCall(baseResolvedCall.completedCall, context, trace)
        }

        val onlyResolvedCall = (baseResolvedCall as ResolvedKotlinCall.OnlyResolvedKotlinCall)
        trace?.record(BindingContext.ONLY_RESOLVED_CALL, onlyResolvedCall.candidate.kotlinCall.psiKotlinCall.psiCall, onlyResolvedCall)

        return StubOnlyResolvedCall(onlyResolvedCall.candidate.lastCall)
    }

    private fun <D : CallableDescriptor> transformAndReportCompletedCall(
            completedCall: CompletedKotlinCall,
            context: BasicCallResolutionContext,
            trace: BindingTrace?
    ): ResolvedCall<D> {
        fun <C> C.runIfTraceNotNull(action: (BasicCallResolutionContext, BindingTrace, C) -> Unit): C {
            if (trace != null) action(context, trace, this)
            return this
        }

        val resolvedCall = when (completedCall) {
            is CompletedKotlinCall.Simple -> {
                NewResolvedCallImpl<D>(completedCall).runIfTraceNotNull(this::bindResolvedCall).runIfTraceNotNull(this::runArgumentsChecks)
            }
            is CompletedKotlinCall.VariableAsFunction -> {
                val resolvedCall = NewVariableAsFunctionResolvedCallImpl(
                        completedCall,
                        NewResolvedCallImpl(completedCall.variableCall),
                        NewResolvedCallImpl<FunctionDescriptor>(completedCall.invokeCall).runIfTraceNotNull(this::runArgumentsChecks)
                ).runIfTraceNotNull(this::bindResolvedCall)

                @Suppress("UNCHECKED_CAST")
                (resolvedCall as ResolvedCall<D>)
            }
        }
        runCallCheckers(resolvedCall, context)

        return resolvedCall
    }

    private fun runCallCheckers(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext) {
        val calleeExpression = if (resolvedCall is VariableAsFunctionResolvedCall)
            resolvedCall.variableCall.call.calleeExpression
        else
            resolvedCall.call.calleeExpression
        val reportOn =
                if (calleeExpression != null && !calleeExpression.isFakeElement) calleeExpression
                else resolvedCall.call.callElement

        val callCheckerContext = CallCheckerContext(context, languageFeatureSettings)
        for (callChecker in callCheckers) {
            callChecker.check(resolvedCall, reportOn, callCheckerContext)
        }
    }


    // todo very beginning code
    private fun runArgumentsChecks(
            context: BasicCallResolutionContext,
            trace: BindingTrace,
            resolvedCall: NewResolvedCallImpl<*>
    ) {

        for (valueArgument in resolvedCall.call.valueArguments) {
            val argumentMapping = resolvedCall.getArgumentMapping(valueArgument!!)
            val (expectedType, callPosition) = when (argumentMapping) {
                is ArgumentMatch -> Pair(
                        getEffectiveExpectedType(argumentMapping.valueParameter, valueArgument),
                        CallPosition.ValueArgumentPosition(resolvedCall, argumentMapping.valueParameter, valueArgument))
                else -> Pair(TypeUtils.NO_EXPECTED_TYPE, CallPosition.Unknown)
            }
            val newContext =
                    context.replaceDataFlowInfo(resolvedCall.dataFlowInfoForArguments.getInfo(valueArgument))
                            .replaceExpectedType(expectedType)
                            .replaceCallPosition(callPosition)
                            .replaceBindingTrace(trace)

            // todo
//            if (valueArgument.isExternal()) continue

            val deparenthesized = valueArgument.getArgumentExpression()?.let {
                KtPsiUtil.getLastElementDeparenthesized(it, context.statementFilter)
            } ?: continue

            var recordedType = context.trace.getType(deparenthesized)

            // For the cases like 'foo(1)' the type of '1' depends on expected type (it can be Int, Byte, etc.),
            // so while the expected type is not known, it's IntegerValueType(1), and should be updated when the expected type is known.
            if (recordedType != null && !recordedType.constructor.isDenotable) {
                recordedType = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(newContext, deparenthesized) ?: recordedType
            }

//            dataFlowAnalyzer.checkType(recordedType, deparenthesized, newContext)
        }

    }

    private fun bindResolvedCall(context: BasicCallResolutionContext, trace: BindingTrace, simpleResolvedCall: NewResolvedCallImpl<*>) {
        reportCallDiagnostic(context, trace, simpleResolvedCall.completedCall)
        val tracing = simpleResolvedCall.completedCall.kotlinCall.psiKotlinCall.tracingStrategy

        tracing.bindReference(trace, simpleResolvedCall)
        tracing.bindResolvedCall(trace, simpleResolvedCall)
    }

    private fun bindResolvedCall(context: BasicCallResolutionContext, trace: BindingTrace, variableAsFunction: NewVariableAsFunctionResolvedCallImpl) {
        reportCallDiagnostic(context, trace, variableAsFunction.variableCall.completedCall)
        reportCallDiagnostic(context, trace, variableAsFunction.functionCall.completedCall)

        val outerTracingStrategy = variableAsFunction.completedCall.kotlinCall.psiKotlinCall.tracingStrategy
        outerTracingStrategy.bindReference(trace, variableAsFunction.variableCall)
        outerTracingStrategy.bindResolvedCall(trace, variableAsFunction)
        variableAsFunction.functionCall.kotlinCall.psiKotlinCall.tracingStrategy.bindReference(trace, variableAsFunction.functionCall)
    }

    private fun reportCallDiagnostic(
            context: BasicCallResolutionContext,
            trace: BindingTrace,
            completedCall: CompletedKotlinCall.Simple
    ) {
        var reported: Boolean
        val reportTrackedTrace = object : BindingTrace by trace {
            override fun report(diagnostic: Diagnostic) {
                trace.report(diagnostic)
                reported = true
            }
        }
        val diagnosticReporter = DiagnosticReporterByTrackingStrategy(context, reportTrackedTrace, completedCall.kotlinCall.psiKotlinCall)

        for (diagnostic in completedCall.resolutionStatus.diagnostics) {
            reported = false
            diagnostic.report(diagnosticReporter)
            if (!reported && REPORT_MISSING_NEW_INFERENCE_DIAGNOSTIC) {
                if (diagnostic.candidateApplicability.isSuccess) {
                    trace.report(Errors.NEW_INFERENCE_DIAGNOSTIC.on(diagnosticReporter.psiKotlinCall.psiCall.callElement, "Missing diagnostic: $diagnostic"))
                }
                else {
                    trace.report(Errors.NEW_INFERENCE_ERROR.on(diagnosticReporter.psiKotlinCall.psiCall.callElement, "Missing diagnostic: $diagnostic"))
                }
            }
        }
    }
}

sealed class NewAbstractResolvedCall<D : CallableDescriptor>(): ResolvedCall<D> {
    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    abstract val kotlinCall: KotlinCall

    private var argumentToParameterMap: Map<ValueArgument, ArgumentMatchImpl>? = null
    private val _valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument> by lazy(this::createValueArguments)

    override fun getCall(): Call = kotlinCall.psiKotlinCall.psiCall

    override fun getValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> = _valueArguments

    override fun getValueArgumentsByIndex(): List<ResolvedValueArgument>? {
        val arguments = ArrayList<ResolvedValueArgument?>(candidateDescriptor.valueParameters.size)
        for (i in 0..candidateDescriptor.valueParameters.size - 1) {
            arguments.add(null)
        }

        for ((parameterDescriptor, value) in valueArguments) {
            val oldValue = arguments.set(parameterDescriptor.index, value)
            if (oldValue != null) {
                return null
            }
        }

        if (arguments.any { it == null }) return null

        @Suppress("UNCHECKED_CAST")
        return arguments as List<ResolvedValueArgument>
    }

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping {
        if (argumentToParameterMap == null) {
            argumentToParameterMap = argumentToParameterMap(resultingDescriptor, valueArguments)
        }
        val argumentMatch = argumentToParameterMap!![valueArgument] ?: return ArgumentUnmapped
        return argumentMatch
    }

    override fun getDataFlowInfoForArguments() = object : DataFlowInfoForArguments {
        override fun getResultInfo() = kotlinCall.psiKotlinCall.resultDataFlowInfo
        override fun getInfo(valueArgument: ValueArgument): DataFlowInfo {
            val externalPsiCallArgument = kotlinCall.externalArgument?.psiCallArgument
            if (externalPsiCallArgument?.valueArgument == valueArgument) {
                return externalPsiCallArgument.dataFlowInfoAfterThisArgument
            }
            kotlinCall.argumentsInParenthesis.find { it.psiCallArgument.valueArgument == valueArgument }?.let {
                return it.psiCallArgument.dataFlowInfoAfterThisArgument
            }

            // valueArgument is not found
            // may be we should return initial DataFlowInfo but I think that it isn't important
            return kotlinCall.psiKotlinCall.resultDataFlowInfo
        }
    }

    private fun argumentToParameterMap(
            resultingDescriptor: CallableDescriptor,
            valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ): Map<ValueArgument, ArgumentMatchImpl> =
            HashMap<ValueArgument, ArgumentMatchImpl>().also { result ->
                for (parameter in resultingDescriptor.valueParameters) {
                    val resolvedArgument = valueArguments[parameter] ?: continue
                    for (arguments in resolvedArgument.arguments) {
                        result[arguments] = ArgumentMatchImpl(parameter).apply { recordMatchStatus(ArgumentMatchStatus.SUCCESS) }
                    }
                }
            }

    private fun createValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> {
        val result = HashMap<ValueParameterDescriptor, ResolvedValueArgument>()
        for (parameter in candidateDescriptor.valueParameters) {
            val resolvedCallArgument = argumentMappingByOriginal[parameter.original] ?: continue
            val valueArgument = when (resolvedCallArgument) {
                ResolvedCallArgument.DefaultArgument -> DefaultValueArgument.DEFAULT
                is ResolvedCallArgument.SimpleArgument -> ExpressionValueArgument(resolvedCallArgument.callArgument.psiCallArgument.valueArgument)
                is ResolvedCallArgument.VarargArgument -> VarargValueArgument().apply {
                    resolvedCallArgument.arguments.map { it.psiCallArgument.valueArgument }.forEach(this::addArgument)
                }
            }
            result[parameter] = valueArgument
        }

        return result
    }
}

class NewResolvedCallImpl<D : CallableDescriptor>(
        val completedCall: CompletedKotlinCall.Simple
): NewAbstractResolvedCall<D>() {
    override val kotlinCall: KotlinCall get() = completedCall.kotlinCall

    override fun getStatus(): ResolutionStatus = completedCall.resolutionStatus.resultingApplicability.toResolutionStatus()

    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
        get() = completedCall.argumentMappingByOriginal

    override fun getCandidateDescriptor(): D = completedCall.candidateDescriptor as D
    override fun getResultingDescriptor(): D = completedCall.resultingDescriptor as D
    override fun getExtensionReceiver(): ReceiverValue? = completedCall.extensionReceiver?.receiverValue
    override fun getDispatchReceiver(): ReceiverValue? = completedCall.dispatchReceiver?.receiverValue
    override fun getExplicitReceiverKind(): ExplicitReceiverKind = completedCall.explicitReceiverKind

    override fun getTypeArguments(): Map<TypeParameterDescriptor, KotlinType> {
        val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()

        val result = HashMap<TypeParameterDescriptor, UnwrappedType>()
        for ((parameter, argument) in typeParameters.zip(completedCall.typeArguments)) {
            result[parameter] = argument
        }
        return result
    }

    override fun getSmartCastDispatchReceiverType(): KotlinType? = null // todo

    fun ResolutionCandidateApplicability.toResolutionStatus(): ResolutionStatus = when (this) {
        ResolutionCandidateApplicability.RESOLVED, ResolutionCandidateApplicability.RESOLVED_LOW_PRIORITY -> ResolutionStatus.SUCCESS
        else -> ResolutionStatus.OTHER_ERROR
    }
}

class NewVariableAsFunctionResolvedCallImpl(
        val completedCall: CompletedKotlinCall.VariableAsFunction,
        override val variableCall: NewResolvedCallImpl<VariableDescriptor>,
        override val functionCall: NewResolvedCallImpl<FunctionDescriptor>
): VariableAsFunctionResolvedCall, ResolvedCall<FunctionDescriptor> by functionCall

class StubOnlyResolvedCall<D : CallableDescriptor>(val candidate: SimpleKotlinResolutionCandidate): NewAbstractResolvedCall<D>() {
    override fun getStatus() = ResolutionStatus.UNKNOWN_STATUS

    override fun getCandidateDescriptor(): D = candidate.candidateDescriptor as D
    override fun getResultingDescriptor(): D = candidateDescriptor
    override fun getExtensionReceiver() = candidate.extensionReceiver?.receiver?.receiverValue
    override fun getDispatchReceiver() = candidate.dispatchReceiverArgument?.receiver?.receiverValue
    override fun getExplicitReceiverKind() = candidate.explicitReceiverKind

    override fun getTypeArguments(): Map<TypeParameterDescriptor, KotlinType> = emptyMap()

    override fun getSmartCastDispatchReceiverType(): KotlinType? = null

    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
        get() = candidate.argumentMappingByOriginal
    override val kotlinCall: KotlinCall get() = candidate.kotlinCall
}