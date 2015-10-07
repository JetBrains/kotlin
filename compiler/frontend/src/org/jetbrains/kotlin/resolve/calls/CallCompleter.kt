/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext.CONSTRAINT_SYSTEM_COMPLETER
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isInvokeCallOnVariable
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.FROM_COMPLETER
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.TYPE_BOUND_POSITION
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.DataFlowAnalyzer
import java.util.*

public class CallCompleter(
        private val argumentTypeResolver: ArgumentTypeResolver,
        private val candidateResolver: CandidateResolver,
        private val symbolUsageValidator: SymbolUsageValidator,
        private val dataFlowAnalyzer: DataFlowAnalyzer,
        private val callCheckers: Iterable<CallChecker>,
        private val builtIns: KotlinBuiltIns
) {
    fun <D : CallableDescriptor> completeCall(
            context: BasicCallResolutionContext,
            results: OverloadResolutionResultsImpl<D>,
            tracing: TracingStrategy
    ): OverloadResolutionResultsImpl<D> {

        val resolvedCall = if (results.isSingleResult()) results.getResultingCall() else null

        // for the case 'foo(a)' where 'foo' is a variable, the call 'foo.invoke(a)' shouldn't be completed separately,
        // it's completed when the outer (variable as function call) is completed
        if (!isInvokeCallOnVariable(context.call)) {

            val temporaryTrace = TemporaryBindingTrace.create(context.trace, "Trace to complete a resulting call")

            completeResolvedCallAndArguments(resolvedCall, results, context.replaceBindingTrace(temporaryTrace), tracing)

            completeAllCandidates(context, results)

            temporaryTrace.commit()
        }

        if (resolvedCall != null) {
            context.performContextDependentCallChecks(resolvedCall)
            for (callChecker in callCheckers) {
                callChecker.check(resolvedCall, context)
            }

            val element = if (resolvedCall is VariableAsFunctionResolvedCall)
                resolvedCall.variableCall.getCall().getCalleeExpression()
            else
                resolvedCall.getCall().getCalleeExpression()
            symbolUsageValidator.validateCall(resolvedCall, resolvedCall.getResultingDescriptor(), context.trace, element!!)
        }

        if (results.isSingleResult() && results.getResultingCall().getStatus().isSuccess()) {
            return results.changeStatusToSuccess()
        }
        return results
    }

    private fun <D : CallableDescriptor> completeAllCandidates(
            context: BasicCallResolutionContext,
            results: OverloadResolutionResultsImpl<D>
    ) {
        @Suppress("UNCHECKED_CAST")
        val candidates = (if (context.collectAllCandidates) {
            results.getAllCandidates()!!
        }
        else {
            results.getResultingCalls()
        }) as Collection<MutableResolvedCall<D>>

        candidates.filter { resolvedCall -> !resolvedCall.isCompleted() }.forEach {
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
        if (resolvedCall == null || resolvedCall.isCompleted() || resolvedCall.getConstraintSystem() == null) {
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
            expectedType: JetType,
            trace: BindingTrace
    ) {
        fun updateSystemIfSuccessful(update: (ConstraintSystemImpl) -> Boolean) {
            val copy = (getConstraintSystem() as ConstraintSystemImpl).copy() as ConstraintSystemImpl
            if (update(copy)) {
                setConstraintSystem(copy)
            }
        }

        val returnType = getCandidateDescriptor().getReturnType()
        if (returnType != null) {
            getConstraintSystem()!!.addSupertypeConstraint(expectedType, returnType, EXPECTED_TYPE_POSITION.position())
        }

        val constraintSystemCompleter = trace[CONSTRAINT_SYSTEM_COMPLETER, getCall().getCalleeExpression()]
        if (constraintSystemCompleter != null) {
            // todo improve error reporting with errors in constraints from completer
            // todo add constraints from completer unconditionally; improve constraints from completer for generic methods
            // add the constraints only if they don't lead to errors (except errors from upper bounds to improve diagnostics)
            updateSystemIfSuccessful {
                system ->
                constraintSystemCompleter.completeConstraintSystem(system, this)
                !system.filterConstraintsOut(TYPE_BOUND_POSITION).getStatus().hasOnlyErrorsDerivedFrom(FROM_COMPLETER)
            }
        }

        if (returnType != null && expectedType === TypeUtils.UNIT_EXPECTED_TYPE) {
            updateSystemIfSuccessful {
                system ->
                system.addSupertypeConstraint(builtIns.getUnitType(), returnType, EXPECTED_TYPE_POSITION.position())
                system.getStatus().isSuccessful()
            }
        }

        val constraintSystem = getConstraintSystem() as ConstraintSystemImpl
        constraintSystem.fixVariables()

        setResultingSubstitutor(getConstraintSystem()!!.getResultingSubstitutor())
    }

    private fun <D : CallableDescriptor> MutableResolvedCall<D>.updateResolutionStatusFromConstraintSystem(
            context: BasicCallResolutionContext,
            tracing: TracingStrategy
    ) {
        val contextWithResolvedCall = CallCandidateResolutionContext.createForCallBeingAnalyzed(this, context, tracing)
        val valueArgumentsCheckingResult = candidateResolver.checkAllValueArguments(contextWithResolvedCall, RESOLVE_FUNCTION_ARGUMENTS)

        val status = getStatus()
        if (getConstraintSystem()!!.getStatus().isSuccessful()) {
            if (status == ResolutionStatus.UNKNOWN_STATUS || status == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE) {
                setStatusToSuccess()
            }
            return
        }

        val receiverType = if (getExtensionReceiver().exists()) getExtensionReceiver().getType() else null
        val errorData = InferenceErrorData.create(
                getCandidateDescriptor(), getConstraintSystem()!!, valueArgumentsCheckingResult.argumentTypes,
                receiverType, context.expectedType)
        tracing.typeInferenceFailed(context.trace, errorData)

        addStatus(ResolutionStatus.OTHER_ERROR)
    }

    private fun <D : CallableDescriptor> completeArguments(
            context: BasicCallResolutionContext,
            results: OverloadResolutionResultsImpl<D>
    ) {
        if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) return

        val getArgumentMapping: (ValueArgument) -> ArgumentMapping
        val getDataFlowInfoForArgument: (ValueArgument) -> DataFlowInfo
        if (results.isSingleResult()) {
            val resolvedCall = results.getResultingCall()
            getArgumentMapping = { argument -> resolvedCall.getArgumentMapping(argument) }
            getDataFlowInfoForArgument = {argument -> resolvedCall.getDataFlowInfoForArguments().getInfo(argument) }
        }
        else {
            getArgumentMapping = { ArgumentUnmapped }
            getDataFlowInfoForArgument = { context.dataFlowInfo }
        }

        for (valueArgument in context.call.getValueArguments()) {
            val argumentMapping = getArgumentMapping(valueArgument!!)
            val expectedType = when (argumentMapping) {
                is ArgumentMatch -> getEffectiveExpectedType(argumentMapping.valueParameter, valueArgument)
                else -> TypeUtils.NO_EXPECTED_TYPE
            }
            val newContext = context.replaceDataFlowInfo(getDataFlowInfoForArgument(valueArgument)).replaceExpectedType(expectedType)
            completeOneArgument(valueArgument, newContext)
        }
    }

    private fun completeOneArgument(
            valueArgument: ValueArgument,
            context: BasicCallResolutionContext
    ) {
        if (valueArgument.isExternal()) return

        val expression = valueArgument.getArgumentExpression() ?: return
        val deparenthesized = JetPsiUtil.getLastElementDeparenthesized(expression, context.statementFilter) ?: return

        val recordedType = expression.let { context.trace.getType(it) }
        var updatedType: JetType? = recordedType

        val results = completeCallForArgument(deparenthesized, context)
        if (results != null && results.isSingleResult()) {
            val resolvedCall = results.getResultingCall()
            updatedType = if (resolvedCall.hasInferredReturnType()) resolvedCall.getResultingDescriptor()?.getReturnType() else null
        }

        // For the cases like 'foo(1)' the type of '1' depends on expected type (it can be Int, Byte, etc.),
        // so while the expected type is not known, it's IntegerValueType(1), and should be updated when the expected type is known.
        if (recordedType != null && !recordedType.getConstructor().isDenotable()) {
            updatedType = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(context, expression)
        }

        updatedType = updateRecordedTypeForArgument(updatedType, recordedType, expression, context.trace)

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
            expression: JetExpression,
            context: BasicCallResolutionContext
    ): OverloadResolutionResultsImpl<*>? {
        val cachedData = getResolutionResultsCachedData(expression, context) ?: return null
        val (cachedResolutionResults, cachedContext, tracing) = cachedData

        @Suppress("UNCHECKED_CAST")
        val cachedResults = cachedResolutionResults as OverloadResolutionResultsImpl<CallableDescriptor>
        val contextForArgument = cachedContext.replaceBindingTrace(context.trace)
                .replaceExpectedType(context.expectedType).replaceCollectAllCandidates(false)

        return completeCall(contextForArgument, cachedResults, tracing)
    }

    private fun updateRecordedTypeForArgument(
            updatedType: JetType?,
            recordedType: JetType?,
            argumentExpression: JetExpression,
            trace: BindingTrace
    ): JetType? {
        //workaround for KT-8218
        if ((!ErrorUtils.containsErrorType(recordedType) && recordedType == updatedType) || updatedType == null) return updatedType

        fun deparenthesizeOrGetSelector(expression: JetExpression?): JetExpression? {
            val deparenthesized = JetPsiUtil.deparenthesizeOnce(expression)
            if (deparenthesized != expression) return deparenthesized

            if (expression is JetQualifiedExpression) return expression.getSelectorExpression()
            return null
        }

        val expressions = ArrayList<JetExpression>()
        var expression: JetExpression? = argumentExpression
        while (expression != null) {
            expressions.add(expression)
            expression = deparenthesizeOrGetSelector(expression)
        }

        var shouldBeMadeNullable: Boolean = false
        expressions.asReversed().forEach { expression ->
            if (!(expression is JetParenthesizedExpression || expression is JetLabeledExpression || expression is JetAnnotatedExpression)) {
                shouldBeMadeNullable = hasNecessarySafeCall(expression, trace)
            }
            BindingContextUtils.updateRecordedType(updatedType, expression, trace, shouldBeMadeNullable)
        }
        return trace.getType(argumentExpression)
    }

    private fun hasNecessarySafeCall(expression: JetExpression, trace: BindingTrace): Boolean {
        // We are interested in type of the last call:
        // 'a.b?.foo()' is safe call, but 'a?.b.foo()' is not.
        // Since receiver is 'a.b' and selector is 'foo()',
        // we can only check if an expression is safe call.
        if (expression !is JetSafeQualifiedExpression) return false

        //If a receiver type is not null, then this safe expression is useless, and we don't need to make the result type nullable.
        val expressionType = trace.getType(expression.getReceiverExpression())
        return expressionType != null && TypeUtils.isNullableType(expressionType)
    }
}
