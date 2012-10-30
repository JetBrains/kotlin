/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.*;
import org.jetbrains.jet.lang.resolve.calls.inference.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionDebugInfo;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.jet.lang.resolve.calls.tasks.TaskPrioritizer;
import org.jetbrains.jet.lang.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus.*;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

public class CandidateResolver {
    @NotNull
    private final JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
    @NotNull
    private TypeResolver typeResolver;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }


    public void checkTypesWithNoCallee(@NotNull ResolutionContext context) {
        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                expressionTypingServices.getType(context.scope, argumentExpression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
            }
        }

        for (JetExpression expression : context.call.getFunctionLiteralArguments()) {
            expressionTypingServices.getType(context.scope, expression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
        }

        for (JetTypeProjection typeProjection : context.call.getTypeArguments()) {
            JetTypeReference typeReference = typeProjection.getTypeReference();
            if (typeReference == null) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(typeProjection));
            }
            else {
                typeResolver.resolveType(context.scope, typeReference, context.trace, true);
            }
        }
    }

    public <D extends CallableDescriptor, F extends D> void performResolutionForCandidateCall(
            @NotNull CallResolutionContext<D, F> context,
            @NotNull ResolutionTask<D, F> task) {

        ProgressIndicatorProvider.checkCanceled();

        ResolvedCallImpl<D> candidateCall = context.candidateCall;
        D candidate = candidateCall.getCandidateDescriptor();

        if (ErrorUtils.isError(candidate)) {
            candidateCall.addStatus(SUCCESS);
            checkTypesWithNoCallee(context.toBasic());
            return;
        }

        if (!Visibilities.isVisible(candidate, context.scope.getContainingDeclaration())) {
            candidateCall.addStatus(OTHER_ERROR);
            context.tracing.invisibleMember(context.trace, candidate);
            return;
        }

        Set<ValueArgument> unmappedArguments = Sets.newLinkedHashSet();
        ValueArgumentsToParametersMapper.Status
                argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(context.call, context.tracing,
                                                                                                        candidateCall, unmappedArguments);
        if (!argumentMappingStatus.isSuccess()) {
            if (argumentMappingStatus == ValueArgumentsToParametersMapper.Status.STRONG_ERROR) {
                candidateCall.addStatus(STRONG_ERROR);
            }
            else {
                candidateCall.addStatus(OTHER_ERROR);
            }
            if ((argumentMappingStatus == ValueArgumentsToParametersMapper.Status.ERROR && candidate.getTypeParameters().isEmpty()) ||
                argumentMappingStatus == ValueArgumentsToParametersMapper.Status.STRONG_ERROR) {
                checkTypesWithNoCallee(context.toBasic());
                return;
            }
            checkUnmappedArgumentTypes(context.toBasic(), unmappedArguments);
        }

        List<JetTypeProjection> jetTypeArguments = context.call.getTypeArguments();
        if (jetTypeArguments.isEmpty()) {
            if (!candidate.getTypeParameters().isEmpty()) {
                ResolutionStatus status = inferTypeArguments(context);
                candidateCall.addStatus(status);
            }
            else {
                candidateCall.addStatus(checkAllValueArguments(context).status);
            }
        }
        else {
            // Explicit type arguments passed

            List<JetType> typeArguments = new ArrayList<JetType>();
            for (JetTypeProjection projection : jetTypeArguments) {
                if (projection.getProjectionKind() != JetProjectionKind.NONE) {
                    context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection));
                }
                JetTypeReference typeReference = projection.getTypeReference();
                if (typeReference != null) {
                    typeArguments.add(typeResolver.resolveType(context.scope, typeReference, context.trace, true));
                }
                else {
                    typeArguments.add(ErrorUtils.createErrorType("Star projection in a call"));
                }
            }
            int expectedTypeArgumentCount = candidate.getTypeParameters().size();
            if (expectedTypeArgumentCount == jetTypeArguments.size()) {

                checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate, context.trace);

                Map<TypeConstructor, TypeProjection>
                        substitutionContext = FunctionDescriptorUtil.createSubstitutionContext((FunctionDescriptor)candidate, typeArguments);
                candidateCall.setResultingSubstitutor(TypeSubstitutor.create(substitutionContext));

                List<TypeParameterDescriptor> typeParameters = candidateCall.getCandidateDescriptor().getTypeParameters();
                for (int i = 0; i < typeParameters.size(); i++) {
                    TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
                    candidateCall.recordTypeArgument(typeParameterDescriptor, typeArguments.get(i));
                }
                candidateCall.addStatus(checkAllValueArguments(context).status);
            }
            else {
                candidateCall.addStatus(OTHER_ERROR);
                context.tracing.wrongNumberOfTypeArguments(context.trace, expectedTypeArgumentCount);
            }
        }

        task.performAdvancedChecks(candidate, context.trace, context.tracing);

        // 'super' cannot be passed as an argument, for receiver arguments expression typer does not track this
        // See TaskPrioritizer for more
        JetSuperExpression superExpression = TaskPrioritizer.getReceiverSuper(candidateCall.getReceiverArgument());
        if (superExpression != null) {
            context.trace.report(SUPER_IS_NOT_AN_EXPRESSION.on(superExpression, superExpression.getText()));
            candidateCall.addStatus(OTHER_ERROR);
        }

        AutoCastUtils.recordAutoCastIfNecessary(candidateCall.getReceiverArgument(), candidateCall.getTrace());
        AutoCastUtils.recordAutoCastIfNecessary(candidateCall.getThisObject(), candidateCall.getTrace());
    }

    public <D extends CallableDescriptor> void completeTypeInferenceDependentOnExpectedTypeForCall(
            CallResolutionContext<D, D> context,
            Set<ResolvedCallWithTrace<D>> successful,
            Set<ResolvedCallWithTrace<D>> failed
    ) {
        ResolvedCallImpl<D> resolvedCall = context.candidateCall;
        assert resolvedCall.hasUnknownTypeParameters();
        D descriptor = resolvedCall.getCandidateDescriptor();
        ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
        assert constraintSystem != null;

        TypeSubstitutor substituteDontCare = ConstraintSystemWithPriorities
                .makeConstantSubstitutor(resolvedCall.getCandidateDescriptor().getTypeParameters(), ConstraintSystemImpl.DONT_CARE);

        // constraints for function literals
        // Value parameters
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet()) {
            ResolvedValueArgument resolvedValueArgument = entry.getValue();
            ValueParameterDescriptor valueParameterDescriptor = entry.getKey();

            for (ValueArgument valueArgument : resolvedValueArgument.getArguments()) {
                if (!JetPsiUtil.isFunctionLiteralWithoutDeclaredParameterTypes(valueArgument.getArgumentExpression())) continue;

                ConstraintSystem systemWithCurrentSubstitution = constraintSystem.copy();
                addConstraintForValueArgument(valueArgument, valueParameterDescriptor, constraintSystem.getCurrentSubstitutor(),
                                              systemWithCurrentSubstitution, context, null);
                if (systemWithCurrentSubstitution.hasContradiction() || systemWithCurrentSubstitution.hasErrorInConstrainingTypes()) {
                    addConstraintForValueArgument(valueArgument, valueParameterDescriptor, substituteDontCare, constraintSystem, context, null);
                }
                else {
                    constraintSystem = systemWithCurrentSubstitution;
                }
            }
        }

        ConstraintSystem constraintSystemWithoutExpectedTypeConstraint = constraintSystem.copy();
        constraintSystem.addSupertypeConstraint(descriptor.getReturnType(), context.expectedType,
                                                ConstraintPosition.EXPECTED_TYPE_POSITION);


        if (!constraintSystem.isSuccessful()) {
            resolvedCall.setResultingSubstitutor(constraintSystemWithoutExpectedTypeConstraint.getResultingSubstitutor());
            List<JetType> argumentTypes = checkValueArgumentTypes(context, resolvedCall, resolvedCall.getTrace()).argumentTypes;
            JetType receiverType = resolvedCall.getReceiverArgument().exists() ? resolvedCall.getReceiverArgument().getType() : null;
            context.tracing.typeInferenceFailed(resolvedCall.getTrace(),
                                                InferenceErrorData
                                                        .create(descriptor, constraintSystem, argumentTypes, receiverType,
                                                                context.expectedType),
                                                constraintSystemWithoutExpectedTypeConstraint);
            resolvedCall.addStatus(ResolutionStatus.TYPE_INFERENCE_ERROR);
            failed.add(resolvedCall);
            return;
        }

        resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor());
        // Here we type check the arguments with inferred types expected
        checkAllValueArguments(context);

        checkBounds(resolvedCall, constraintSystem, resolvedCall.getTrace(), context.tracing);
        resolvedCall.setHasUnknownTypeParameters(false);
        if (resolvedCall.getStatus().isSuccess() || resolvedCall.getStatus() == ResolutionStatus.UNKNOWN_STATUS) {
            resolvedCall.addStatus(ResolutionStatus.SUCCESS);
            successful.add(resolvedCall);
        }
        else {
            failed.add(resolvedCall);
        }
    }

    private <D extends CallableDescriptor, F extends D> ResolutionStatus inferTypeArguments(CallResolutionContext<D, F> context) {
        ResolvedCallImpl<D> candidateCall = context.candidateCall;
        final D candidate = candidateCall.getCandidateDescriptor();

        context.trace.get(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, context.call.getCallElement());

        ConstraintSystemImpl constraintsSystem = new ConstraintSystemImpl();

        // If the call is recursive, e.g.
        //   fun foo<T>(t : T) : T = foo(t)
        // we can't use same descriptor objects for T's as actual type values and same T's as unknowns,
        // because constraints become trivial (T :< T), and inference fails
        //
        // Thus, we replace the parameters of our descriptor with fresh objects (perform alpha-conversion)
        CallableDescriptor candidateWithFreshVariables = FunctionDescriptorUtil.alphaConvertTypeParameters(candidate);


        for (TypeParameterDescriptor typeParameterDescriptor : candidateWithFreshVariables.getTypeParameters()) {
            constraintsSystem.registerTypeVariable(typeParameterDescriptor, Variance.INVARIANT); // TODO: variance of the occurrences
        }

        TypeSubstitutor substituteDontCare = ConstraintSystemWithPriorities
            .makeConstantSubstitutor(candidateWithFreshVariables.getTypeParameters(), ConstraintSystemImpl.DONT_CARE);

        // Value parameters
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : candidateCall.getValueArguments().entrySet()) {
            ResolvedValueArgument resolvedValueArgument = entry.getValue();
            ValueParameterDescriptor valueParameterDescriptor = candidateWithFreshVariables.getValueParameters().get(entry.getKey().getIndex());


            for (ValueArgument valueArgument : resolvedValueArgument.getArguments()) {
                if (JetPsiUtil.isFunctionLiteralWithoutDeclaredParameterTypes(valueArgument.getArgumentExpression())) continue;
                // TODO : more attempts, with different expected types

                // Here we type check expecting an error type (DONT_CARE, substitution with substituteDontCare)
                // and throw the results away
                // We'll type check the arguments later, with the inferred types expected
                boolean[] isErrorType = new boolean[1];
                addConstraintForValueArgument(valueArgument, valueParameterDescriptor, substituteDontCare, constraintsSystem,
                                              context, isErrorType);
                if (isErrorType[0]) {
                    candidateCall.argumentHasNoType();
                }
            }
        }

        // Receiver
        // Error is already reported if something is missing
        ReceiverDescriptor receiverArgument = candidateCall.getReceiverArgument();
        ReceiverParameterDescriptor receiverParameter = candidateWithFreshVariables.getReceiverParameter();
        if (receiverArgument.exists() && receiverParameter.exists()) {
            constraintsSystem.addSubtypeConstraint(receiverParameter.getType(), receiverArgument.getType(),
                                                   ConstraintPosition.RECEIVER_POSITION);
        }

        ConstraintSystem
                constraintSystemWithRightTypeParameters = constraintsSystem.replaceTypeVariables(new Function<TypeParameterDescriptor, TypeParameterDescriptor>() {
            @Override
            public TypeParameterDescriptor apply(@Nullable TypeParameterDescriptor typeParameterDescriptor) {
                assert typeParameterDescriptor != null;
                return candidate.getTypeParameters().get(typeParameterDescriptor.getIndex());
            }
        });
        candidateCall.setConstraintSystem(constraintSystemWithRightTypeParameters);


        // Solution
        boolean hasContradiction = constraintsSystem.hasContradiction();
        boolean boundsAreSatisfied = ConstraintsUtil.checkBoundsAreSatisfied(constraintsSystem);
        if (!hasContradiction && boundsAreSatisfied) {
            candidateCall.setHasUnknownTypeParameters(true);
            return SUCCESS;
        }
        ValueArgumentsCheckingResult checkingResult = checkAllValueArguments(context);
        ResolutionStatus argumentsStatus = checkingResult.status;
        List<JetType> argumentTypes = checkingResult.argumentTypes;
        JetType receiverType = candidateCall.getReceiverArgument().exists() ? candidateCall.getReceiverArgument().getType() : null;
        InferenceErrorData.ExtendedInferenceErrorData inferenceErrorData = InferenceErrorData
                .create(candidate, constraintSystemWithRightTypeParameters, argumentTypes, receiverType, context.expectedType);
        if (hasContradiction) {
            context.tracing.typeInferenceFailed(candidateCall.getTrace(), inferenceErrorData, constraintSystemWithRightTypeParameters);
        }
        else {
            context.tracing.upperBoundViolated(candidateCall.getTrace(), inferenceErrorData);
        }
        return TYPE_INFERENCE_ERROR.combine(argumentsStatus);
    }

    private void addConstraintForValueArgument(
            @NotNull ValueArgument valueArgument,
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull TypeSubstitutor substitutor,
            @NotNull ConstraintSystem constraintSystem,
            @NotNull ResolutionContext context,
            @Nullable boolean[] isErrorType) {

        JetType effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument);
        JetExpression argumentExpression = valueArgument.getArgumentExpression();
        JetType type;
        if (argumentExpression != null) {
            TemporaryBindingTrace traceForUnknown = TemporaryBindingTrace.create(
                    context.trace, "transient trace to resolve argument", argumentExpression);
            type = expressionTypingServices.getType(context.scope, argumentExpression,
                    substitutor.substitute(effectiveExpectedType, Variance.INVARIANT), context.dataFlowInfo, traceForUnknown);
        }
        else {
            type = null;
        }
        constraintSystem.addSubtypeConstraint(effectiveExpectedType, type, ConstraintPosition.getValueParameterPosition(
                valueParameterDescriptor.getIndex()));
        //todo no return
        if (isErrorType != null) {
            isErrorType[0] = type == null || ErrorUtils.isErrorType(type);
        }
    }

    private <D extends CallableDescriptor, F extends D> ValueArgumentsCheckingResult checkAllValueArguments(CallResolutionContext<D, F> context) {
        ValueArgumentsCheckingResult checkingResult = checkValueArgumentTypes(context, context.candidateCall, context.candidateCall.getTrace());
        ResolutionStatus resultStatus = checkingResult.status;
        ResolvedCall<D> candidateCall = context.candidateCall;

        // Comment about a very special case.
        // Call 'b.foo(1)' where class 'Foo' has an extension member 'fun B.invoke(Int)' should be checked two times for safe call (in 'checkReceiver'), because
        // both 'b' (receiver) and 'foo' (this object) might be nullable. In the first case we mark dot, in the second 'foo'.
        // Class 'CallForImplicitInvoke' helps up to recognise this case, and parameter 'implicitInvokeCheck' helps us to distinguish whether we check receiver or this object.

        resultStatus = resultStatus.combine(checkReceiver(context, candidateCall, candidateCall.getResultingDescriptor().getReceiverParameter(), candidateCall.getReceiverArgument(),
                                                          candidateCall.getExplicitReceiverKind().isReceiver(), false));

        resultStatus = resultStatus.combine(checkReceiver(context, candidateCall, candidateCall.getResultingDescriptor().getExpectedThisObject(), candidateCall.getThisObject(),
                                                          candidateCall.getExplicitReceiverKind().isThisObject(),
                                                          // for the invocation 'foo(1)' where foo is a variable of function type we should mark 'foo' if there is unsafe call error
                                                          context.call instanceof CallTransformer.CallForImplicitInvoke));
        return new ValueArgumentsCheckingResult(resultStatus, checkingResult.argumentTypes);
    }

    private <D extends CallableDescriptor> ValueArgumentsCheckingResult checkValueArgumentTypes(
            @NotNull ResolutionContext context,
            @NotNull ResolvedCallImpl<D> candidateCall,
            @NotNull BindingTrace trace) {
        ResolutionStatus resultStatus = SUCCESS;
        DataFlowInfo dataFlowInfo = context.dataFlowInfo;
        List<JetType> argumentTypes = Lists.newArrayList();
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : candidateCall.getValueArguments().entrySet()) {
            ValueParameterDescriptor parameterDescriptor = entry.getKey();
            ResolvedValueArgument resolvedArgument = entry.getValue();


            for (ValueArgument argument : resolvedArgument.getArguments()) {
                JetExpression expression = argument.getArgumentExpression();
                if (expression == null) continue;

                JetType expectedType = getEffectiveExpectedType(parameterDescriptor, argument);
                if (TypeUtils.dependsOnTypeParameters(expectedType, candidateCall.getCandidateDescriptor().getTypeParameters())) {
                    expectedType = NO_EXPECTED_TYPE;
                }
                JetTypeInfo typeInfo = expressionTypingServices.getTypeInfo(context.scope, expression, expectedType, dataFlowInfo, trace);
                JetType type = typeInfo.getType();
                argumentTypes.add(type);
                dataFlowInfo = dataFlowInfo.and(typeInfo.getDataFlowInfo());
                if (type == null || ErrorUtils.isErrorType(type)) {
                    candidateCall.argumentHasNoType();
                }
                else if (expectedType != NO_EXPECTED_TYPE && !typeChecker.isSubtypeOf(type, expectedType)) {
                    //                    VariableDescriptor variableDescriptor = AutoCastUtils.getVariableDescriptorFromSimpleName(temporaryTrace.getBindingContext(), argument);
                    //                    if (variableDescriptor != null) {
                    //                        JetType autoCastType = null;
                    //                        for (JetType possibleType : dataFlowInfo.getPossibleTypesForVariable(variableDescriptor)) {
                    //                            if (semanticServices.getTypeChecker().isSubtypeOf(type, parameterType)) {
                    //                                autoCastType = possibleType;
                    //                                break;
                    //                            }
                    //                        }
                    //                        if (autoCastType != null) {
                    //                            if (AutoCastUtils.isStableVariable(variableDescriptor)) {
                    //                                temporaryTrace.record(AUTOCAST, argument, autoCastType);
                    //                            }
                    //                            else {
                    //                                temporaryTrace.report(AUTOCAST_IMPOSSIBLE.on(argument, autoCastType, variableDescriptor));
                    //                                resultStatus = false;
                    //                            }
                    //                        }
                    //                    }
                    //                    else {
                    resultStatus = OTHER_ERROR;
                }
            }
        }
        return new ValueArgumentsCheckingResult(resultStatus, argumentTypes);
    }

    private void checkUnmappedArgumentTypes(ResolutionContext context, Set<ValueArgument> unmappedArguments) {
        for (ValueArgument valueArgument : unmappedArguments) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                expressionTypingServices.getType(context.scope, argumentExpression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
            }
        }
    }

    private <D extends CallableDescriptor, F extends D> ResolutionStatus checkReceiver(CallResolutionContext<D, F> context, ResolvedCall<D> candidateCall,
            ReceiverParameterDescriptor receiverParameter, ReceiverDescriptor receiverArgument,
            boolean isExplicitReceiver, boolean implicitInvokeCheck) {

        BindingContext bindingContext = context.candidateCall.getTrace().getBindingContext();

        ResolutionStatus result = SUCCESS;
        if (receiverParameter.exists() && receiverArgument.exists()) {
            boolean safeAccess = isExplicitReceiver && !implicitInvokeCheck && candidateCall.isSafeCall();
            JetType receiverArgumentType = receiverArgument.getType();
            AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(context.dataFlowInfo, bindingContext);
            if (!safeAccess && !receiverParameter.getType().isNullable() && !autoCastService.isNotNull(receiverArgument)) {

                context.tracing.unsafeCall(context.candidateCall.getTrace(), receiverArgumentType, implicitInvokeCheck);
                result = UNSAFE_CALL_ERROR;
            }
            else {
                JetType effectiveReceiverArgumentType = safeAccess
                                                        ? TypeUtils.makeNotNullable(receiverArgumentType)
                                                        : receiverArgumentType;
                if (!TypeUtils.dependsOnTypeParameters(receiverParameter.getType(),
                                                       candidateCall.getCandidateDescriptor().getTypeParameters()) &&
                        !typeChecker.isSubtypeOf(effectiveReceiverArgumentType, receiverParameter.getType())) {
                    context.tracing.wrongReceiverType(context.candidateCall.getTrace(), receiverParameter, receiverArgument);
                    result = OTHER_ERROR;
                }
            }
            DataFlowValue receiverValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(receiverArgument, bindingContext);
            if (safeAccess && !context.dataFlowInfo.getNullability(receiverValue).canBeNull()) {
                context.tracing.unnecessarySafeCall(context.candidateCall.getTrace(), receiverArgumentType);
            }
        }
        return result;
    }

    private static class ValueArgumentsCheckingResult {

        public final List<JetType> argumentTypes;
        public final ResolutionStatus status;

        private ValueArgumentsCheckingResult(@NotNull ResolutionStatus status, @NotNull List<JetType> argumentTypes) {
            this.status = status;
            this.argumentTypes = argumentTypes;
        }

    }

    @NotNull
    private static JetType getEffectiveExpectedType(ValueParameterDescriptor parameterDescriptor, ValueArgument argument) {
        if (argument.getSpreadElement() != null) {
            if (parameterDescriptor.getVarargElementType() == null) {
                // Spread argument passed to a non-vararg parameter, an error is already reported by ValueArgumentsToParametersMapper
                return ConstraintSystemImpl.DONT_CARE;
            }
            else {
                return parameterDescriptor.getType();
            }
        }
        else {
            if (argument.isNamed()) {
                return parameterDescriptor.getType();
            }
            else {
                JetType varargElementType = parameterDescriptor.getVarargElementType();
                if (varargElementType == null) {
                    return parameterDescriptor.getType();
                }
                return varargElementType;
            }
        }
    }

    private static <D extends CallableDescriptor> void checkBounds(
            @NotNull ResolvedCallImpl<D> call,
            @NotNull ConstraintSystem constraintSystem,
            @NotNull BindingTrace trace,
            @NotNull TracingStrategy tracing
    ) {
        for (TypeParameterDescriptor typeParameter : call.getCandidateDescriptor().getTypeParameters()) {
            if (!ConstraintsUtil.checkUpperBoundIsSatisfied(constraintSystem, typeParameter)) {
                tracing.upperBoundViolated(trace, InferenceErrorData.create(call.getCandidateDescriptor(), constraintSystem));
            }
        }
    }

    private static void checkGenericBoundsInAFunctionCall(
            @NotNull List<JetTypeProjection> jetTypeArguments,
            @NotNull List<JetType> typeArguments,
            @NotNull CallableDescriptor functionDescriptor,
            @NotNull BindingTrace trace) {
        Map<TypeConstructor, TypeProjection> context = Maps.newHashMap();

        List<TypeParameterDescriptor> typeParameters = functionDescriptor.getOriginal().getTypeParameters();
        for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor typeParameter = typeParameters.get(i);
            JetType typeArgument = typeArguments.get(i);
            context.put(typeParameter.getTypeConstructor(), new TypeProjection(typeArgument));
        }
        TypeSubstitutor substitutor = TypeSubstitutor.create(context);
        for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            JetType typeArgument = typeArguments.get(i);
            JetTypeReference typeReference = jetTypeArguments.get(i).getTypeReference();
            if (typeReference != null) {
                DescriptorResolver.checkBounds(typeReference, typeArgument, typeParameterDescriptor, substitutor, trace);
            }
        }
    }
}
