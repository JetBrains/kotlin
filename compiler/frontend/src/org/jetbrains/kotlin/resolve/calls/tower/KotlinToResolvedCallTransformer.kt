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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.DiagnosticReporterByTrackingStrategy
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isFakeElement
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.components.AdditionalDiagnosticReporter
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.inference.buildResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.inference.substituteAndApproximateCapturedTypes
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.makeNullableTypeIfSafeReceiver
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.scopes.receivers.CastImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.expressions.DataFlowAnalyzer
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*


class KotlinToResolvedCallTransformer(
        private val callCheckers: Iterable<CallChecker>,
        private val languageFeatureSettings: LanguageVersionSettings,
        private val dataFlowAnalyzer: DataFlowAnalyzer,
        private val argumentTypeResolver: ArgumentTypeResolver,
        private val constantExpressionEvaluator: ConstantExpressionEvaluator,
        private val deprecationResolver: DeprecationResolver,
        private val expressionTypingServices: ExpressionTypingServices,
        private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
        private val additionalDiagnosticReporter: AdditionalDiagnosticReporter
) {

    companion object {
        private val REPORT_MISSING_NEW_INFERENCE_DIAGNOSTIC
            get() = false
    }

    fun <D : CallableDescriptor> onlyTransform(
            resolvedCallAtom: ResolvedCallAtom
    ): ResolvedCall<D> = transformToResolvedCall(resolvedCallAtom, null)

    fun <D : CallableDescriptor> transformAndReport(
            baseResolvedCall: CallResolutionResult,
            context: BasicCallResolutionContext
    ): ResolvedCall<D> {
        val candidate = baseResolvedCall.resultCallAtom!!
        when (baseResolvedCall.type) {
            CallResolutionResult.Type.PARTIAL -> {
                context.trace.record(BindingContext.ONLY_RESOLVED_CALL, candidate.atom.psiKotlinCall.psiCall, baseResolvedCall)

                return createStubResolvedCallAndWriteItToTrace(candidate, context.trace)
            }
            CallResolutionResult.Type.ERROR, CallResolutionResult.Type.COMPLETED -> {
                val resultSubstitutor = baseResolvedCall.constraintSystem.buildResultingSubstitutor()
                val ktPrimitiveCompleter = ResolvedAtomCompleter(resultSubstitutor, context.trace, context, this,
                                                                 expressionTypingServices, argumentTypeResolver, doubleColonExpressionResolver,
                                                                 languageFeatureSettings, deprecationResolver)

                for (subKtPrimitive in candidate.subResolvedAtoms) {
                    ktPrimitiveCompleter.completeAll(subKtPrimitive)
                }

                return ktPrimitiveCompleter.completeResolvedCall(candidate) as ResolvedCall<D>
            }
            CallResolutionResult.Type.ALL_CANDIDATES -> error("Cannot transform result for ALL_CANDIDATES mode")
        }
    }

    fun <D : CallableDescriptor> createStubResolvedCallAndWriteItToTrace(candidate: ResolvedCallAtom, trace: BindingTrace): ResolvedCall<D> {
        val result = transformToResolvedCall<D>(candidate, trace)
        val psiKotlinCall = candidate.atom.psiKotlinCall
        val tracing = psiKotlinCall.safeAs<PSIKotlinCallForInvoke>()?.baseCall?.tracingStrategy ?: psiKotlinCall.tracingStrategy

        tracing.bindReference(trace, result)
        tracing.bindResolvedCall(trace, result)
        return result
    }

    fun <D : CallableDescriptor> transformToResolvedCall(
            completedCallAtom: ResolvedCallAtom,
            trace: BindingTrace?,
            resultSubstitutor: NewTypeSubstitutor? = null // if substitutor is not null, it means that this call is completed
    ): ResolvedCall<D> {
        val psiKotlinCall = completedCallAtom.atom.psiKotlinCall
        return if (psiKotlinCall is PSIKotlinCallForInvoke) {
            @Suppress("UNCHECKED_CAST")
            NewVariableAsFunctionResolvedCallImpl(
                    createOrGet(psiKotlinCall.variableCall.resolvedCall, trace, resultSubstitutor),
                    createOrGet(completedCallAtom, trace, resultSubstitutor)
            ) as ResolvedCall<D>
        }
        else {
            createOrGet(completedCallAtom, trace, resultSubstitutor)
        }
    }

    private fun <D : CallableDescriptor> createOrGet(
            completedSimpleAtom: ResolvedCallAtom,
            trace: BindingTrace?,
            resultSubstitutor: NewTypeSubstitutor?
    ): NewResolvedCallImpl<D> {
        if (trace != null) {
            val storedResolvedCall = completedSimpleAtom.atom.psiKotlinCall.psiCall.getResolvedCall(trace.bindingContext)?.
                    safeAs<NewResolvedCallImpl<D>>()
            if (storedResolvedCall != null) {
                storedResolvedCall.setResultingSubstitutor(resultSubstitutor)
                return storedResolvedCall
            }
        }
        return NewResolvedCallImpl(completedSimpleAtom, resultSubstitutor)
    }

    fun runCallCheckers(resolvedCall: ResolvedCall<*>, callCheckerContext: CallCheckerContext) {
        val calleeExpression = if (resolvedCall is VariableAsFunctionResolvedCall)
            resolvedCall.variableCall.call.calleeExpression
        else
            resolvedCall.call.calleeExpression
        val reportOn =
                if (calleeExpression != null && !calleeExpression.isFakeElement) calleeExpression
                else resolvedCall.call.callElement

        for (callChecker in callCheckers) {
            callChecker.check(resolvedCall, reportOn, callCheckerContext)

            if (resolvedCall is VariableAsFunctionResolvedCall) {
                callChecker.check(resolvedCall.variableCall, reportOn, callCheckerContext)
            }
        }
    }

    // todo very beginning code
    fun runArgumentsChecks(
            context: BasicCallResolutionContext,
            trace: BindingTrace,
            resolvedCall: NewResolvedCallImpl<*>
    ) {

        for (valueArgument in resolvedCall.call.valueArguments) {
            val argumentMapping = resolvedCall.getArgumentMapping(valueArgument!!)
            val (expectedType, callPosition) = when (argumentMapping) {
                is ArgumentMatch -> Pair(
                        getEffectiveExpectedType(argumentMapping.valueParameter, valueArgument, context),
                        CallPosition.ValueArgumentPosition(resolvedCall, argumentMapping.valueParameter, valueArgument))
                else -> Pair(TypeUtils.NO_EXPECTED_TYPE, CallPosition.Unknown)
            }
            val newContext =
                    context.replaceDataFlowInfo(resolvedCall.dataFlowInfoForArguments.getInfo(valueArgument))
                            .replaceExpectedType(expectedType)
                            .replaceCallPosition(callPosition)
                            .replaceBindingTrace(trace)

            // todo external argument

            val argumentExpression = valueArgument.getArgumentExpression() ?: continue
            updateRecordedType(argumentExpression, newContext)
        }

    }

    fun updateRecordedType(
            expression: KtExpression,
            context: BasicCallResolutionContext
    ): KotlinType? {
        val deparenthesized = expression.let {
            KtPsiUtil.getLastElementDeparenthesized(it, context.statementFilter)
        } ?: return null

        val recordedType = context.trace.getType(deparenthesized)
        var updatedType = getResolvedCallForArgumentExpression(deparenthesized, context)?.run {
            makeNullableTypeIfSafeReceiver(resultingDescriptor.returnType, context)
        } ?: recordedType

        // For the cases like 'foo(1)' the type of '1' depends on expected type (it can be Int, Byte, etc.),
        // so while the expected type is not known, it's IntegerValueType(1), and should be updated when the expected type is known.
        if (recordedType != null && !recordedType.constructor.isDenotable) {
            updatedType = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(context, deparenthesized) ?: updatedType
        }

        updatedType = updateRecordedTypeForArgument(updatedType, recordedType, expression, context)

        dataFlowAnalyzer.checkType(updatedType, deparenthesized, context)

        return updatedType
    }

    private fun getResolvedCallForArgumentExpression(expression: KtExpression, context: BasicCallResolutionContext) =
            if (!ExpressionTypingUtils.dependsOnExpectedType(expression))
                null
            else
                expression.getResolvedCall(context.trace.bindingContext)

    // See CallCompleter#updateRecordedTypeForArgument
    private fun updateRecordedTypeForArgument(
            updatedType: KotlinType?,
            recordedType: KotlinType?,
            argumentExpression: KtExpression,
            context: BasicCallResolutionContext
    ): KotlinType? {
        if ((!ErrorUtils.containsErrorType(recordedType) && recordedType == updatedType) || updatedType == null) return updatedType

        val expressions = ArrayList<KtExpression>().also { expressions ->
            var expression: KtExpression? = argumentExpression
            while (expression != null) {
                expressions.add(expression)
                expression = deparenthesizeOrGetSelector(expression, context.statementFilter)
            }
            expressions.reverse()
        }

        var shouldBeMadeNullable: Boolean = false
        for (expression in expressions) {
            if (!(expression is KtParenthesizedExpression || expression is KtLabeledExpression || expression is KtAnnotatedExpression)) {
                shouldBeMadeNullable = hasNecessarySafeCall(expression, context.trace)
            }
            BindingContextUtils.updateRecordedType(updatedType, expression, context.trace, shouldBeMadeNullable)
        }

        return context.trace.getType(argumentExpression)
    }

    private fun deparenthesizeOrGetSelector(expression: KtExpression, statementFilter: StatementFilter): KtExpression? {
        val deparenthesized = KtPsiUtil.deparenthesizeOnce(expression)
        if (deparenthesized != expression) return deparenthesized

        return when (expression) {
            is KtBlockExpression -> statementFilter.getLastStatementInABlock(expression)
            is KtQualifiedExpression -> expression.selectorExpression
            else -> null
        }
    }

    private fun hasNecessarySafeCall(expression: KtExpression, trace: BindingTrace): Boolean {
        // We are interested in type of the last call:
        // 'a.b?.foo()' is safe call, but 'a?.b.foo()' is not.
        // Since receiver is 'a.b' and selector is 'foo()',
        // we can only check if an expression is safe call.
        if (expression !is KtSafeQualifiedExpression) return false

        //If a receiver type is not null, then this safe expression is useless, and we don't need to make the result type nullable.
        val expressionType = trace.getType(expression.receiverExpression)
        return expressionType != null && TypeUtils.isNullableType(expressionType)
    }

    internal fun bindAndReport(context: BasicCallResolutionContext, trace: BindingTrace, resolvedCall: ResolvedCall<*>) {
        resolvedCall.safeAs<NewResolvedCallImpl<*>>()?.let { bindAndReport(context, trace, it) }
        resolvedCall.safeAs<NewVariableAsFunctionResolvedCallImpl>()?.let { bindAndReport(context, trace, it) }
    }

    private fun bindAndReport(context: BasicCallResolutionContext, trace: BindingTrace, simpleResolvedCall: NewResolvedCallImpl<*>) {
        val tracing = simpleResolvedCall.resolvedCallAtom.atom.psiKotlinCall.tracingStrategy

        tracing.bindReference(trace, simpleResolvedCall)
        tracing.bindResolvedCall(trace, simpleResolvedCall)

        reportCallDiagnostic(context, trace, simpleResolvedCall.resolvedCallAtom, simpleResolvedCall.resultingDescriptor)
    }

    private fun bindAndReport(context: BasicCallResolutionContext, trace: BindingTrace, variableAsFunction: NewVariableAsFunctionResolvedCallImpl) {
        val outerTracingStrategy = variableAsFunction.baseCall.tracingStrategy
        outerTracingStrategy.bindReference(trace, variableAsFunction.variableCall)
        outerTracingStrategy.bindResolvedCall(trace, variableAsFunction)
        variableAsFunction.functionCall.kotlinCall.psiKotlinCall.tracingStrategy.bindReference(trace, variableAsFunction.functionCall)

        reportCallDiagnostic(context, trace, variableAsFunction.variableCall.resolvedCallAtom, variableAsFunction.variableCall.resultingDescriptor)
        reportCallDiagnostic(context, trace, variableAsFunction.functionCall.resolvedCallAtom, variableAsFunction.functionCall.resultingDescriptor)
    }

    private fun reportCallDiagnostic(
            context: BasicCallResolutionContext,
            trace: BindingTrace,
            completedCallAtom: ResolvedCallAtom,
            resultingDescriptor: CallableDescriptor
    ) {
        val trackingTrace = TrackingBindingTrace(trace)
        val newContext = context.replaceBindingTrace(trackingTrace)
        val diagnosticReporter = DiagnosticReporterByTrackingStrategy(constantExpressionEvaluator, newContext, completedCallAtom.atom.psiKotlinCall)

        val diagnosticHolder = KotlinDiagnosticsHolder.SimpleHolder()
        additionalDiagnosticReporter.reportAdditionalDiagnostics(completedCallAtom, resultingDescriptor, diagnosticHolder)

        for (diagnostic in completedCallAtom.diagnostics + diagnosticHolder.getDiagnostics()) {
            trackingTrace.reported = false
            diagnostic.report(diagnosticReporter)

            val dontRecordToTraceAsIs = diagnostic is ResolutionDiagnostic && diagnostic !is VisibilityError
            val shouldReportMissingDiagnostic = !trackingTrace.reported && !dontRecordToTraceAsIs
            if (shouldReportMissingDiagnostic && REPORT_MISSING_NEW_INFERENCE_DIAGNOSTIC) {
                val factory = if (diagnostic.candidateApplicability.isSuccess) Errors.NEW_INFERENCE_DIAGNOSTIC else Errors.NEW_INFERENCE_ERROR
                trace.report(factory.on(diagnosticReporter.psiKotlinCall.psiCall.callElement, "Missing diagnostic: $diagnostic"))
            }
        }
    }
}

