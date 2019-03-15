/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.DiagnosticReporterByTrackingStrategy
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isFakeElement
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.components.AdditionalDiagnosticReporter
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.inference.buildResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.inference.substituteAndApproximateCapturedTypes
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.makeNullableTypeIfSafeReceiver
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
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
    private val additionalTypeCheckers: Iterable<AdditionalTypeChecker>,
    private val dataFlowAnalyzer: DataFlowAnalyzer,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val deprecationResolver: DeprecationResolver,
    private val expressionTypingServices: ExpressionTypingServices,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val additionalDiagnosticReporter: AdditionalDiagnosticReporter,
    private val moduleDescriptor: ModuleDescriptor,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val builtIns: KotlinBuiltIns
) {
    companion object {
        private val REPORT_MISSING_NEW_INFERENCE_DIAGNOSTIC
            get() = false
    }

    fun <D : CallableDescriptor> onlyTransform(
        resolvedCallAtom: ResolvedCallAtom,
        diagnostics: Collection<KotlinCallDiagnostic>
    ): ResolvedCall<D> = transformToResolvedCall(resolvedCallAtom, null, null, diagnostics)

    fun <D : CallableDescriptor> transformAndReport(
        baseResolvedCall: CallResolutionResult,
        context: BasicCallResolutionContext,
        tracingStrategy: TracingStrategy
    ): ResolvedCall<D> {
        return when (baseResolvedCall) {
            is PartialCallResolutionResult -> {
                val candidate = baseResolvedCall.resultCallAtom

                val psiKotlinCall = candidate.atom.psiKotlinCall
                val psiCall = if (psiKotlinCall is PSIKotlinCallForInvoke)
                    psiKotlinCall.baseCall.psiCall
                else
                    psiKotlinCall.psiCall

                context.trace.record(BindingContext.ONLY_RESOLVED_CALL, psiCall, baseResolvedCall)
                context.trace.record(BindingContext.PARTIAL_CALL_RESOLUTION_CONTEXT, psiCall, context)

                context.inferenceSession.addPartialCallInfo(
                    PSIPartialCallInfo(baseResolvedCall, context, tracingStrategy)
                )

                createStubResolvedCallAndWriteItToTrace(candidate, context.trace, baseResolvedCall.diagnostics)
            }

            is CompletedCallResolutionResult, is ErrorCallResolutionResult -> {
                val candidate = (baseResolvedCall as SingleCallResolutionResult).resultCallAtom

                if (baseResolvedCall is CompletedCallResolutionResult) {
                    context.inferenceSession.addCompletedCallInfo(PSICompletedCallInfo(baseResolvedCall, context, tracingStrategy))
                }

                if (context.inferenceSession.writeOnlyStubs()) {
                    return createStubResolvedCallAndWriteItToTrace(
                        candidate,
                        context.trace,
                        baseResolvedCall.diagnostics,
                        completedCall = true
                    )
                }

                val resultSubstitutor = baseResolvedCall.constraintSystem.buildResultingSubstitutor()
                val ktPrimitiveCompleter = ResolvedAtomCompleter(
                    resultSubstitutor, context, this, expressionTypingServices, argumentTypeResolver,
                    doubleColonExpressionResolver, builtIns, deprecationResolver, moduleDescriptor, dataFlowValueFactory
                )

                for (subKtPrimitive in candidate.subResolvedAtoms) {
                    ktPrimitiveCompleter.completeAll(subKtPrimitive)
                }

                ktPrimitiveCompleter.completeResolvedCall(candidate, baseResolvedCall.diagnostics) as ResolvedCall<D>
            }

            is SingleCallResolutionResult -> error("Call resolution result for one candidate didn't transformed: $baseResolvedCall")
            is AllCandidatesResolutionResult -> error("Cannot transform result for ALL_CANDIDATES mode")
        }
    }

    fun <D : CallableDescriptor> createStubResolvedCallAndWriteItToTrace(
        candidate: ResolvedCallAtom,
        trace: BindingTrace,
        diagnostics: Collection<KotlinCallDiagnostic>,
        completedCall: Boolean = false
    ): ResolvedCall<D> {
        val substitutor = if (completedCall) NewTypeSubstitutorByConstructorMap(emptyMap()) else null
        val result = transformToResolvedCall<D>(candidate, trace, substitutor, diagnostics)
        val psiKotlinCall = candidate.atom.psiKotlinCall
        val tracing = psiKotlinCall.safeAs<PSIKotlinCallForInvoke>()?.baseCall?.tracingStrategy ?: psiKotlinCall.tracingStrategy

        tracing.bindReference(trace, result)
        tracing.bindResolvedCall(trace, result)
        return result
    }

    fun <D : CallableDescriptor> transformToResolvedCall(
        completedCallAtom: ResolvedCallAtom,
        trace: BindingTrace?,
        resultSubstitutor: NewTypeSubstitutor? = null, // if substitutor is not null, it means that this call is completed
        diagnostics: Collection<KotlinCallDiagnostic>
    ): ResolvedCall<D> {
        val psiKotlinCall = completedCallAtom.atom.psiKotlinCall
        return if (psiKotlinCall is PSIKotlinCallForInvoke) {
            @Suppress("UNCHECKED_CAST")
            NewVariableAsFunctionResolvedCallImpl(
                createOrGet(psiKotlinCall.variableCall.resolvedCall, trace, resultSubstitutor, diagnostics),
                createOrGet(completedCallAtom, trace, resultSubstitutor, diagnostics)
            ) as ResolvedCall<D>
        } else {
            createOrGet(completedCallAtom, trace, resultSubstitutor, diagnostics)
        }
    }

    private fun <D : CallableDescriptor> createOrGet(
        completedSimpleAtom: ResolvedCallAtom,
        trace: BindingTrace?,
        resultSubstitutor: NewTypeSubstitutor?,
        diagnostics: Collection<KotlinCallDiagnostic>
    ): NewResolvedCallImpl<D> {
        if (trace != null) {
            val storedResolvedCall = completedSimpleAtom.atom.psiKotlinCall.getResolvedPsiKotlinCall<D>(trace)
            if (storedResolvedCall != null) {
                storedResolvedCall.setResultingSubstitutor(resultSubstitutor)
                storedResolvedCall.updateDiagnostics(diagnostics)
                return storedResolvedCall
            }
        }
        return NewResolvedCallImpl(completedSimpleAtom, resultSubstitutor, diagnostics)
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

    fun runAdditionalReceiversCheckers(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext) {
        context.checkReceiver(
            resolvedCall,
            resolvedCall.resultingDescriptor.extensionReceiverParameter,
            resolvedCall.extensionReceiver,
            resolvedCall.explicitReceiverKind.isExtensionReceiver,
            implicitInvokeCheck = false
        )
        context.checkReceiver(
            resolvedCall,
            resolvedCall.resultingDescriptor.dispatchReceiverParameter,
            resolvedCall.dispatchReceiver,
            resolvedCall.explicitReceiverKind.isDispatchReceiver,
            implicitInvokeCheck = context.call is CallTransformer.CallForImplicitInvoke
        )

    }

    private fun BasicCallResolutionContext.checkReceiver(
        resolvedCall: ResolvedCall<*>,
        receiverParameter: ReceiverParameterDescriptor?,
        receiverArgument: ReceiverValue?,
        isExplicitReceiver: Boolean,
        implicitInvokeCheck: Boolean
    ) {
        if (receiverParameter == null || receiverArgument == null) return
        val safeAccess = isExplicitReceiver && !implicitInvokeCheck && resolvedCall.call.isSemanticallyEquivalentToSafeCall
        additionalTypeCheckers.forEach { it.checkReceiver(receiverParameter, receiverArgument, safeAccess, this) }
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
                is ArgumentMatch -> {
                    val expectedType = resolvedCall.getExpectedTypeForSamConvertedArgument(valueArgument)
                                ?: getEffectiveExpectedType(argumentMapping.valueParameter, valueArgument, context)
                    Pair(
                        expectedType,
                        CallPosition.ValueArgumentPosition(resolvedCall, argumentMapping.valueParameter, valueArgument)
                    )
                }
                else -> Pair(TypeUtils.NO_EXPECTED_TYPE, CallPosition.Unknown)
            }
            val newContext =
                context.replaceDataFlowInfo(resolvedCall.dataFlowInfoForArguments.getInfo(valueArgument))
                    .replaceExpectedType(expectedType)
                    .replaceCallPosition(callPosition)
                    .replaceBindingTrace(trace)

            // todo external argument

            val argumentExpression = valueArgument.getArgumentExpression() ?: continue
            updateRecordedType(argumentExpression, newContext, resolvedCall.isReallySuccess())
        }

    }

    fun updateRecordedType(
        expression: KtExpression,
        context: BasicCallResolutionContext,
        reportErrorForTypeMismatch: Boolean
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

        dataFlowAnalyzer.checkType(updatedType, deparenthesized, context, reportErrorForTypeMismatch)

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

    internal fun bindAndReport(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        resolvedCall: ResolvedCall<*>,
        diagnostics: Collection<KotlinCallDiagnostic>
    ) {
        resolvedCall.safeAs<NewResolvedCallImpl<*>>()?.let { bindAndReport(context, trace, it, diagnostics) }
        resolvedCall.safeAs<NewVariableAsFunctionResolvedCallImpl>()?.let { bindAndReport(context, trace, it, diagnostics) }
    }

    private fun bindAndReport(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        simpleResolvedCall: NewResolvedCallImpl<*>,
        diagnostics: Collection<KotlinCallDiagnostic>
    ) {
        val tracing = simpleResolvedCall.resolvedCallAtom.atom.psiKotlinCall.tracingStrategy

        tracing.bindReference(trace, simpleResolvedCall)
        tracing.bindResolvedCall(trace, simpleResolvedCall)

        reportCallDiagnostic(context, trace, simpleResolvedCall.resolvedCallAtom, simpleResolvedCall.resultingDescriptor, diagnostics)
    }

    private fun bindAndReport(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        variableAsFunction: NewVariableAsFunctionResolvedCallImpl,
        diagnostics: Collection<KotlinCallDiagnostic>
    ) {
        val outerTracingStrategy = variableAsFunction.baseCall.tracingStrategy
        val variableCall = variableAsFunction.variableCall
        val functionCall = variableAsFunction.functionCall

        outerTracingStrategy.bindReference(trace, variableCall)
        outerTracingStrategy.bindResolvedCall(trace, variableAsFunction)
        functionCall.kotlinCall.psiKotlinCall.tracingStrategy.bindReference(trace, functionCall)

        reportCallDiagnostic(context, trace, variableCall.resolvedCallAtom, variableCall.resultingDescriptor, diagnostics)
        reportCallDiagnostic(context, trace, functionCall.resolvedCallAtom, functionCall.resultingDescriptor, emptyList())
    }

    fun reportCallDiagnostic(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        completedCallAtom: ResolvedCallAtom,
        resultingDescriptor: CallableDescriptor,
        diagnostics: Collection<KotlinCallDiagnostic>
    ) {
        val trackingTrace = TrackingBindingTrace(trace)
        val newContext = context.replaceBindingTrace(trackingTrace)
        val diagnosticReporter =
            DiagnosticReporterByTrackingStrategy(constantExpressionEvaluator, newContext, completedCallAtom.atom.psiKotlinCall, context.dataFlowValueFactory)

        val diagnosticHolder = KotlinDiagnosticsHolder.SimpleHolder()
        additionalDiagnosticReporter.reportAdditionalDiagnostics(completedCallAtom, resultingDescriptor, diagnosticHolder, diagnostics)

        for (diagnostic in diagnostics + diagnosticHolder.getDiagnostics()) {
            trackingTrace.reported = false
            diagnostic.report(diagnosticReporter)

            if (diagnostic is ResolvedUsingDeprecatedVisibility) {
                reportResolvedUsingDeprecatedVisibility(
                    completedCallAtom.atom.psiKotlinCall.psiCall, completedCallAtom.candidateDescriptor,
                    resultingDescriptor, diagnostic, trace
                )
            }

            val dontRecordToTraceAsIs = diagnostic is ResolutionDiagnostic && diagnostic !is VisibilityError
            val shouldReportMissingDiagnostic = !trackingTrace.reported && !dontRecordToTraceAsIs
            if (shouldReportMissingDiagnostic && REPORT_MISSING_NEW_INFERENCE_DIAGNOSTIC) {
                val factory =
                    if (diagnostic.candidateApplicability.isSuccess) Errors.NEW_INFERENCE_DIAGNOSTIC else Errors.NEW_INFERENCE_ERROR
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

sealed class NewAbstractResolvedCall<D : CallableDescriptor>() : ResolvedCall<D> {
    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    abstract val kotlinCall: KotlinCall

    protected var argumentToParameterMap: Map<ValueArgument, ArgumentMatchImpl>? = null
    protected var _valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>? = null

    private var nonTrivialUpdatedResultInfo: DataFlowInfo? = null

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
        override fun getResultInfo(): DataFlowInfo = nonTrivialUpdatedResultInfo ?: kotlinCall.psiKotlinCall.resultDataFlowInfo

        override fun getInfo(valueArgument: ValueArgument): DataFlowInfo {
            val externalPsiCallArgument = kotlinCall.externalArgument?.psiCallArgument
            if (externalPsiCallArgument?.valueArgument == valueArgument) {
                return externalPsiCallArgument.dataFlowInfoAfterThisArgument
            }
            return kotlinCall.psiKotlinCall.dataFlowInfoForArguments.getInfo(valueArgument)
        }
    }

    // Currently, updated only with info from effect system
    internal fun updateResultingDataFlowInfo(dataFlowInfo: DataFlowInfo) {
        if (dataFlowInfo == DataFlowInfo.EMPTY) return
        assert(nonTrivialUpdatedResultInfo == null) {
            "Attempt to rewrite resulting dataFlowInfo enhancement for call: $kotlinCall"
        }
        nonTrivialUpdatedResultInfo = dataFlowInfo.and(kotlinCall.psiKotlinCall.resultDataFlowInfo)
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
    substitutor: NewTypeSubstitutor?,
    private var diagnostics: Collection<KotlinCallDiagnostic>
) : NewAbstractResolvedCall<D>() {
    var isCompleted = false
        private set
    private lateinit var resultingDescriptor: D

    private lateinit var typeArguments: List<UnwrappedType>

    private var extensionReceiver = resolvedCallAtom.extensionReceiverArgument?.receiver?.receiverValue
    private var dispatchReceiver = resolvedCallAtom.dispatchReceiverArgument?.receiver?.receiverValue
    private var smartCastDispatchReceiverType: KotlinType? = null
    private var expedtedTypeForSamConvertedArgumentMap: MutableMap<ValueArgument, UnwrappedType>? = null


    override val kotlinCall: KotlinCall get() = resolvedCallAtom.atom

    override fun getStatus(): ResolutionStatus = getResultApplicability(diagnostics).toResolutionStatus()

    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
        get() = resolvedCallAtom.argumentMappingByOriginal

    override fun getCandidateDescriptor(): D = resolvedCallAtom.candidateDescriptor as D
    override fun getResultingDescriptor(): D = resultingDescriptor
    override fun getExtensionReceiver(): ReceiverValue? = extensionReceiver
    override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver
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

    fun updateDiagnostics(completedDiagnostics: Collection<KotlinCallDiagnostic>) {
        diagnostics = completedDiagnostics
    }

    private fun updateExtensionReceiverType(newType: KotlinType) {
        if (extensionReceiver?.type == newType) return
        extensionReceiver = extensionReceiver?.replaceType(newType)
    }

    private fun updateDispatchReceiverType(newType: KotlinType) {
        if (dispatchReceiver?.type == newType) return
        dispatchReceiver = dispatchReceiver?.replaceType(newType)
    }

    fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        //clear cached values
        argumentToParameterMap = null
        _valueArguments = null
        if (substitutor != null) {
            // todo: add asset that we do not complete call many times
            isCompleted = true

            dispatchReceiver?.type?.let {
                val newType = substitutor.safeSubstitute(it.unwrap())
                updateDispatchReceiverType(newType)
            }

            extensionReceiver?.type?.let {
                val newType = substitutor.safeSubstitute(it.unwrap())
                updateExtensionReceiverType(newType)
            }
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
                        substitutor ?: FreshVariableNewTypeSubstitutor.Empty
                    )
                else ->
                    candidateDescriptor
            }
        } as D

        typeArguments = resolvedCallAtom.substitutor.freshVariables.map {
            val substituted = (substitutor ?: FreshVariableNewTypeSubstitutor.Empty).safeSubstitute(it.defaultType)
            TypeApproximator().approximateToSuperType(substituted, TypeApproximatorConfiguration.CapturedTypesApproximation) ?: substituted
        }

        calculateExpedtedTypeForSamConvertedArgumentMap(substitutor)
    }

    fun getExpectedTypeForSamConvertedArgument(valueArgument: ValueArgument): UnwrappedType? =
        expedtedTypeForSamConvertedArgumentMap?.get(valueArgument)

    private fun calculateExpedtedTypeForSamConvertedArgumentMap(substitutor: NewTypeSubstitutor?) {
        if (resolvedCallAtom.argumentsWithConversion.isEmpty()) return

        expedtedTypeForSamConvertedArgumentMap = hashMapOf()
        for ((argument, description) in resolvedCallAtom.argumentsWithConversion) {
            val typeWithFreshVariables = resolvedCallAtom.substitutor.safeSubstitute(description.convertedTypeByCandidateParameter)
            val expectedType = substitutor?.safeSubstitute(typeWithFreshVariables) ?: typeWithFreshVariables
            expedtedTypeForSamConvertedArgumentMap!![argument.psiCallArgument.valueArgument] = expectedType
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
) : VariableAsFunctionResolvedCall, ResolvedCall<FunctionDescriptor> by functionCall {
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
