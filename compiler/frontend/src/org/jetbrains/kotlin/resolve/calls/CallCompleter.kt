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

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.contracts.EffectSystem
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.CONSTRAINT_SYSTEM_COMPLETER
import org.jetbrains.kotlin.resolve.annotations.hasImplicitIntegerCoercionAnnotation
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
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.DataFlowAnalyzer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

class CallCompleter(
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val candidateResolver: CandidateResolver,
    private val dataFlowAnalyzer: DataFlowAnalyzer,
    private val callCheckers: Iterable<CallChecker>,
    private val moduleDescriptor: ModuleDescriptor,
    private val deprecationResolver: DeprecationResolver,
    private val effectSystem: EffectSystem,
    private val dataFlowValueFactory: DataFlowValueFactory
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
            completeResolvedCallAndArguments(resolvedCall, results, context, tracing)
            completeAllCandidates(context, results)
        }

        if (resolvedCall != null && context.trace.wantsDiagnostics()) {
            val calleeExpression = if (resolvedCall is VariableAsFunctionResolvedCall)
                resolvedCall.variableCall.call.calleeExpression
            else
                resolvedCall.call.calleeExpression
            val reportOn =
                if (calleeExpression != null && !calleeExpression.isFakeElement) calleeExpression
                else resolvedCall.call.callElement

            val callCheckerContext = CallCheckerContext(context, deprecationResolver, moduleDescriptor)
            for (callChecker in callCheckers) {
                callChecker.check(resolvedCall, reportOn, callCheckerContext)

                if (resolvedCall is VariableAsFunctionResolvedCall) {
                    callChecker.check(resolvedCall.variableCall, reportOn, callCheckerContext)
                }
            }
        }

        if (results.isSingleResult && results.resultingCall.status.isSuccess) {
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
            results.allCandidates!!
        } else {
            results.resultingCalls
        }) as Collection<MutableResolvedCall<D>>

        val temporaryBindingTrace =
            TemporaryBindingTrace.create(context.trace, "Trace to complete a candidate that is not a resulting call")
        candidates.filterNot { resolvedCall -> resolvedCall.isCompleted }.forEach { resolvedCall ->

            completeResolvedCallAndArguments(
                resolvedCall,
                results,
                context.replaceBindingTrace(temporaryBindingTrace),
                TracingStrategy.EMPTY
            )
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
            resolvedCall?.updateResultDataFlowInfoUsingEffects(context.trace)
            resolvedCall?.markCallAsCompleted()
            return
        }

        resolvedCall.completeConstraintSystem(context.expectedType, context.trace)

        completeArguments(context, results)

        resolvedCall.updateResolutionStatusFromConstraintSystem(context, tracing)
        resolvedCall.updateResultDataFlowInfoUsingEffects(context.trace)
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
                if (!TypeUtils.noExpectedType(expectedType) && expectedType.isFunctionOrSuspendFunctionType)
                    expectedType.getReturnTypeFromFunctionType()
                else TypeUtils.NO_EXPECTED_TYPE
            } else expectedType

        fun ConstraintSystem.Builder.typeInSystem(type: KotlinType?): KotlinType? =
            type?.let {
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
                val returnTypeInSystem = builder.typeInSystem(returnType)
                if (returnTypeInSystem != null) {
                    builder.addSubtypeConstraint(returnTypeInSystem, expectedReturnType, EXPECTED_TYPE_POSITION.position())
                    builder.build()
                } else null
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
                val returnTypeInSystem = builder.typeInSystem(returnType)
                if (returnTypeInSystem != null) {
                    builder.addSubtypeConstraint(returnTypeInSystem, moduleDescriptor.builtIns.unitType, EXPECTED_TYPE_POSITION.position())
                    val system = builder.build()
                    if (system.status.isSuccessful()) system else null
                } else null
            }
        }

        if (call.isCallableReference() && !TypeUtils.noExpectedType(expectedType) && expectedType.isFunctionOrSuspendFunctionType) {
            updateSystemIfNeeded { builder ->
                candidateDescriptor.valueParameters.zip(expectedType.getValueParameterTypesFromFunctionType())
                    .forEach { (parameter, argument) ->
                        val valueParameterInSystem = builder.typeInSystem(parameter.type)
                        builder.addSubtypeConstraint(
                            valueParameterInSystem,
                            argument.type,
                            VALUE_PARAMETER_POSITION.position(parameter.index)
                        )
                    }

                builder.build()
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
        } else {
            getArgumentMapping = { ArgumentUnmapped }
            getDataFlowInfoForArgument = { context.dataFlowInfo }
        }

        for (valueArgument in context.call.valueArguments) {
            val argumentMapping = getArgumentMapping(valueArgument!!)

            val expectedType: KotlinType
            val callPosition: CallPosition
            val parameter: ValueParameterDescriptor?
            when (argumentMapping) {
                is ArgumentMatch -> {
                    expectedType = getEffectiveExpectedType(argumentMapping.valueParameter, valueArgument, context)
                    callPosition = CallPosition.ValueArgumentPosition(results.resultingCall, argumentMapping.valueParameter, valueArgument)
                    parameter = argumentMapping.valueParameter
                }
                else -> {
                    expectedType = TypeUtils.NO_EXPECTED_TYPE
                    callPosition = CallPosition.Unknown
                    parameter = null
                }
            }

            val newContext =
                context.replaceDataFlowInfo(getDataFlowInfoForArgument(valueArgument))
                    .replaceExpectedType(expectedType)
                    .replaceCallPosition(callPosition)
            completeOneArgument(valueArgument, parameter, newContext)
        }
    }

    private fun createTypeForConvertableConstant(constant: CompileTimeConstant<*>): SimpleType? {
        val value = constant.getValue(TypeUtils.NO_EXPECTED_TYPE).safeAs<Number>()?.toLong() ?: return null
        val typeConstructor = IntegerValueTypeConstructor(value, moduleDescriptor, constant.parameters)
        return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            Annotations.EMPTY, typeConstructor, emptyList(), false,
            ErrorUtils.createErrorScope("Scope for number value type ($typeConstructor)", true)
        )
    }

    private fun completeOneArgument(
        argument: ValueArgument,
        parameter: ValueParameterDescriptor?,
        context: BasicCallResolutionContext
    ) {
        if (argument.isExternal()) return

        val expression = argument.getArgumentExpression() ?: return
        val deparenthesized = KtPsiUtil.getLastElementDeparenthesized(expression, context.statementFilter) ?: return

        val recordedType = context.trace.getType(expression)
        var updatedType: KotlinType? = recordedType

        val results = completeCallForArgument(deparenthesized, context)

        if (results != null && results.isSingleResult) {
            val resolvedCall = results.resultingCall
            val constant = context.trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized]
            if (constant !is IntegerValueTypeConstant || !constant.convertedFromSigned) {
                updatedType =
                        if (resolvedCall.hasInferredReturnType())
                            resolvedCall.makeNullableTypeIfSafeReceiver(resolvedCall.resultingDescriptor?.returnType, context)
                        else
                            null
            }
        }

        // For the cases like 'foo(1)' the type of '1' depends on expected type (it can be Int, Byte, etc.),
        // so while the expected type is not known, it's IntegerValueType(1), and should be updated when the expected type is known.
        if (recordedType != null && !recordedType.constructor.isDenotable) {
            updatedType = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(context, expression) ?: updatedType
        }

        if (parameter?.hasImplicitIntegerCoercionAnnotation() == true) {
            val argumentCompileTimeValue = context.trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized]
            if (argumentCompileTimeValue != null && argumentCompileTimeValue.parameters.isConvertableConstVal) {
                val generalNumberType = createTypeForConvertableConstant(argumentCompileTimeValue)
                if (generalNumberType != null) {
                    updatedType = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(
                        context.trace, context.statementFilter, context.expectedType, generalNumberType, expression
                    )
                }
            }
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

        var shouldBeMadeNullable = false
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

    private fun MutableResolvedCall<*>.updateResultDataFlowInfoUsingEffects(bindingTrace: BindingTrace) {
        if (dataFlowInfoForArguments is MutableDataFlowInfoForArguments.WithoutArgumentsCheck) return

        val moduleDescriptor = DescriptorUtils.getContainingModule(this.resultingDescriptor?.containingDeclaration ?: return)
        val resultDFIfromES = effectSystem.getDataFlowInfoForFinishedCall(this, bindingTrace, moduleDescriptor)
        dataFlowInfoForArguments.updateResultInfo(resultDFIfromES)

        effectSystem.recordDefiniteInvocations(this, bindingTrace, moduleDescriptor)
    }
}