class TrackingBindingTrace(val trace: BindingTrace) : BindingTrace by trace {
    var reported: Boolean = false

    override fun report(diagnostic: Diagnostic) {
        trace.report(diagnostic)
        reported = true
    }

    fun markAsReported() {
        reported = true
    }
}

sealed class NewAbstractResolvedCall<D : CallableDescriptor>(): ResolvedCall<D> {
    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    abstract val kotlinCall: KotlinCall

    protected var argumentToParameterMap: Map<ValueArgument, ArgumentMatchImpl>? = null
    protected var _valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>? = null

    override fun getCall(): Call = kotlinCall.psiKotlinCall.psiCall

    override fun getValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> {
        if (_valueArguments == null) {
            _valueArguments = createValueArguments()
        }
        return _valueArguments!!
    }

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
        return argumentToParameterMap!![valueArgument] ?: ArgumentUnmapped
    }

    override fun getDataFlowInfoForArguments() = object : DataFlowInfoForArguments {
        override fun getResultInfo() = kotlinCall.psiKotlinCall.resultDataFlowInfo
        override fun getInfo(valueArgument: ValueArgument): DataFlowInfo {
            val externalPsiCallArgument = kotlinCall.externalArgument?.psiCallArgument
            if (externalPsiCallArgument?.valueArgument == valueArgument) {
                return externalPsiCallArgument.dataFlowInfoAfterThisArgument
            }
            return kotlinCall.psiKotlinCall.dataFlowInfoForArguments.getInfo(valueArgument)
        }
    }

    private fun argumentToParameterMap(
            resultingDescriptor: CallableDescriptor,
            valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ): Map<ValueArgument, ArgumentMatchImpl> =
            LinkedHashMap<ValueArgument, ArgumentMatchImpl>().also { result ->
                for (parameter in resultingDescriptor.valueParameters) {
                    val resolvedArgument = valueArguments[parameter] ?: continue
                    for (arguments in resolvedArgument.arguments) {
                        result[arguments] = ArgumentMatchImpl(parameter).apply { recordMatchStatus(ArgumentMatchStatus.SUCCESS) }
                    }
                }
            }

    private fun createValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> =
            LinkedHashMap<ValueParameterDescriptor, ResolvedValueArgument>().also { result ->
                for ((originalParameter, resolvedCallArgument) in argumentMappingByOriginal) {
                    val resultingParameter = resultingDescriptor.valueParameters[originalParameter.index]
                    result[resultingParameter] = when (resolvedCallArgument) {
                        ResolvedCallArgument.DefaultArgument ->
                            DefaultValueArgument.DEFAULT
                        is ResolvedCallArgument.SimpleArgument -> {
                            val valueArgument = resolvedCallArgument.callArgument.psiCallArgument.valueArgument
                            if (resultingParameter.isVararg)
                                VarargValueArgument().apply { addArgument(valueArgument) }
                            else
                                ExpressionValueArgument(valueArgument)
                        }
                        is ResolvedCallArgument.VarargArgument ->
                            VarargValueArgument().apply {
                                resolvedCallArgument.arguments.map { it.psiCallArgument.valueArgument }.forEach { addArgument(it) }
                            }
                    }
                }
            }

}

