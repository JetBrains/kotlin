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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.FunctionDescriptorUtil
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver.getLastElementDeparenthesized
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.*
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionResultsCache
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.RECEIVER_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.VALUE_PARAMETER_POSITION
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.INCOMPLETE_TYPE_INFERENCE
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.OTHER_ERROR
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.TypeUtils.makeConstantSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils


class GenericCandidateResolver(
        val argumentTypeResolver: ArgumentTypeResolver
) {

    fun <D : CallableDescriptor> inferTypeArguments(context: CallCandidateResolutionContext<D>): ResolutionStatus {
        val candidateCall = context.candidateCall
        val candidate = candidateCall.getCandidateDescriptor()

        val constraintSystem = ConstraintSystemImpl()
        candidateCall.setConstraintSystem(constraintSystem)

        // If the call is recursive, e.g.
        //   fun foo<T>(t : T) : T = foo(t)
        // we can't use same descriptor objects for T's as actual type values and same T's as unknowns,
        // because constraints become trivial (T :< T), and inference fails
        //
        // Thus, we replace the parameters of our descriptor with fresh objects (perform alpha-conversion)
        val candidateWithFreshVariables = FunctionDescriptorUtil.alphaConvertTypeParameters(candidate)

        val conversionToOriginal = candidateWithFreshVariables.getTypeParameters().zip(candidate.getTypeParameters()).toMap()
        constraintSystem.registerTypeVariables(candidateWithFreshVariables.getTypeParameters(), { Variance.INVARIANT }, { conversionToOriginal[it]!! })

        val substituteDontCare = makeConstantSubstitutor(candidate.getTypeParameters(), DONT_CARE)

        // Value parameters
        for (entry in candidateCall.getValueArguments().entrySet()) {
            val resolvedValueArgument = entry.getValue()
            val valueParameterDescriptor = candidate.getValueParameters().get(entry.getKey().getIndex())


            for (valueArgument in resolvedValueArgument.getArguments()) {
                // TODO : more attempts, with different expected types

                // Here we type check expecting an error type (DONT_CARE, substitution with substituteDontCare)
                // and throw the results away
                // We'll type check the arguments later, with the inferred types expected
                addConstraintForValueArgument(valueArgument, valueParameterDescriptor, substituteDontCare,
                                              constraintSystem, context, SHAPE_FUNCTION_ARGUMENTS)
            }
        }

        // Receiver
        // Error is already reported if something is missing
        val receiverArgument = candidateCall.getExtensionReceiver()
        val receiverParameter = candidate.getExtensionReceiverParameter()
        if (receiverArgument.exists() && receiverParameter != null) {
            var receiverType: JetType? = if (context.candidateCall.isSafeCall())
                TypeUtils.makeNotNullable(receiverArgument.getType())
            else
                receiverArgument.getType()
            if (receiverArgument is ExpressionReceiver) {
                receiverType = updateResultTypeForSmartCasts(receiverType, receiverArgument.getExpression(), context)
            }
            constraintSystem.addSubtypeConstraint(receiverType, receiverParameter.getType(), RECEIVER_POSITION.position())
        }

        // Solution
        val hasContradiction = constraintSystem.getStatus().hasContradiction()
        if (!hasContradiction) {
            return INCOMPLETE_TYPE_INFERENCE
        }
        return OTHER_ERROR
    }

    fun addConstraintForValueArgument(
            valueArgument: ValueArgument,
            valueParameterDescriptor: ValueParameterDescriptor,
            substitutor: TypeSubstitutor,
            constraintSystem: ConstraintSystem,
            context: CallCandidateResolutionContext<*>,
            resolveFunctionArgumentBodies: ResolveArgumentsMode
    ) {

        val effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument)
        val argumentExpression = valueArgument.getArgumentExpression()

        val expectedType = substitutor.substitute(effectiveExpectedType, Variance.INVARIANT)
        val dataFlowInfoForArgument = context.candidateCall.getDataFlowInfoForArguments().getInfo(valueArgument)
        val newContext = context.replaceExpectedType(expectedType).replaceDataFlowInfo(dataFlowInfoForArgument)

        val typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(argumentExpression, newContext, resolveFunctionArgumentBodies)
        context.candidateCall.getDataFlowInfoForArguments().updateInfo(valueArgument, typeInfoForCall.dataFlowInfo)

        val constraintPosition = VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex())

        if (addConstraintForNestedCall(argumentExpression, constraintPosition, constraintSystem, newContext, effectiveExpectedType)) return

        val type = updateResultTypeForSmartCasts(typeInfoForCall.type, argumentExpression, context.replaceDataFlowInfo(dataFlowInfoForArgument))
        constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, constraintPosition)
    }

    private fun addConstraintForNestedCall(
            argumentExpression: JetExpression?,
            constraintPosition: ConstraintPosition,
            constraintSystem: ConstraintSystem,
            context: CallCandidateResolutionContext<*>,
            effectiveExpectedType: JetType
    ): Boolean {
        val resolutionResults = getResolutionResultsCachedData(argumentExpression, context)?.resolutionResults
        if (resolutionResults == null || !resolutionResults.isSingleResult()) return false

        val resultingCall = resolutionResults.getResultingCall()
        if (resultingCall.isCompleted()) return false

        val argumentConstraintSystem = resultingCall.getConstraintSystem() as ConstraintSystemImpl? ?: return false

        val candidateDescriptor = resultingCall.getCandidateDescriptor()
        val returnType = candidateDescriptor.getReturnType() ?: return false

        val nestedTypeVariables = with (argumentConstraintSystem) {
            returnType.getNestedTypeVariables()
        }
        // we add an additional type variable only if no information is inferred for it.
        // otherwise we add currently inferred return type as before
        if (nestedTypeVariables.any { argumentConstraintSystem.getTypeBounds(it).bounds.isNotEmpty() }) return false

        val candidateWithFreshVariables = FunctionDescriptorUtil.alphaConvertTypeParameters(candidateDescriptor)
        val conversion = candidateDescriptor.getTypeParameters().zip(candidateWithFreshVariables.getTypeParameters()).toMap()

        val freshVariables = nestedTypeVariables.map { conversion[it] }.filterNotNull()
        constraintSystem.registerTypeVariables(freshVariables, { Variance.INVARIANT }, { it }, external = true)

        constraintSystem.addSubtypeConstraint(candidateWithFreshVariables.getReturnType(), effectiveExpectedType, constraintPosition)
        return true
    }

    private fun updateResultTypeForSmartCasts(
            type: JetType?,
            argumentExpression: JetExpression?,
            context: ResolutionContext<*>
    ): JetType? {
        val deparenthesizedArgument = getLastElementDeparenthesized(argumentExpression, context)
        if (deparenthesizedArgument == null || type == null) return type

        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(deparenthesizedArgument, type, context)
        if (!dataFlowValue.isPredictable()) return type

        val possibleTypes = context.dataFlowInfo.getPossibleTypes(dataFlowValue)
        if (possibleTypes.isEmpty()) return type

        return TypeUtils.intersect(JetTypeChecker.DEFAULT, possibleTypes)
    }

    public fun <D : CallableDescriptor> completeTypeInferenceDependentOnFunctionLiteralsForCall(
            context: CallCandidateResolutionContext<D>
    ) {
        val resolvedCall = context.candidateCall
        val constraintSystem = resolvedCall.getConstraintSystem() ?: return

        // constraints for function literals
        // Value parameters
        for (entry in resolvedCall.getValueArguments().entrySet()) {
            val resolvedValueArgument = entry.getValue()
            val valueParameterDescriptor = entry.getKey()

            for (valueArgument in resolvedValueArgument.getArguments()) {
                addConstraintForFunctionLiteral(valueArgument, valueParameterDescriptor, constraintSystem, context)
            }
        }
        resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor())
    }

    private fun <D : CallableDescriptor> addConstraintForFunctionLiteral(
            valueArgument: ValueArgument,
            valueParameterDescriptor: ValueParameterDescriptor,
            constraintSystem: ConstraintSystem,
            context: CallCandidateResolutionContext<D>
    ) {
        val argumentExpression = valueArgument.getArgumentExpression() ?: return
        if (!ArgumentTypeResolver.isFunctionLiteralArgument(argumentExpression, context)) return

        val functionLiteral = ArgumentTypeResolver.getFunctionLiteralArgument(argumentExpression, context)

        val effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument)
        var expectedType = constraintSystem.getCurrentSubstitutor().substitute(effectiveExpectedType, Variance.INVARIANT)
        if (expectedType == null || TypeUtils.isDontCarePlaceholder(expectedType)) {
            expectedType = argumentTypeResolver.getShapeTypeOfFunctionLiteral(functionLiteral, context.scope, context.trace, false)
        }
        if (expectedType == null || !KotlinBuiltIns.isFunctionOrExtensionFunctionType(expectedType)
            || hasUnknownFunctionParameter(expectedType)) {
            return
        }
        val dataFlowInfoForArguments = context.candidateCall.getDataFlowInfoForArguments()
        val dataFlowInfoForArgument = dataFlowInfoForArguments.getInfo(valueArgument)

        //todo analyze function literal body once in 'dependent' mode, then complete it with respect to expected type
        val hasExpectedReturnType = !hasUnknownReturnType(expectedType)
        val position = VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex())
        if (hasExpectedReturnType) {
            val temporaryToResolveFunctionLiteral = TemporaryTraceAndCache.create(
                    context, "trace to resolve function literal with expected return type", argumentExpression)

            val statementExpression = JetPsiUtil.getExpressionOrLastStatementInBlock(functionLiteral.getBodyExpression()) ?: return
            val mismatch = BooleanArray(1)
            val errorInterceptingTrace = ExpressionTypingUtils.makeTraceInterceptingTypeMismatch(
                    temporaryToResolveFunctionLiteral.trace, statementExpression, mismatch)
            val newContext = context.replaceBindingTrace(errorInterceptingTrace).replaceExpectedType(expectedType)
                    .replaceDataFlowInfo(dataFlowInfoForArgument).replaceResolutionResultsCache(temporaryToResolveFunctionLiteral.cache)
                    .replaceContextDependency(INDEPENDENT)
            val type = argumentTypeResolver.getFunctionLiteralTypeInfo(
                    argumentExpression, functionLiteral, newContext, RESOLVE_FUNCTION_ARGUMENTS).type
            if (!mismatch[0]) {
                constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, position)
                temporaryToResolveFunctionLiteral.commit()
                return
            }
        }
        val expectedTypeWithoutReturnType = if (hasExpectedReturnType) replaceReturnTypeByUnknown(expectedType) else expectedType
        val newContext = context.replaceExpectedType(expectedTypeWithoutReturnType).replaceDataFlowInfo(dataFlowInfoForArgument)
                .replaceContextDependency(INDEPENDENT)
        val type = argumentTypeResolver.getFunctionLiteralTypeInfo(argumentExpression, functionLiteral, newContext, RESOLVE_FUNCTION_ARGUMENTS).type
        constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, position)
    }
}

fun getResolutionResultsCachedData(expression: JetExpression?, context: ResolutionContext<*>): ResolutionResultsCache.CachedData? {
    if (!ExpressionTypingUtils.dependsOnExpectedType(expression)) return null
    val argumentCall = expression?.getCall(context.trace.getBindingContext()) ?: return null

    return context.resolutionResultsCache[argumentCall]
}