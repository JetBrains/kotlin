/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.contracts.EffectSystem
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.CONSTRAINT_SYSTEM_COMPLETER
import org.jetbrains.kotlin.resolve.calls.util.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.util.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.util.isInvokeCallOnVariable
import org.jetbrains.kotlin.resolve.calls.util.isCallableReference
import org.jetbrains.kotlin.resolve.calls.util.isFakeElement
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
import org.jetbrains.kotlin.resolve.calls.util.makeNullableTypeIfSafeReceiver
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.checkers.MissingDependencySupertypeChecker
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorScopeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
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
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val missingSupertypesResolver: MissingSupertypesResolver
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

        if (context.trace.wantsDiagnostics()) {
            if (resolvedCall == null) {
                checkMissingSupertypes(context, missingSupertypesResolver)
            } else {
                val calleeExpression = if (resolvedCall is VariableAsFunctionResolvedCall)
                    resolvedCall.variableCall.call.calleeExpression
                else
                    resolvedCall.call.calleeExpression
                val reportOn =
                    if (calleeExpression != null && !calleeExpression.isFakeElement) calleeExpression
                    else resolvedCall.call.callElement

                val callCheckerContext = CallCheckerContext(context, deprecationResolver, moduleDescriptor, missingSupertypesResolver)
                for (callChecker in callCheckers) {
                    callChecker.check(resolvedCall, reportOn, callCheckerContext)

                    if (resolvedCall is VariableAsFunctionResolvedCall) {
                        callChecker.check(resolvedCall.variableCall, reportOn, callCheckerContext)
                    }
                }
            }
        }

        if (results.isSingleResult && results.resultingCall.status.isSuccess) {
            return results.changeStatusToSuccess()
        }
        return results
    }

    private fun checkMissingSupertypes(
        context: BasicCallResolutionContext,
        missingSupertypesResolver: MissingSupertypesResolver
    ) {
        val call = context.call
        val explicitReceiver = call.explicitReceiver.safeAs<ReceiverValue>() ?: return

        MissingDependencySupertypeChecker.checkSupertypes(
            explicitReceiver.type, call.callElement, context.trace, missingSupertypesResolver
        )
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
        disableContractsInsideContractsBlock(context.call, resolvedCall?.resultingDescriptor, context.scope, context.trace)

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

        val isNewInferenceEnabled = effectSystem.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
        val resultingSubstitutor = if (isNewInferenceEnabled) {
            system.resultingSubstitutor.replaceWithContravariantApproximatingSubstitution()
        } else system.resultingSubstitutor

        setSubstitutor(resultingSubstitutor)
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
            TypeAttributes.Empty, typeConstructor, emptyList(), false,
            ErrorUtils.createErrorScope(ErrorScopeKind.INTEGER_LITERAL_TYPE_SCOPE, throwExceptions = true, typeConstructor.toString())
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

        val constant = context.trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized]
        val convertedConst = constant is IntegerValueTypeConstant && constant.convertedFromSigned

        if (results != null && results.isSingleResult) {
            val resolvedCall = results.resultingCall
            if (!convertedConst) {
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

        if (parameter != null && ImplicitIntegerCoercion.isEnabledForParameter(parameter)) {
            val argumentCompileTimeValue = context.trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized]
            if (argumentCompileTimeValue != null && argumentCompileTimeValue.parameters.isConvertableConstVal) {
                val generalNumberType = createTypeForConvertableConstant(argumentCompileTimeValue)
                if (generalNumberType != null) {
                    updatedType = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(
                        context.trace, context.statementFilter, context.expectedType, generalNumberType, expression
                    )
                }
            }
        } else if (convertedConst) {
            context.trace.report(Errors.SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED.on(deparenthesized))
        }

        updatedType = updateRecordedTypeForArgument(updatedType, recordedType, expression, context.statementFilter, context.trace)

        // While the expected type is not known, the function literal arguments are not analyzed (to analyze function literal bodies once),
        // but they should be analyzed when the expected type is known (during the call completion).
        ArgumentTypeResolver.getFunctionLiteralArgumentIfAny(expression, context)?.let { functionLiteralArgument ->
            argumentTypeResolver.getFunctionLiteralTypeInfo(expression, functionLiteralArgument, context, RESOLVE_FUNCTION_ARGUMENTS, false)
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
        expressions.asReversed().forEach { ktExpression ->
            if (!(ktExpression is KtParenthesizedExpression || ktExpression is KtLabeledExpression || ktExpression is KtAnnotatedExpression)) {
                shouldBeMadeNullable = hasNecessarySafeCall(ktExpression, trace)
            }
            BindingContextUtils.updateRecordedType(updatedType, ktExpression, trace, shouldBeMadeNullable)
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
