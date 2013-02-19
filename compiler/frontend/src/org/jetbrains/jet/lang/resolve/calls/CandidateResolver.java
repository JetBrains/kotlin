/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorUtil;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.*;
import org.jetbrains.jet.lang.resolve.calls.context.CallCandidateResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.CallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.ResolveMode;
import org.jetbrains.jet.lang.resolve.calls.context.TypeInfoForCall;
import org.jetbrains.jet.lang.resolve.calls.inference.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionDebugInfo;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.jet.lang.resolve.calls.tasks.TaskPrioritizer;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.expressions.DataFlowUtils;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT;
import static org.jetbrains.jet.lang.diagnostics.Errors.SUPER_IS_NOT_AN_EXPRESSION;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.DONT_CARE;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.PLACEHOLDER_FUNCTION_TYPE;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.SKIP_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus.*;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

public class CandidateResolver {
    @NotNull
    private ArgumentTypeResolver argumentTypeResolver;

    @Inject
    public void setArgumentTypeResolver(@NotNull ArgumentTypeResolver argumentTypeResolver) {
        this.argumentTypeResolver = argumentTypeResolver;
    }

    public <D extends CallableDescriptor, F extends D> void performResolutionForCandidateCall(
            @NotNull CallCandidateResolutionContext<D> context,
            @NotNull ResolutionTask<D, F> task) {

        ProgressIndicatorProvider.checkCanceled();

        ResolvedCallImpl<D> candidateCall = context.candidateCall;
        D candidate = candidateCall.getCandidateDescriptor();

        if (ErrorUtils.isError(candidate)) {
            candidateCall.addStatus(SUCCESS);
            argumentTypeResolver.checkTypesWithNoCallee(context.toBasic());
            return;
        }

        if (!checkOuterClassMemberIsAccessible(context)) {
            candidateCall.addStatus(OTHER_ERROR);
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
                argumentTypeResolver.checkTypesWithNoCallee(context.toBasic());
                return;
            }
            candidateCall.setUnmappedArguments(unmappedArguments);
        }