class NewResolvedCallImpl<D : CallableDescriptor>(
        val resolvedCallAtom: ResolvedCallAtom,
        substitutor: NewTypeSubstitutor?
): NewAbstractResolvedCall<D>() {
    var isCompleted = false
        private set
    private lateinit var resultingDescriptor: D

    private lateinit var typeArguments: List<UnwrappedType>

    private var extensionReceiver = resolvedCallAtom.extensionReceiverArgument?.receiver?.receiverValue
    private var smartCastDispatchReceiverType: KotlinType? = null

    override val kotlinCall: KotlinCall get() = resolvedCallAtom.atom

    override fun getStatus(): ResolutionStatus = getResultApplicability(resolvedCallAtom.diagnostics).toResolutionStatus()

    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
        get() = resolvedCallAtom.argumentMappingByOriginal

    override fun getCandidateDescriptor(): D = resolvedCallAtom.candidateDescriptor as D
    override fun getResultingDescriptor(): D = resultingDescriptor
    override fun getExtensionReceiver(): ReceiverValue? = extensionReceiver
    override fun getDispatchReceiver(): ReceiverValue? = resolvedCallAtom.dispatchReceiverArgument?.receiver?.receiverValue
    override fun getExplicitReceiverKind(): ExplicitReceiverKind = resolvedCallAtom.explicitReceiverKind

    override fun getTypeArguments(): Map<TypeParameterDescriptor, KotlinType> {
        val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()
        return typeParameters.zip(typeArguments).toMap()
    }

    override fun getSmartCastDispatchReceiverType(): KotlinType? = smartCastDispatchReceiverType

    fun updateExtensionReceiverWithSmartCastIfNeeded(smartCastExtensionReceiverType: KotlinType) {
        if (extensionReceiver is ImplicitClassReceiver) {
            extensionReceiver = CastImplicitClassReceiver(
                    (extensionReceiver as ImplicitClassReceiver).classDescriptor,
                    smartCastExtensionReceiverType
            )
        }
    }

    fun setSmartCastDispatchReceiverType(smartCastDispatchReceiverType: KotlinType) {
        this.smartCastDispatchReceiverType = smartCastDispatchReceiverType
    }

    fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        //clear cached values
        argumentToParameterMap = null
        _valueArguments = null
        if (substitutor != null) {
            // todo: add asset that we do not complete call many times
            isCompleted = true
        }

        resultingDescriptor = run {
            val candidateDescriptor = resolvedCallAtom.candidateDescriptor
            val containsCapturedTypes = resolvedCallAtom.candidateDescriptor.returnType?.contains { it is NewCapturedType } ?: false

            when {
                candidateDescriptor is FunctionDescriptor ||
                (candidateDescriptor is PropertyDescriptor && (candidateDescriptor.typeParameters.isNotEmpty() || containsCapturedTypes)) ->
                    // this code is very suspicious. Now it is very useful for BE, because they cannot do nothing with captured types,
                    // but it seems like temporary solution.
                    candidateDescriptor.substitute(resolvedCallAtom.substitutor).substituteAndApproximateCapturedTypes(
                            substitutor ?: FreshVariableNewTypeSubstitutor.Empty)
                else ->
                    candidateDescriptor
            }
        } as D

        typeArguments = resolvedCallAtom.substitutor.freshVariables.map {
            val substituted = (substitutor ?: FreshVariableNewTypeSubstitutor.Empty).safeSubstitute(it.defaultType)
            TypeApproximator().approximateToSuperType(substituted, TypeApproximatorConfiguration.CapturedTypesApproximation) ?: substituted
        }
    }

    init {
        setResultingSubstitutor(substitutor)
    }
}

fun ResolutionCandidateApplicability.toResolutionStatus(): ResolutionStatus = when (this) {
    ResolutionCandidateApplicability.RESOLVED, ResolutionCandidateApplicability.RESOLVED_LOW_PRIORITY -> ResolutionStatus.SUCCESS
    ResolutionCandidateApplicability.INAPPLICABLE_WRONG_RECEIVER -> ResolutionStatus.RECEIVER_TYPE_ERROR
    else -> ResolutionStatus.OTHER_ERROR
}

class NewVariableAsFunctionResolvedCallImpl(
        override val variableCall: NewResolvedCallImpl<VariableDescriptor>,
        override val functionCall: NewResolvedCallImpl<FunctionDescriptor>
): VariableAsFunctionResolvedCall, ResolvedCall<FunctionDescriptor> by functionCall {
    val baseCall get() = functionCall.resolvedCallAtom.atom.psiKotlinCall.cast<PSIKotlinCallForInvoke>().baseCall
}

fun ResolvedCall<*>.isNewNotCompleted(): Boolean {
    if (this is NewVariableAsFunctionResolvedCallImpl) {
        return !functionCall.isCompleted
    }
    if (this is NewResolvedCallImpl<*>) {
        return !isCompleted
    }
    return false
}
