/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.DiagnosticReporterByTrackingStrategy
import org.jetbrains.kotlin.resolve.calls.util.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.isFakeElement
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.components.AdditionalDiagnosticReporter
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.inference.buildResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.util.makeNullableTypeIfSafeReceiver
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorScopeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.expressions.DataFlowAnalyzer
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContextDelegate
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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
    private val builtIns: KotlinBuiltIns,
    private val typeSystemContext: TypeSystemInferenceExtensionContextDelegate,
    private val smartCastManager: SmartCastManager,
    private val typeApproximator: TypeApproximator,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val candidateInterceptor: CandidateInterceptor,
) {
    companion object {
        private val REPORT_MISSING_NEW_INFERENCE_DIAGNOSTIC
            get() = false

        fun keyForPartiallyResolvedCall(resolvedCallAtom: ResolvedCallAtom): Call {
            val psiKotlinCall = resolvedCallAtom.atom.psiKotlinCall
            return if (psiKotlinCall is PSIKotlinCallForInvoke)
                psiKotlinCall.baseCall.psiCall
            else
                psiKotlinCall.psiCall
        }
    }

    fun <D : CallableDescriptor> onlyTransform(
        resolvedCallAtom: ResolvedCallAtom,
        diagnostics: Collection<KotlinCallDiagnostic>,
    ): NewAbstractResolvedCall<D> = transformToResolvedCall(resolvedCallAtom, null, null, diagnostics)

    fun <D : CallableDescriptor> transformAndReport(
        baseResolvedCall: CallResolutionResult,
        context: BasicCallResolutionContext,
        tracingStrategy: TracingStrategy,
    ): NewAbstractResolvedCall<D> {
        return when (baseResolvedCall) {
            is PartialCallResolutionResult -> {
                val candidate = baseResolvedCall.resultCallAtom

                val psiCall = keyForPartiallyResolvedCall(candidate)

                context.trace.record(BindingContext.ONLY_RESOLVED_CALL, psiCall, PartialCallContainer(baseResolvedCall))
                context.trace.record(BindingContext.PARTIAL_CALL_RESOLUTION_CONTEXT, psiCall, context)

                if (baseResolvedCall.forwardToInferenceSession) {
                    context.inferenceSession.addPartialCallInfo(
                        PSIPartialCallInfo(baseResolvedCall, context, tracingStrategy),
                    )
                }

                createStubResolvedCallAndWriteItToTrace<D>(candidate, context.trace, baseResolvedCall.diagnostics, substitutor = null)
            }

            is CompletedCallResolutionResult, is ErrorCallResolutionResult -> {
                val candidate = (baseResolvedCall as SingleCallResolutionResult).resultCallAtom

                val resultSubstitutor =
                    baseResolvedCall.constraintSystem.getBuilder().currentStorage().buildResultingSubstitutor(typeSystemContext)
                if (context.inferenceSession.writeOnlyStubs(baseResolvedCall)) {
                    val stub = createStubResolvedCallAndWriteItToTrace<CallableDescriptor>(
                        candidate,
                        context.trace,
                        baseResolvedCall.diagnostics,
                        substitutor = resultSubstitutor,
                    )

                    forwardCallToInferenceSession(baseResolvedCall, context, stub, tracingStrategy)

                    @Suppress("UNCHECKED_CAST")
                    return stub as NewAbstractResolvedCall<D>
                }

                val ktPrimitiveCompleter = ResolvedAtomCompleter(
                    resultSubstitutor, context, this, expressionTypingServices, argumentTypeResolver,
                    doubleColonExpressionResolver, builtIns, deprecationResolver, moduleDescriptor, dataFlowValueFactory,
                    typeApproximator, missingSupertypesResolver,
                )

                if (context.inferenceSession.shouldCompleteResolvedSubAtomsOf(candidate)) {
                    candidate.subResolvedAtoms?.forEach { subKtPrimitive ->
                        ktPrimitiveCompleter.completeAll(subKtPrimitive)
                    }
                }

                @Suppress("UNCHECKED_CAST")
                val resolvedCall = ktPrimitiveCompleter.completeResolvedCall(
                    candidate, baseResolvedCall.completedDiagnostic(resultSubstitutor),
                ) as NewAbstractResolvedCall<D>

                forwardCallToInferenceSession(baseResolvedCall, context, resolvedCall, tracingStrategy)

                resolvedCall
            }

            is SingleCallResolutionResult -> error("Call resolution result for one candidate didn't transformed: $baseResolvedCall")
            is AllCandidatesResolutionResult -> error("Cannot transform result for ALL_CANDIDATES mode")
        }
    }

    private fun forwardCallToInferenceSession(
        baseResolvedCall: CallResolutionResult,
        context: BasicCallResolutionContext,
        resolvedCall: NewAbstractResolvedCall<*>,
        tracingStrategy: TracingStrategy,
    ) {
        if (baseResolvedCall is CompletedCallResolutionResult) {
            context.inferenceSession.addCompletedCallInfo(PSICompletedCallInfo(baseResolvedCall, context, resolvedCall, tracingStrategy))
        }
    }

    fun <D : CallableDescriptor> createStubResolvedCallAndWriteItToTrace(
        candidate: ResolvedCallAtom,
        trace: BindingTrace,
        diagnostics: Collection<KotlinCallDiagnostic>,
        substitutor: NewTypeSubstitutor?,
    ): NewAbstractResolvedCall<D> {
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
        diagnostics: Collection<KotlinCallDiagnostic>,
    ): NewAbstractResolvedCall<D> {
        val psiKotlinCall = completedCallAtom.atom.psiKotlinCall

        completedCallAtom.setCandidateDescriptor(
            candidateInterceptor.interceptResolvedCallAtomCandidate(
                completedCallAtom.candidateDescriptor,
                completedCallAtom,
                trace,
                resultSubstitutor,
                diagnostics
            )
        )

        return if (psiKotlinCall is PSIKotlinCallForInvoke) {
            val diagnosticsForVariableCall = if (completedCallAtom.candidateDescriptor is FunctionDescriptor) emptyList() else diagnostics
            val diagnosticsForFunctionCall = if (completedCallAtom.candidateDescriptor is FunctionDescriptor) diagnostics else emptyList()

            @Suppress("UNCHECKED_CAST")
            NewVariableAsFunctionResolvedCallImpl(
                createOrGet(psiKotlinCall.variableCall.resolvedCall, trace, resultSubstitutor, diagnosticsForVariableCall),
                createOrGet(completedCallAtom, trace, resultSubstitutor, diagnosticsForFunctionCall),
            ) as NewAbstractResolvedCall<D>
        } else {
            createOrGet(completedCallAtom, trace, resultSubstitutor, diagnostics)
        }
    }

    private fun <D : CallableDescriptor> createOrGet(
        completedSimpleAtom: ResolvedCallAtom,
        trace: BindingTrace?,
        resultSubstitutor: NewTypeSubstitutor?,
        diagnostics: Collection<KotlinCallDiagnostic>,
    ): NewAbstractResolvedCall<D> {
        if (trace != null) {
            val storedResolvedCall = completedSimpleAtom.atom.psiKotlinCall.getResolvedPsiKotlinCall<D>(trace)
            if (storedResolvedCall != null) {
                storedResolvedCall.setResultingSubstitutor(resultSubstitutor)
                storedResolvedCall.updateDiagnostics(diagnostics)
                return storedResolvedCall
            }
        }

        return if (completedSimpleAtom.atom.callKind == KotlinCallKind.CALLABLE_REFERENCE) {
            NewCallableReferenceResolvedCall(
                completedSimpleAtom as ResolvedCallableReferenceCallAtom,
                typeApproximator,
                expressionTypingServices.languageVersionSettings,
                resultSubstitutor
            )
        } else {
            NewResolvedCallImpl(
                completedSimpleAtom, resultSubstitutor, diagnostics, typeApproximator, expressionTypingServices.languageVersionSettings
            )
        }
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
            implicitInvokeCheck = false,
        )
        context.checkReceiver(
            resolvedCall,
            resolvedCall.resultingDescriptor.dispatchReceiverParameter,
            resolvedCall.dispatchReceiver,
            resolvedCall.explicitReceiverKind.isDispatchReceiver,
            implicitInvokeCheck = context.call is CallTransformer.CallForImplicitInvoke,
        )

    }

    private fun BasicCallResolutionContext.checkReceiver(
        resolvedCall: ResolvedCall<*>,
        receiverParameter: ReceiverParameterDescriptor?,
        receiverArgument: ReceiverValue?,
        isExplicitReceiver: Boolean,
        implicitInvokeCheck: Boolean,
    ) {
        if (receiverParameter == null || receiverArgument == null) return
        val safeAccess = isExplicitReceiver && !implicitInvokeCheck && resolvedCall.call.isSemanticallyEquivalentToSafeCall
        additionalTypeCheckers.forEach { it.checkReceiver(receiverParameter, receiverArgument, safeAccess, this) }
    }

    // todo very beginning code
    fun runArgumentsChecks(context: BasicCallResolutionContext, resolvedCall: NewAbstractResolvedCall<*>) {
        if (resolvedCall !is NewResolvedCallImpl<*>) return

        for (valueArgument in resolvedCall.call.valueArguments) {
            val argumentMapping = resolvedCall.getArgumentMapping(valueArgument!!)
            val parameter: ValueParameterDescriptor?
            val (expectedType, callPosition) = when (argumentMapping) {
                is ArgumentMatch -> {
                    parameter = argumentMapping.valueParameter

                    // We should take expected type from the last used conversion
                    // TODO: move this logic into ParameterTypeConversion
                    val expectedType =
                        resolvedCall.getExpectedTypeForUnitConvertedArgument(valueArgument)
                            ?: resolvedCall.getExpectedTypeForSuspendConvertedArgument(valueArgument)
                            ?: resolvedCall.getExpectedTypeForSamConvertedArgument(valueArgument)
                            ?: getEffectiveExpectedType(argumentMapping.valueParameter, valueArgument, context)
                    Pair(
                        expectedType,
                        CallPosition.ValueArgumentPosition(resolvedCall, argumentMapping.valueParameter, valueArgument),
                    )
                }
                else -> {
                    parameter = null
                    Pair(TypeUtils.NO_EXPECTED_TYPE, CallPosition.Unknown)
                }
            }
            val newContext =
                context.replaceDataFlowInfo(resolvedCall.dataFlowInfoForArguments.getInfo(valueArgument))
                    .replaceExpectedType(expectedType)
                    .replaceCallPosition(callPosition)


            val constantConvertedArgument = resolvedCall.getArgumentTypeForConstantConvertedArgument(valueArgument)
            val argumentExpression = valueArgument.getArgumentExpression() ?: continue

            if (constantConvertedArgument != null) {
                context.trace.record(BindingContext.COMPILE_TIME_VALUE, argumentExpression, constantConvertedArgument)
                BindingContextUtils.updateRecordedType(
                    constantConvertedArgument.unknownIntegerType, argumentExpression, context.trace, false
                )
            }

            if (!valueArgument.isExternal()) {
                updateRecordedType(
                    argumentExpression,
                    parameter,
                    newContext,
                    constantConvertedArgument?.unknownIntegerType?.unwrap(),
                    resolvedCall.isReallySuccess()
                )
            }
        }
    }

    fun updateRecordedType(
        expression: KtExpression,
        parameter: ValueParameterDescriptor?,
        context: BasicCallResolutionContext,
        convertedArgumentType: UnwrappedType?,
        reportErrorForTypeMismatch: Boolean,
    ): KotlinType? {
        val deparenthesized = expression.let {
            KtPsiUtil.getLastElementDeparenthesized(it, context.statementFilter)
        } ?: return null

        val recordedType = context.trace.getType(deparenthesized)
        val recordedTypeForParenthesized = context.trace.getType(expression)

        var updatedType = convertedArgumentType ?: getResolvedCallForArgumentExpression(deparenthesized, context)?.run {
            makeNullableTypeIfSafeReceiver(resultingDescriptor.returnType, context)
        } ?: recordedType

        // For the cases like 'foo(1)' the type of '1' depends on expected type (it can be Int, Byte, etc.),
        // so while the expected type is not known, it's IntegerValueType(1), and should be updated when the expected type is known.
        if (recordedType != null && !recordedType.constructor.isDenotable) {
            updatedType = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(context, deparenthesized) ?: updatedType
        }

        var reportErrorDuringTypeCheck = reportErrorForTypeMismatch

        if (parameter != null && ImplicitIntegerCoercion.isEnabledForParameter(parameter)) {
            val argumentCompileTimeValue = context.trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized]
            if (argumentCompileTimeValue != null && argumentCompileTimeValue.parameters.isConvertableConstVal) {
                val generalNumberType = createTypeForConvertableConstant(argumentCompileTimeValue)
                if (generalNumberType != null) {
                    updatedType = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(
                        context.trace, context.statementFilter, context.expectedType, generalNumberType, expression,
                    )
                    reportErrorDuringTypeCheck = true
                }

            }
        } else if (convertedArgumentType != null) {
            context.trace.report(Errors.SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED.on(deparenthesized))
        }

        updatedType = updateRecordedTypeForArgument(updatedType, recordedType, recordedTypeForParenthesized, expression, context)

        dataFlowAnalyzer.checkType(updatedType, deparenthesized, context, reportErrorDuringTypeCheck)

        return updatedType
    }

    private fun createTypeForConvertableConstant(constant: CompileTimeConstant<*>): SimpleType? {
        val value = constant.getValue(TypeUtils.NO_EXPECTED_TYPE).safeAs<Number>()?.toLong() ?: return null
        val typeConstructor = IntegerLiteralTypeConstructor(value, moduleDescriptor, constant.parameters)
        return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            TypeAttributes.Empty, typeConstructor, emptyList(), false,
            ErrorUtils.createErrorScope(ErrorScopeKind.INTEGER_LITERAL_TYPE_SCOPE, throwExceptions = true, typeConstructor.toString()),
        )
    }

    fun getResolvedCallForArgumentExpression(expression: KtExpression, context: BasicCallResolutionContext) =
        if (!ExpressionTypingUtils.dependsOnExpectedType(expression))
            null
        else
            expression.getResolvedCall(context.trace.bindingContext) as? NewAbstractResolvedCall<*>

    // See CallCompleter#updateRecordedTypeForArgument
    private fun updateRecordedTypeForArgument(
        updatedType: KotlinType?,
        recordedType: KotlinType?,
        recordedTypeForParenthesized: KotlinType?,
        argumentExpression: KtExpression,
        context: BasicCallResolutionContext,
    ): KotlinType? {
        if ((!ErrorUtils.containsErrorType(recordedType) && recordedType == updatedType && recordedType == recordedTypeForParenthesized) || updatedType == null)
            return updatedType

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

    internal fun bind(trace: BindingTrace, resolvedCall: ResolvedCall<*>) {
        resolvedCall.safeAs<NewAbstractResolvedCall<*>>()?.let { bind(trace, it) }
        resolvedCall.safeAs<NewVariableAsFunctionResolvedCallImpl>()?.let { bind(trace, it) }
    }

    fun reportDiagnostics(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        resolvedCall: NewAbstractResolvedCall<*>,
        diagnostics: Collection<KotlinCallDiagnostic>,
    ) {
        when (resolvedCall) {
            is NewVariableAsFunctionResolvedCallImpl -> {
                val variableCall = resolvedCall.variableCall
                val functionCall = resolvedCall.functionCall

                reportCallDiagnostic(context, trace, variableCall, variableCall.resultingDescriptor, diagnostics)
                reportCallDiagnostic(context, trace, functionCall, functionCall.resultingDescriptor, emptyList())
            }
            else -> {
                val resolvedCallAtom = resolvedCall.resolvedCallAtom
                if (resolvedCallAtom != null) {
                    reportCallDiagnostic(context, trace, resolvedCall, resolvedCall.resultingDescriptor, diagnostics)
                }
            }
        }
    }

    private fun bind(trace: BindingTrace, simpleResolvedCall: NewAbstractResolvedCall<*>) {
        val tracing = simpleResolvedCall.psiKotlinCall.tracingStrategy

        tracing.bindReference(trace, simpleResolvedCall)
        tracing.bindResolvedCall(trace, simpleResolvedCall)
    }

    private fun bind(trace: BindingTrace, variableAsFunction: NewVariableAsFunctionResolvedCallImpl) {
        val outerTracingStrategy = variableAsFunction.baseCall.tracingStrategy
        val variableCall = variableAsFunction.variableCall
        val functionCall = variableAsFunction.functionCall

        outerTracingStrategy.bindReference(trace, variableCall)
        outerTracingStrategy.bindResolvedCall(trace, variableAsFunction)
        functionCall.psiKotlinCall.tracingStrategy.bindReference(trace, functionCall)
    }

    fun reportCallDiagnostic(
        context: BasicCallResolutionContext,
        trace: BindingTrace,
        resolvedCall: NewAbstractResolvedCall<*>,
        resultingDescriptor: CallableDescriptor,
        diagnostics: Collection<KotlinCallDiagnostic>,
    ) {
        val trackingTrace = TrackingBindingTrace(trace)
        val newContext = context.replaceBindingTrace(trackingTrace)

        val diagnosticHolder = KotlinDiagnosticsHolder.SimpleHolder()
        val resolvedCallAtom = resolvedCall.resolvedCallAtom

        if (resolvedCallAtom != null) {
            additionalDiagnosticReporter.reportAdditionalDiagnostics(resolvedCallAtom, resultingDescriptor, diagnosticHolder, diagnostics)
        }

        val allDiagnostics = diagnostics + diagnosticHolder.getDiagnostics()

        val diagnosticReporter = DiagnosticReporterByTrackingStrategy(
            constantExpressionEvaluator,
            newContext,
            resolvedCall.psiKotlinCall,
            context.dataFlowValueFactory,
            allDiagnostics,
            smartCastManager,
            typeSystemContext
        )

        for (diagnostic in allDiagnostics) {
            trackingTrace.reported = false
            diagnostic.report(diagnosticReporter)

            if (diagnostic is ResolvedUsingDeprecatedVisibility) {
                reportResolvedUsingDeprecatedVisibility(
                    resolvedCall.psiKotlinCall.psiCall, resolvedCall.candidateDescriptor, resultingDescriptor, diagnostic, trace,
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
