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

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.coroutines.controllerTypeIfCoroutine
import org.jetbrains.kotlin.coroutines.resolveCoroutineHandleResultCallIfNeeded
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.CONSTRAINT_SYSTEM_COMPLETER
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isInvokeCallOnVariable
import org.jetbrains.kotlin.resolve.calls.callUtil.isCallableReference
import org.jetbrains.kotlin.resolve.calls.callUtil.isFakeElement
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.*
import org.jetbrains.kotlin.resolve.calls.inference.filterConstraintsOut
import org.jetbrains.kotlin.resolve.calls.inference.toHandle
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.makeNullableTypeIfSafeReceiver
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.DataFlowAnalyzer
import org.jetbrains.kotlin.types.expressions.FakeCallResolver
import java.util.*

class CallCompleter(
        private val argumentTypeResolver: ArgumentTypeResolver,
        private val candidateResolver: CandidateResolver,
        private val dataFlowAnalyzer: DataFlowAnalyzer,
        private val callCheckers: Iterable<CallChecker>,
        private val builtIns: KotlinBuiltIns,
        private val fakeCallResolver: FakeCallResolver,
        private val languageVersionSettings: LanguageVersionSettings
) {
    fun <D : CallableDescriptor> completeCall(
            context: BasicCallResolutionContext,
            results: OverloadResolutionResultsImpl<D>,
            tracing: TracingStrategy
    ): OverloadResolutionResultsImpl<D> {

        val resolvedCall = if (results.isSingleResult) results.resultingCall else null

        // for the case 'foo(a)' where 'foo' is a variable, the call 'foo.invoke(a)' shouldn't be completed separately,
        // it's completed when the outer (variable as function call) is completed
        if (!isInvokeCallOnVariable(context.call)) {
            val temporaryTrace = TemporaryBindingTrace.create(context.trace, "Trace to complete a resulting call")

            completeResolvedCallAndArguments(resolvedCall, results, context.replaceBindingTrace(temporaryTrace), tracing)

            completeAllCandidates(context, results)

            temporaryTrace.commit()
        }

        if (resolvedCall != null) {
            val calleeExpression = if (resolvedCall is VariableAsFunctionResolvedCall)
                resolvedCall.variableCall.call.calleeExpression
            else
                resolvedCall.call.calleeExpression
            val reportOn =
                    if (calleeExpression != null && !calleeExpression.isFakeElement) calleeExpression
                    else resolvedCall.call.callElement

            if (context.trace.wantsDiagnostics()) {
                val callCheckerContext = CallCheckerContext(context, languageVersionSettings)
                for (callChecker in callCheckers) {
                    callChecker.check(resolvedCall, reportOn, callCheckerContext)
                }
            }

            resolveHandleResultCallForCoroutineLambdaExpressions(context, resolvedCall)
        }

        if (results.isSingleResult && results.resultingCall.status.isSuccess) {
            return results.changeStatusToSuccess()
        }
        return results
    }

    private fun <D : CallableDescriptor> resolveHandleResultCallForCoroutineLambdaExpressions(
            context: BasicCallResolutionContext,
            resolvedCall: ResolvedCall<D>
    ) {
        resolvedCall.valueArguments.values
                .flatMap { it.arguments.map { it.getArgumentExpression() } }
                .filterIsInstance<KtLambdaExpression>()
                .forEach {
                    val function = context.trace.bindingContext[BindingContext.FUNCTION, it.functionLiteral] ?: return@forEach

                    function.controllerTypeIfCoroutine ?: return@forEach

                    val lastBlockStatement = it.functionLiteral.bodyExpression?.statements?.lastOrNull()

                    // Already resolved
                    if (lastBlockStatement is KtReturnExpression) return@forEach

                    fakeCallResolver.resolveCoroutineHandleResultCallIfNeeded(it.functionLiteral, lastBlockStatement, function, context)
                }
    }

    private fun <D : CallableDescriptor> completeAllCandidates(
            context: BasicCallResolutionContext,
            results: OverloadResolutionResultsImpl<D>
    ) {
        @Suppress("UNCHECKED_CAST")
        val candidates = (if (context.collectAllCandidates) {
            results.allCandidates!!
        }
        else {
            results.resultingCalls
        }) as Collection<MutableResolvedCall<D>>

        candidates.filterNot { resolvedCall -> resolvedCall.isCompleted }.forEach {
            resolvedCall ->

            val temporaryBindingTrace = TemporaryBindingTrace.create(context.trace, "Trace to complete a candidate that is not a resulting call")
            completeResolvedCallAndArguments(resolvedCall, results, context.replaceBindingTrace(temporaryBindingTrace), TracingStrategy.EMPTY)
        }
    }

    private fun <D : CallableDescriptor> completeResolvedCallAndArguments(
            resolvedCall: MutableResolvedCall<D>?,
            results: OverloadResolutionResultsImpl<D>,
            context: BasicCallResolutionContext,
            tracing: TracingStrategy
    ) {
        if (resolvedCall == null || resolvedCall.isCompleted || resolvedCall.constraintSystem == null) {
            completeArguments(context, results)
            resolvedCall?.markCallAsCompleted()
            return
        }

        resolvedCall.completeConstraintSystem(context.expectedType, context.trace)

        completeArguments(context, results)

        resolvedCall.updateResolutionStatusFromConstraintSystem(context, tracing)
        resolvedCall.markCallAsCompleted()
    }

    private fun <D : CallableDescriptor> MutableResolvedCall<D>.completeConstraintSystem(
            expectedType: KotlinType,
            trace: BindingTrace
    ) {
        val returnType = candidateDescriptor.returnType

        val expectedReturnType =
                if (call.isCallableReference()) {
                    // TODO: compute generic type argument for R in the kotlin.Function<R> supertype (KT-12963)
                    // TODO: also add constraints for parameter types (KT-12964)
                    if (!TypeUtils.noExpectedType(expectedType) && expectedType.isFunctionType) expectedType.getReturnTypeFromFunctionType()
                    else TypeUtils.NO_EXPECTED_TYPE
                }
                else expectedType

        fun ConstraintSystem.Builder.returnTypeInSystem(): KotlinType? =
                returnType?.let {
                    val substitutor = typeVariableSubstitutors[call.toHandle()] ?: error("No substitutor for call: $call")
                    substitutor.substitute(it, Variance.INVARIANT)
                }

        fun updateSystemIfNeeded(buildSystemWithAdditionalConstraints: (ConstraintSystem.Builder) -> ConstraintSystem?) {
            val system = buildSystemWithAdditionalConstraints(constraintSystem!!.toBuilder())
            if (system != null) {
                setConstraintSystem(system)
            }
        }

        if (returnType != null && !TypeUtils.noExpectedType(expectedReturnType)) {
            updateSystemIfNeeded { builder ->
                val returnTypeInSystem = builder.returnTypeInSystem()
                if (returnTypeInSystem != null) {
                    builder.addSubtypeConstraint(returnTypeInSystem, expectedReturnType, EXPECTED_TYPE_POSITION.position())
                    builder.build()
                }
                else null
            }
        }

        val constraintSystemCompleter = trace[CONSTRAINT_SYSTEM_COMPLETER, call.calleeExpression]
        if (constraintSystemCompleter != null) {
            // todo improve error reporting with errors in constraints from completer
            // todo add constraints from completer unconditionally; improve constraints from completer for generic methods
            // add the constraints only if they don't lead to errors (except errors from upper bounds to improve diagnostics)
            updateSystemIfNeeded { builder ->
                constraintSystemCompleter.completeConstraintSystem(builder, this)
                val system = builder.build()
                val status = system.filterConstraintsOut(TYPE_BOUND_POSITION).status
                if (status.hasOnlyErrorsDerivedFrom(FROM_COMPLETER)) null else system
            }
        }

        if (returnType != null && expectedReturnType === TypeUtils.UNIT_EXPECTED_TYPE) {
            updateSystemIfNeeded { builder ->
                val returnTypeInSystem = builder.returnTypeInSystem()
                if (returnTypeInSystem != null) {
                    builder.addSubtypeConstraint(returnTypeInSystem, builtIns.unitType, EXPECTED_TYPE_POSITION.position())
                    val system = builder.build()
                    if (system.status.isSuccessful()) system else null
                }
                else null
            }
        }


        val builder = constraintSystem!!.toBuilder()
        builder.fixVariables()
        val system = builder.build()
        setConstraintSystem(system)

        setResultingSubstitutor(system.resultingSubstitutor)
    }

    private fun <D : CallableDescriptor> MutableResolvedCall<D>.updateResolutionStatusFromConstraintSystem(
            context: BasicCallResolutionContext,
            tracing: TracingStrategy
    ) {
        val contextWithResolvedCall = CallCandidateResolutionContext.createForCallBeingAnalyzed(this, context, tracing)
        val valueArgumentsCheckingResult = candidateResolver.checkAllValueArguments(contextWithResolvedCall, RESOLVE_FUNCTION_ARGUMENTS)

        val status = status
        if (constraintSystem!!.status.isSuccessful()) {
            if (status == ResolutionStatus.UNKNOWN_STATUS || status == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE) {
                setStatusToSuccess()
            }
            return
        }

        val receiverType = extensionReceiver?.type

        val errorData = InferenceErrorData.create(
                candidateDescriptor, constraintSystem!!, valueArgumentsCheckingResult.argumentTypes,
                receiverType, context.expectedType, context.call
        )
        tracing.typeInferenceFailed(context, errorData)

        addStatus(ResolutionStatus.OTHER_ERROR)
    }

    private fun <D : CallableDescriptor> completeArguments(
            context: BasicCallResolutionContext,
            results: OverloadResolutionResultsImpl<D>
    ) {
        if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) return

        val getArgumentMapping: (ValueArgument) -> ArgumentMapping
        val getDataFlowInfoForArgument: (ValueArgument) -> DataFlowInfo
        if (results.isSingleResult) {
            val resolvedCall = results.resultingCall
            getArgumentMapping = { argument -> resolvedCall.getArgumentMapping(argument) }
            getDataFlowInfoForArgument = { argument -> resolvedCall.dataFlowInfoForArguments.getInfo(argument) }
        }
        else {
            getArgumentMapping = { ArgumentUnmapped }
            getDataFlowInfoForArgument = { context.dataFlowInfo }
        }

        for (valueArgument in context.call.valueArguments) {
            val argumentMapping = getArgumentMapping(valueArgument!!)
            val (expectedType, callPosition) = when (argumentMapping) {
                is ArgumentMatch -> Pair(
                        getEffectiveExpectedType(argumentMapping.valueParameter, valueArgument),
                        CallPosition.ValueArgumentPosition(results.resultingCall, argumentMapping.valueParameter, valueArgument))
                else -> Pair(TypeUtils.NO_EXPECTED_TYPE, CallPosition.Unknown)
            }
            val newContext =
                    context.replaceDataFlowInfo(getDataFlowInfoForArgument(valueArgument))
                            .replaceExpectedType(expectedType)
                            .replaceCallPosition(callPosition)
            completeOneArgument(valueArgument, newContext)
        }
    }

    private fun completeOneArgument(
            valueArgument: ValueArgument,
            context: BasicCallResolutionContext
    ) {
        if (valueArgument.isExternal()) return

        val expression = valueArgument.getArgumentExpression() ?: return
        val deparenthesized = KtPsiUtil.getLastElementDeparenthesized(expression, context.statementFilter) ?: return

        val recordedType = context.trace.getType(expression)
        var updatedType: KotlinType? = recordedType

        val results = completeCallForArgument(deparenthesized, context)
        if (results != null && results.isSingleResult) {
            val resolvedCall = results.resultingCall
            updatedType = if (resolvedCall.hasInferredReturnType()) {
                resolvedCall.makeNullableTypeIfSafeReceiver(resolvedCall.resultingDescriptor?.returnType, context)
            }
            else null
        }

        // For the cases like 'foo(1)' the type of '1' depends on expected type (it can be Int, Byte, etc.),
        // so while the expected type is not known, it's IntegerValueType(1), and should be updated when the expected type is known.
        if (recordedType != null && !recordedType.constructor.isDenotable) {
            updatedType = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(context, expression) ?: updatedType
        }

        updatedType = updateRecordedTypeForArgument(updatedType, recordedType, expression, context.statementFilter, context.trace)

        // While the expected type is not known, the function literal arguments are not analyzed (to analyze function literal bodies once),
        // but they should be analyzed when the expected type is known (during the call completion).
        ArgumentTypeResolver.getFunctionLiteralArgumentIfAny(expression, context)?.let { functionLiteralArgument ->
            argumentTypeResolver.getFunctionLiteralTypeInfo(expression, functionLiteralArgument, context, RESOLVE_FUNCTION_ARGUMENTS)
        }

        // While the expected type is not known, (possibly overloaded) callable references can have placeholder types
        // (to avoid exponential search for overloaded higher-order functions).
        // They should be analyzed now.
        ArgumentTypeResolver.getCallableReferenceExpressionIfAny(expression, context)?.let { callableReferenceArgument ->
            argumentTypeResolver.getCallableReferenceTypeInfo(expression, callableReferenceArgument, context, RESOLVE_FUNCTION_ARGUMENTS)
        }

        dataFlowAnalyzer.checkType(updatedType, deparenthesized, context)
    }

    private fun completeCallForArgument(
            expression: KtExpression,
            context: BasicCallResolutionContext
    ): OverloadResolutionResultsImpl<*>? {
        val cachedData = getResolutionResultsCachedData(expression, context) ?: return null
        val (cachedResolutionResults, cachedContext, tracing) = cachedData

        val contextForArgument = cachedContext.replaceBindingTrace(context.trace)
                .replaceExpectedType(context.expectedType).replaceCollectAllCandidates(false).replaceCallPosition(context.callPosition)

        return completeCall(contextForArgument, cachedResolutionResults, tracing)
    }

    private fun updateRecordedTypeForArgument(
            updatedType: KotlinType?,
            recordedType: KotlinType?,
            argumentExpression: KtExpression,
            statementFilter: StatementFilter,
            trace: BindingTrace
    ): KotlinType? {
        //workaround for KT-8218
        if ((!ErrorUtils.containsErrorType(recordedType) && recordedType == updatedType) || updatedType == null) return updatedType

        fun deparenthesizeOrGetSelector(expression: KtExpression?): KtExpression? {
            val deparenthesized = KtPsiUtil.deparenthesizeOnce(expression)
            if (deparenthesized != expression) return deparenthesized

            // see KtPsiUtil.getLastElementDeparenthesized
            if (expression is KtBlockExpression) {
                return statementFilter.getLastStatementInABlock(expression)
            }

            return (expression as? KtQualifiedExpression)?.selectorExpression
        }

        val expressions = ArrayList<KtExpression>()
        var expression: KtExpression? = argumentExpression
        while (expression != null) {
            expressions.add(expression)
            expression = deparenthesizeOrGetSelector(expression)
        }

        var shouldBeMadeNullable: Boolean = false
        expressions.asReversed().forEach { expression ->
            if (!(expression is KtParenthesizedExpression || expression is KtLabeledExpression || expression is KtAnnotatedExpression)) {
                shouldBeMadeNullable = hasNecessarySafeCall(expression, trace)
            }
            BindingContextUtils.updateRecordedType(updatedType, expression, trace, shouldBeMadeNullable)
        }
        return trace.getType(argumentExpression)
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
}