        List<JetTypeProjection> jetTypeArguments = context.call.getTypeArguments();
        if (jetTypeArguments.isEmpty()) {
            if (!candidate.getTypeParameters().isEmpty()) {
                ResolutionStatus status = inferTypeArguments(context);
                candidateCall.addStatus(status);
            }
            else {
                candidateCall.addStatus(checkAllValueArguments(context, SKIP_FUNCTION_ARGUMENTS).status);
            }
        }
        else {
            // Explicit type arguments passed

            List<JetType> typeArguments = new ArrayList<JetType>();
            for (JetTypeProjection projection : jetTypeArguments) {
                if (projection.getProjectionKind() != JetProjectionKind.NONE) {
                    context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection));
                }
                typeArguments.add(argumentTypeResolver.resolveTypeRefWithDefault(
                        projection.getTypeReference(), context.scope, context.trace, ErrorUtils.createErrorType("Star projection in a call")));
            }
            int expectedTypeArgumentCount = candidate.getTypeParameters().size();
            if (expectedTypeArgumentCount == jetTypeArguments.size()) {

                checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate, context.trace);

                Map<TypeConstructor, TypeProjection>
                        substitutionContext = FunctionDescriptorUtil
                        .createSubstitutionContext((FunctionDescriptor) candidate, typeArguments);
                TypeSubstitutor substitutor = TypeSubstitutor.create(substitutionContext);
                candidateCall.setResultingSubstitutor(substitutor);

                List<TypeParameterDescriptor> typeParameters = candidateCall.getCandidateDescriptor().getTypeParameters();
                for (int i = 0; i < typeParameters.size(); i++) {
                    TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
                    candidateCall.recordTypeArgument(typeParameterDescriptor, typeArguments.get(i));
                }
                candidateCall.addStatus(checkAllValueArguments(context, SKIP_FUNCTION_ARGUMENTS).status);
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

    private static boolean checkOuterClassMemberIsAccessible(@NotNull CallCandidateResolutionContext<?> context) {
        // In "this@Outer.foo()" the error will be reported on "this@Outer" instead
        if (context.call.getExplicitReceiver().exists()) return true;

        ClassDescriptor candidateThis = getDeclaringClass(context.candidateCall.getCandidateDescriptor());
        if (candidateThis == null || candidateThis.getKind().isObject()) return true;

        return DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, context.call.getCallElement(), candidateThis);
    }

    @Nullable
    private static ClassDescriptor getDeclaringClass(@NotNull CallableDescriptor candidate) {
        ReceiverParameterDescriptor expectedThis = candidate.getExpectedThisObject();
        if (expectedThis == null) return null;
        DeclarationDescriptor descriptor = expectedThis.getContainingDeclaration();
        return descriptor instanceof ClassDescriptor ? (ClassDescriptor) descriptor : null;
    }

    public <D extends CallableDescriptor> void completeTypeInferenceDependentOnFunctionLiteralsForCall(
            CallCandidateResolutionContext<D> context
    ) {
        ResolvedCallImpl<D> resolvedCall = context.candidateCall;
        assert resolvedCall.hasIncompleteTypeParameters();
        ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
        assert constraintSystem != null;

        // constraints for function literals
        // Value parameters
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet()) {
            ResolvedValueArgument resolvedValueArgument = entry.getValue();
            ValueParameterDescriptor valueParameterDescriptor = entry.getKey();

            for (ValueArgument valueArgument : resolvedValueArgument.getArguments()) {
                if (!(valueArgument.getArgumentExpression() instanceof JetFunctionLiteralExpression)) continue;

                addConstraintForFunctionLiteral(valueArgument, valueParameterDescriptor, constraintSystem, context);
            }
        }
        resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor());
    }

    public <D extends CallableDescriptor> void completeTypeInferenceDependentOnExpectedTypeForCall(
            @NotNull CallCandidateResolutionContext<D> context,
            boolean isInnerCall
    ) {
        ResolvedCallImpl<D> resolvedCall = context.candidateCall;
        assert resolvedCall.hasIncompleteTypeParameters();
        D descriptor = resolvedCall.getCandidateDescriptor();
        ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
        assert constraintSystem != null;

        ConstraintSystem constraintSystemWithoutExpectedTypeConstraint = constraintSystem.copy();
        constraintSystem.addSupertypeConstraint(context.expectedType, descriptor.getReturnType(),
                                                ConstraintPosition.EXPECTED_TYPE_POSITION);


        if (!constraintSystem.isSuccessful()) {
            resolvedCall.setResultingSubstitutor(constraintSystemWithoutExpectedTypeConstraint.getResultingSubstitutor());
            completeNestedCallsInference(context);
            List<JetType> argumentTypes = checkValueArgumentTypes(context, resolvedCall, context.trace,
                                                                  RESOLVE_FUNCTION_ARGUMENTS).argumentTypes;
            JetType receiverType = resolvedCall.getReceiverArgument().exists() ? resolvedCall.getReceiverArgument().getType() : null;
            context.tracing.typeInferenceFailed(context.trace,
                                                InferenceErrorData
                                                        .create(descriptor, constraintSystem, argumentTypes, receiverType,
                                                                context.expectedType),
                                                constraintSystemWithoutExpectedTypeConstraint);
            resolvedCall.addStatus(ResolutionStatus.OTHER_ERROR);
            return;
        }

        boolean boundsAreSatisfied = ConstraintsUtil.checkBoundsAreSatisfied(constraintSystem, /*substituteOtherTypeParametersInBounds=*/true);
        if (!boundsAreSatisfied) {
            ConstraintSystemImpl copy = (ConstraintSystemImpl) constraintSystem.copy();
            copy.processDeclaredBoundConstraints();
            boundsAreSatisfied = copy.isSuccessful() && ConstraintsUtil.checkBoundsAreSatisfied(copy, /*substituteOtherTypeParametersInBounds=*/true);
            if (boundsAreSatisfied) {
                constraintSystem = copy;
            }
        }
        if (!boundsAreSatisfied) {
            context.tracing.upperBoundViolated(context.trace, InferenceErrorData.create(resolvedCall.getCandidateDescriptor(), constraintSystem));
        }
        resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor());

        completeNestedCallsInference(context);
        // Here we type check the arguments with inferred types expected
        checkAllValueArguments(context, context.trace, RESOLVE_FUNCTION_ARGUMENTS);

        resolvedCall.setHasUnknownTypeParameters(false);
        ResolutionStatus status = resolvedCall.getStatus();
        if (status == ResolutionStatus.UNKNOWN_STATUS || status == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE) {
            resolvedCall.setStatusToSuccess();
        }
        if (isInnerCall) {
            PsiElement callElement = context.call.getCallElement();
            if (callElement instanceof JetCallExpression) {
                DataFlowUtils.checkType(resolvedCall.getResultingDescriptor().getReturnType(), (JetCallExpression) callElement, context, context.dataFlowInfo);
            }
        }
    }

    public <D extends CallableDescriptor> void completeNestedCallsInference(
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        ResolvedCallImpl<D> resolvedCall = context.candidateCall;
        ConstraintSystem constraintSystem = context.candidateCall.getConstraintSystem();
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet()) {
            ValueParameterDescriptor parameterDescriptor = entry.getKey();
            ResolvedValueArgument resolvedArgument = entry.getValue();

            for (ValueArgument argument : resolvedArgument.getArguments()) {
                JetExpression expression = argument.getArgumentExpression();
                if (expression == null) continue;

                JetType effectiveExpectedType = getEffectiveExpectedType(parameterDescriptor, argument);
                JetType expectedType = constraintSystem != null
                                       ? constraintSystem.getCurrentSubstitutor().substitute(effectiveExpectedType, Variance.INVARIANT)
                                       : effectiveExpectedType;

                CallCandidateResolutionContext<FunctionDescriptor> storedContextForArgument =
                        context.trace.get(BindingContext.DEFERRED_COMPUTATION_FOR_CALL, expression);
                if (storedContextForArgument == null) continue;

                CallCandidateResolutionContext<FunctionDescriptor> contextForArgument =
                        storedContextForArgument.replaceResolveMode(ResolveMode.TOP_LEVEL_CALL).replaceBindingTrace(context.trace).replaceExpectedType(expectedType);
                if (contextForArgument.candidateCall.hasIncompleteTypeParameters()) {
                    completeTypeInferenceDependentOnExpectedTypeForCall(contextForArgument, true);
                }
                else {
                    completeNestedCallsInference(contextForArgument);
                }
            }
        }
    }

    private <D extends CallableDescriptor> void addConstraintForFunctionLiteral(
            @NotNull ValueArgument valueArgument,
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull ConstraintSystem constraintSystem,
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        JetExpression argumentExpression = valueArgument.getArgumentExpression();
        assert argumentExpression instanceof JetFunctionLiteralExpression;
        JetType effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument);
        JetType expectedType = constraintSystem.getCurrentSubstitutor().substitute(effectiveExpectedType, Variance.INVARIANT);
        if (expectedType == null || !KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(expectedType)
                || CallResolverUtil.hasUnknownFunctionParameter(expectedType)) {
            return;
        }
        boolean hasExpectedReturnType = !CallResolverUtil.hasUnknownReturnType(expectedType);
        if (hasExpectedReturnType) {
            TemporaryBindingTrace traceToResolveFunctionLiteral = TemporaryBindingTrace.create(
                    context.trace, "trace to resolve function literal with expected return type", argumentExpression);

            JetElement statementExpression = JetPsiUtil.getLastStatementInABlock(((JetFunctionLiteralExpression) argumentExpression).getBodyExpression());
            if (statementExpression == null) return;
            final boolean[] mismatch = new boolean[1];
            ObservableBindingTrace errorInterceptingTrace = ExpressionTypingUtils.makeTraceInterceptingTypeMismatch(
                    traceToResolveFunctionLiteral, statementExpression, mismatch);
            CallCandidateResolutionContext<D> newContext =
                    context.replaceBindingTrace(errorInterceptingTrace).replaceExpectedType(expectedType);
            JetType type = argumentTypeResolver.getArgumentTypeInfo(argumentExpression, newContext, RESOLVE_FUNCTION_ARGUMENTS).getType();
            if (!mismatch[0]) {
                constraintSystem.addSubtypeConstraint(
                        type, effectiveExpectedType, ConstraintPosition.getValueParameterPosition(valueParameterDescriptor.getIndex()));
                traceToResolveFunctionLiteral.commit();
                return;
            }
            BindingContextUtils.commitResolutionCacheData(traceToResolveFunctionLiteral, context.trace);
        }
        JetType expectedTypeWithoutReturnType = hasExpectedReturnType ? CallResolverUtil.replaceReturnTypeByUnknown(expectedType) : expectedType;
        CallCandidateResolutionContext<D> newContext = context.replaceExpectedType(expectedTypeWithoutReturnType);
        JetType type = argumentTypeResolver.getArgumentTypeInfo(argumentExpression, newContext, RESOLVE_FUNCTION_ARGUMENTS).getType();
        constraintSystem.addSubtypeConstraint(
                type, effectiveExpectedType, ConstraintPosition.getValueParameterPosition(valueParameterDescriptor.getIndex()));
    }

    private <D extends CallableDescriptor> ResolutionStatus inferTypeArguments(CallCandidateResolutionContext<D> context) {
        ResolvedCallImpl<D> candidateCall = context.candidateCall;
        final D candidate = candidateCall.getCandidateDescriptor();

        context.trace.get(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, context.call.getCallElement());

        ConstraintSystemImpl constraintSystem = new ConstraintSystemImpl();

        // If the call is recursive, e.g.
        //   fun foo<T>(t : T) : T = foo(t)
        // we can't use same descriptor objects for T's as actual type values and same T's as unknowns,
        // because constraints become trivial (T :< T), and inference fails
        //
        // Thus, we replace the parameters of our descriptor with fresh objects (perform alpha-conversion)
        CallableDescriptor candidateWithFreshVariables = FunctionDescriptorUtil.alphaConvertTypeParameters(candidate);


        for (TypeParameterDescriptor typeParameterDescriptor : candidateWithFreshVariables.getTypeParameters()) {
            constraintSystem.registerTypeVariable(typeParameterDescriptor, Variance.INVARIANT); // TODO: variance of the occurrences
        }

        TypeSubstitutor substituteDontCare = ConstraintSystemWithPriorities
            .makeConstantSubstitutor(candidateWithFreshVariables.getTypeParameters(), DONT_CARE);

        // Value parameters
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : candidateCall.getValueArguments().entrySet()) {
            ResolvedValueArgument resolvedValueArgument = entry.getValue();
            ValueParameterDescriptor valueParameterDescriptor = candidateWithFreshVariables.getValueParameters().get(entry.getKey().getIndex());


            for (ValueArgument valueArgument : resolvedValueArgument.getArguments()) {
                // TODO : more attempts, with different expected types

                // Here we type check expecting an error type (DONT_CARE, substitution with substituteDontCare)
                // and throw the results away
                // We'll type check the arguments later, with the inferred types expected
                boolean[] isErrorType = new boolean[1];
                addConstraintForValueArgument(valueArgument, valueParameterDescriptor, substituteDontCare, constraintSystem,
                                              context, isErrorType, SKIP_FUNCTION_ARGUMENTS);
                if (isErrorType[0]) {
                    candidateCall.argumentHasNoType();
                }
            }
        }

        // Receiver
        // Error is already reported if something is missing
        ReceiverValue receiverArgument = candidateCall.getReceiverArgument();
        ReceiverParameterDescriptor receiverParameter = candidateWithFreshVariables.getReceiverParameter();
        if (receiverArgument.exists() && receiverParameter != null) {
            JetType receiverType =
                    context.candidateCall.isSafeCall()
                    ? TypeUtils.makeNotNullable(receiverArgument.getType())
                    : receiverArgument.getType();
            constraintSystem.addSubtypeConstraint(receiverType, receiverParameter.getType(), ConstraintPosition.RECEIVER_POSITION);
        }

        ConstraintSystem
                constraintSystemWithRightTypeParameters = constraintSystem.replaceTypeVariables(new Function<TypeParameterDescriptor, TypeParameterDescriptor>() {
            @Override
            public TypeParameterDescriptor apply(@Nullable TypeParameterDescriptor typeParameterDescriptor) {
                assert typeParameterDescriptor != null;
                return candidate.getTypeParameters().get(typeParameterDescriptor.getIndex());
            }
        });
        candidateCall.setConstraintSystem(constraintSystemWithRightTypeParameters);


        // Solution
        boolean hasContradiction = constraintSystem.hasContradiction();
        boolean boundsAreSatisfied = ConstraintsUtil.checkBoundsAreSatisfied(constraintSystem, /*substituteOtherTypeParametersInBounds=*/false);
        candidateCall.setHasUnknownTypeParameters(true);
        if (!hasContradiction && boundsAreSatisfied) {
            return INCOMPLETE_TYPE_INFERENCE;
        }
        ValueArgumentsCheckingResult checkingResult = checkAllValueArguments(context, SKIP_FUNCTION_ARGUMENTS);
        ResolutionStatus argumentsStatus = checkingResult.status;
        return OTHER_ERROR.combine(argumentsStatus);
    }

    private <D extends CallableDescriptor> void addConstraintForValueArgument(
            @NotNull ValueArgument valueArgument,
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull TypeSubstitutor substitutor,
            @NotNull ConstraintSystem constraintSystem,
            @NotNull CallCandidateResolutionContext<D> context,
            @Nullable boolean[] isErrorType,
            @NotNull CallResolverUtil.ResolveArgumentsMode resolveFunctionArgumentBodies) {

        JetType effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument);
        JetExpression argumentExpression = valueArgument.getArgumentExpression();
        TemporaryBindingTrace traceToResolveArgument = TemporaryBindingTrace.create(
                context.trace, "transient trace to resolve argument", argumentExpression);
        JetType expectedType = substitutor.substitute(effectiveExpectedType, Variance.INVARIANT);
        CallResolutionContext newContext = context.replaceBindingTrace(traceToResolveArgument).replaceExpectedType(expectedType);
        TypeInfoForCall typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(argumentExpression, newContext,
                                                                                   resolveFunctionArgumentBodies);
        CallCandidateResolutionContext<FunctionDescriptor> contextForArgument = typeInfoForCall.getCallCandidateResolutionContext();
        if (contextForArgument != null) {
            //todo return JetTypeInfo instead of TypeInfoForCall, remove TypeInfoForCall
            traceToResolveArgument.commit();
        }
        JetType type = typeInfoForCall.getType();
        constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, ConstraintPosition.getValueParameterPosition(
                valueParameterDescriptor.getIndex()));
        if (isErrorType != null) {
            isErrorType[0] = type == null || ErrorUtils.isErrorType(type);
        }
    }

    private <D extends CallableDescriptor> ValueArgumentsCheckingResult checkAllValueArguments(
            @NotNull CallCandidateResolutionContext<D> context,
            @NotNull CallResolverUtil.ResolveArgumentsMode resolveFunctionArgumentBodies) {
        return checkAllValueArguments(context, context.candidateCall.getTrace(), resolveFunctionArgumentBodies);
    }

    private <D extends CallableDescriptor> ValueArgumentsCheckingResult checkAllValueArguments(
            @NotNull CallCandidateResolutionContext<D> context,
            @NotNull BindingTrace trace,
            @NotNull CallResolverUtil.ResolveArgumentsMode resolveFunctionArgumentBodies
    ) {
        ValueArgumentsCheckingResult checkingResult = checkValueArgumentTypes(
                context, context.candidateCall, trace, resolveFunctionArgumentBodies);
        ResolutionStatus resultStatus = checkingResult.status;
        ResolvedCall<D> candidateCall = context.candidateCall;

        // Comment about a very special case.
        // Call 'b.foo(1)' where class 'Foo' has an extension member 'fun B.invoke(Int)' should be checked two times for safe call (in 'checkReceiver'), because
        // both 'b' (receiver) and 'foo' (this object) might be nullable. In the first case we mark dot, in the second 'foo'.
        // Class 'CallForImplicitInvoke' helps up to recognise this case, and parameter 'implicitInvokeCheck' helps us to distinguish whether we check receiver or this object.

        resultStatus = resultStatus.combine(checkReceiver(
                context, candidateCall, trace,
                candidateCall.getResultingDescriptor().getReceiverParameter(),
                candidateCall.getReceiverArgument(), candidateCall.getExplicitReceiverKind().isReceiver(), false));

        resultStatus = resultStatus.combine(checkReceiver(
                context, candidateCall, trace,
                candidateCall.getResultingDescriptor().getExpectedThisObject(), candidateCall.getThisObject(),
                candidateCall.getExplicitReceiverKind().isThisObject(),
                // for the invocation 'foo(1)' where foo is a variable of function type we should mark 'foo' if there is unsafe call error
                context.call instanceof CallTransformer.CallForImplicitInvoke));
        return new ValueArgumentsCheckingResult(resultStatus, checkingResult.argumentTypes);
    }

    private <D extends CallableDescriptor, C extends CallResolutionContext<C>> ValueArgumentsCheckingResult checkValueArgumentTypes(
            @NotNull CallResolutionContext<C> context,
            @NotNull ResolvedCallImpl<D> candidateCall,
            @NotNull BindingTrace trace,
            @NotNull CallResolverUtil.ResolveArgumentsMode resolveFunctionArgumentBodies) {
        ResolutionStatus resultStatus = SUCCESS;
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
                CallResolutionContext newContext = context.replaceDataFlowInfo(candidateCall.getDataFlowInfo()).replaceBindingTrace(trace)
                        .replaceExpectedType(expectedType);
                TypeInfoForCall typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(
                        expression, newContext, resolveFunctionArgumentBodies);
                JetType type = typeInfoForCall.getType();
                candidateCall.addDataFlowInfo(typeInfoForCall.getDataFlowInfo());

                if (type == null || (ErrorUtils.isErrorType(type) && type != PLACEHOLDER_FUNCTION_TYPE)) {
                    candidateCall.argumentHasNoType();
                    argumentTypes.add(type);
                }
                else {
                    JetType resultingType;
                    if (expectedType == NO_EXPECTED_TYPE || argumentTypeResolver.isSubtypeOfForArgumentType(type, expectedType)) {
                        resultingType = type;
                    }
                    else {
                        resultingType = autocastValueArgumentTypeIfPossible(expression, expectedType, type, trace, candidateCall.getDataFlowInfo());
                        if (resultingType == null) {
                            resultingType = type;
                            resultStatus = OTHER_ERROR;
                        }
                    }

                    argumentTypes.add(resultingType);
                }
            }
        }
        return new ValueArgumentsCheckingResult(resultStatus, argumentTypes);
    }

    @Nullable
    private JetType autocastValueArgumentTypeIfPossible(
            @NotNull JetExpression expression,
            @NotNull JetType expectedType,
            @NotNull JetType actualType,
            @NotNull BindingTrace trace,
            @NotNull DataFlowInfo dataFlowInfo
    ) {
        ExpressionReceiver receiverToCast = new ExpressionReceiver(expression, actualType);
        List<ReceiverValue> variants = AutoCastUtils.getAutoCastVariants(trace.getBindingContext(), dataFlowInfo, receiverToCast);
        for (ReceiverValue receiverValue : variants) {
            JetType possibleType = receiverValue.getType();
            if (argumentTypeResolver.isSubtypeOfForArgumentType(possibleType, expectedType)) {
                return possibleType;
            }
        }
        return null;
    }

    private <D extends CallableDescriptor> ResolutionStatus checkReceiver(
            CallCandidateResolutionContext<D> context, ResolvedCall<D> candidateCall, BindingTrace trace,
            ReceiverParameterDescriptor receiverParameter, ReceiverValue receiverArgument,
            boolean isExplicitReceiver, boolean implicitInvokeCheck) {

        BindingContext bindingContext = trace.getBindingContext();

        ResolutionStatus result = SUCCESS;
        if (receiverParameter != null && receiverArgument.exists()) {
            boolean safeAccess = isExplicitReceiver && !implicitInvokeCheck && candidateCall.isSafeCall();
            JetType receiverArgumentType = receiverArgument.getType();
            AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(context.dataFlowInfo, bindingContext);
            if (!safeAccess && !receiverParameter.getType().isNullable() && !autoCastService.isNotNull(receiverArgument)) {

                context.tracing.unsafeCall(trace, receiverArgumentType, implicitInvokeCheck);
                result = UNSAFE_CALL_ERROR;
            }
            else {
                JetType effectiveReceiverArgumentType = safeAccess
                                                        ? TypeUtils.makeNotNullable(receiverArgumentType)
                                                        : receiverArgumentType;
                if (!TypeUtils.dependsOnTypeParameters(receiverParameter.getType(),
                                                       candidateCall.getCandidateDescriptor().getTypeParameters()) &&
                        !argumentTypeResolver.isSubtypeOfForArgumentType(effectiveReceiverArgumentType, receiverParameter.getType())) {
                    context.tracing.wrongReceiverType(trace, receiverParameter, receiverArgument);
                    result = OTHER_ERROR;
                }
            }
            DataFlowValue receiverValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(receiverArgument, bindingContext);
            if (safeAccess && !context.dataFlowInfo.getNullability(receiverValue).canBeNull()) {
                context.tracing.unnecessarySafeCall(trace, receiverArgumentType);
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
                return DONT_CARE;
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
