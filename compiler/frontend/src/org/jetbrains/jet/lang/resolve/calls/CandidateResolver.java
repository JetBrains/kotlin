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
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.*;
import org.jetbrains.jet.lang.resolve.calls.context.*;
import org.jetbrains.jet.lang.resolve.calls.inference.*;
import org.jetbrains.jet.lang.resolve.calls.model.*;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionDebugInfo;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.jet.lang.resolve.calls.tasks.TaskPrioritizer;
import org.jetbrains.jet.lang.resolve.calls.util.ExpressionAsFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.DataFlowUtils;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT;
import static org.jetbrains.jet.lang.diagnostics.Errors.SUPER_IS_NOT_AN_EXPRESSION;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.CallTransformer.CallForImplicitInvoke;
import static org.jetbrains.jet.lang.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus.*;
import static org.jetbrains.jet.lang.types.TypeUtils.*;

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

        candidateCall.addStatus(checkReceiverTypeError(context.candidateCall));

        if (ErrorUtils.isError(candidate)) {
            candidateCall.addStatus(SUCCESS);
            markAllArgumentsAsUnmapped(context);
            return;
        }

        if (!checkOuterClassMemberIsAccessible(context)) {
            candidateCall.addStatus(OTHER_ERROR);
            markAllArgumentsAsUnmapped(context);
            return;
        }


        DeclarationDescriptorWithVisibility invisibleMember =
                Visibilities.findInvisibleMember(candidate, context.scope.getContainingDeclaration());
        if (invisibleMember != null) {
            candidateCall.addStatus(OTHER_ERROR);
            context.tracing.invisibleMember(context.trace, invisibleMember);
            markAllArgumentsAsUnmapped(context);
            return;
        }

        if (task.checkArguments == CheckValueArgumentsMode.ENABLED) {
            Set<ValueArgument> unmappedArguments = Sets.newLinkedHashSet();
            ValueArgumentsToParametersMapper.Status
                    argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(context.call, context.tracing,
                                                                                                            candidateCall, unmappedArguments);
            if (!argumentMappingStatus.isSuccess()) {
                if (argumentMappingStatus == ValueArgumentsToParametersMapper.Status.STRONG_ERROR) {
                    candidateCall.addStatus(RECEIVER_PRESENCE_ERROR);
                }
                else {
                    candidateCall.addStatus(OTHER_ERROR);
                }
                candidateCall.setUnmappedArguments(unmappedArguments);
                if ((argumentMappingStatus == ValueArgumentsToParametersMapper.Status.ERROR && candidate.getTypeParameters().isEmpty()) ||
                    argumentMappingStatus == ValueArgumentsToParametersMapper.Status.STRONG_ERROR) {
                    return;
                }
            }
        }

        List<JetTypeProjection> jetTypeArguments = context.call.getTypeArguments();
        if (jetTypeArguments.isEmpty()) {
            if (!candidate.getTypeParameters().isEmpty()) {
                ResolutionStatus status = inferTypeArguments(context);
                candidateCall.addStatus(status);
            }
            else {
                candidateCall.addStatus(checkAllValueArguments(context, SHAPE_FUNCTION_ARGUMENTS).status);
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

                candidateCall.addStatus(checkAllValueArguments(context, SHAPE_FUNCTION_ARGUMENTS).status);
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

    private static void markAllArgumentsAsUnmapped(CallCandidateResolutionContext<?> context) {
        if (context.checkArguments == CheckValueArgumentsMode.ENABLED) {
            context.candidateCall.setUnmappedArguments(context.call.getValueArguments());
        }
    }

    private static boolean checkOuterClassMemberIsAccessible(@NotNull CallCandidateResolutionContext<?> context) {
        // In "this@Outer.foo()" the error will be reported on "this@Outer" instead
        if (context.call.getExplicitReceiver().exists() || context.call.getThisObject().exists()) return true;

        ClassDescriptor candidateThis = getDeclaringClass(context.candidateCall.getCandidateDescriptor());
        if (candidateThis == null || candidateThis.getKind().isSingleton()) return true;

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
        ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
        if (!resolvedCall.hasIncompleteTypeParameters() || constraintSystem == null) return;

        // constraints for function literals
        // Value parameters
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet()) {
            ResolvedValueArgument resolvedValueArgument = entry.getValue();
            ValueParameterDescriptor valueParameterDescriptor = entry.getKey();

            for (ValueArgument valueArgument : resolvedValueArgument.getArguments()) {
                addConstraintForFunctionLiteral(valueArgument, valueParameterDescriptor, constraintSystem, context);
            }
        }
        resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor());
    }

    @Nullable
    public <D extends CallableDescriptor> JetType completeTypeInferenceDependentOnExpectedTypeForCall(
            @NotNull CallCandidateResolutionContext<D> context,
            boolean isInnerCall
    ) {
        ResolvedCallImpl<D> resolvedCall = context.candidateCall;
        assert resolvedCall.hasIncompleteTypeParameters();
        assert resolvedCall.getConstraintSystem() != null;

        JetType unsubstitutedReturnType = resolvedCall.getCandidateDescriptor().getReturnType();
        if (unsubstitutedReturnType != null) {
            resolvedCall.getConstraintSystem().addSupertypeConstraint(
                    context.expectedType, unsubstitutedReturnType, ConstraintPosition.EXPECTED_TYPE_POSITION);
        }

        updateSystemWithConstraintSystemCompleter(context, resolvedCall);

        updateSystemIfExpectedTypeIsUnit(context, resolvedCall);

        ((ConstraintSystemImpl)resolvedCall.getConstraintSystem()).processDeclaredBoundConstraints();

        if (!resolvedCall.getConstraintSystem().getStatus().isSuccessful()) {
            return reportInferenceError(context);
        }
        resolvedCall.setResultingSubstitutor(resolvedCall.getConstraintSystem().getResultingSubstitutor());

        completeNestedCallsInference(context);
        // Here we type check the arguments with inferred types expected
        checkAllValueArguments(context, context.trace, RESOLVE_FUNCTION_ARGUMENTS);

        resolvedCall.setHasUnknownTypeParameters(false);
        ResolutionStatus status = resolvedCall.getStatus();
        if (status == ResolutionStatus.UNKNOWN_STATUS || status == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE) {
            resolvedCall.setStatusToSuccess();
        }
        JetType returnType = resolvedCall.getResultingDescriptor().getReturnType();
        if (isInnerCall) {
            PsiElement callElement = context.call.getCallElement();
            if (callElement instanceof JetCallExpression) {
                DataFlowUtils.checkType(returnType, (JetCallExpression) callElement, context, context.dataFlowInfo);
            }
        }
        return returnType;
    }

    private static <D extends CallableDescriptor> void updateSystemWithConstraintSystemCompleter(
            @NotNull CallCandidateResolutionContext<D> context,
            @NotNull ResolvedCallImpl<D> resolvedCall
    ) {
        ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
        assert constraintSystem != null;
        ConstraintSystemCompleter constraintSystemCompleter = context.trace.get(
                BindingContext.CONSTRAINT_SYSTEM_COMPLETER, context.call.getCalleeExpression());
        if (constraintSystemCompleter == null) return;

        ConstraintSystem copy = constraintSystem.copy();

        constraintSystemCompleter.completeConstraintSystem(copy, resolvedCall);

        //todo improve error reporting with errors in constraints from completer
        if (!copy.getStatus().hasOnlyErrorsFromPosition(ConstraintPosition.FROM_COMPLETER)) {
            resolvedCall.setConstraintSystem(copy);
        }
    }

    private static <D extends CallableDescriptor> void updateSystemIfExpectedTypeIsUnit(
            @NotNull CallCandidateResolutionContext<D> context,
            @NotNull ResolvedCallImpl<D> resolvedCall
    ) {
        ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
        assert constraintSystem != null;
        JetType returnType = resolvedCall.getCandidateDescriptor().getReturnType();
        if (returnType == null) return;

        if (!constraintSystem.getStatus().isSuccessful() && context.expectedType == TypeUtils.UNIT_EXPECTED_TYPE) {
            ConstraintSystemImpl copy = (ConstraintSystemImpl) constraintSystem.copy();

            copy.addSupertypeConstraint(KotlinBuiltIns.getInstance().getUnitType(), returnType, ConstraintPosition.EXPECTED_TYPE_POSITION);
            if (copy.getStatus().isSuccessful()) {
                resolvedCall.setConstraintSystem(copy);
            }
        }
    }

    private <D extends CallableDescriptor> JetType reportInferenceError(
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        ResolvedCallImpl<D> resolvedCall = context.candidateCall;
        ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
        assert constraintSystem != null;

        resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor());
        completeNestedCallsInference(context);
        List<JetType> argumentTypes = checkValueArgumentTypes(
                context, resolvedCall, context.trace, RESOLVE_FUNCTION_ARGUMENTS).argumentTypes;
        JetType receiverType = resolvedCall.getReceiverArgument().exists() ? resolvedCall.getReceiverArgument().getType() : null;
        InferenceErrorData errorData = InferenceErrorData
                .create(resolvedCall.getCandidateDescriptor(), constraintSystem, argumentTypes, receiverType, context.expectedType);

        context.tracing.typeInferenceFailed(context.trace, errorData);
        resolvedCall.addStatus(ResolutionStatus.OTHER_ERROR);
        if (!resolvedCall.hasInferredReturnType()) return null;
        return resolvedCall.getResultingDescriptor().getReturnType();
    }

    public <D extends CallableDescriptor> void completeNestedCallsInference(
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        if (context.call.getCallType() == Call.CallType.INVOKE) return;
        ResolvedCallImpl<D> resolvedCall = context.candidateCall;
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet()) {
            ValueParameterDescriptor parameterDescriptor = entry.getKey();
            ResolvedValueArgument resolvedArgument = entry.getValue();

            for (ValueArgument argument : resolvedArgument.getArguments()) {
                completeInferenceForArgument(argument, parameterDescriptor, context);
            }
        }
        completeUnmappedArguments(context, context.candidateCall.getUnmappedArguments());
        recordReferenceForInvokeFunction(context);
    }

    private <D extends CallableDescriptor> void completeInferenceForArgument(
            @NotNull ValueArgument argument,
            @NotNull ValueParameterDescriptor parameterDescriptor,
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        JetExpression expression = argument.getArgumentExpression();
        if (expression == null) return;

        JetType expectedType = getEffectiveExpectedType(parameterDescriptor, argument);
        context = context.replaceExpectedType(expectedType);

        JetExpression keyExpression = getDeferredComputationKeyExpression(expression);
        CallCandidateResolutionContext<? extends CallableDescriptor> storedContextForArgument =
                context.resolutionResultsCache.getDeferredComputation(keyExpression);

        PsiElement parent = expression.getParent();
        if (parent instanceof JetWhenExpression && expression == ((JetWhenExpression) parent).getSubjectExpression()
            || (expression instanceof JetFunctionLiteralExpression)) {
            return;
        }
        if (storedContextForArgument == null) {
            JetType type = ArgumentTypeResolver.updateResultArgumentTypeIfNotDenotable(context, expression);
            checkResultArgumentType(type, argument, context);
            return;
        }

        CallCandidateResolutionContext<? extends CallableDescriptor> contextForArgument = storedContextForArgument
                .replaceContextDependency(INDEPENDENT).replaceBindingTrace(context.trace).replaceExpectedType(expectedType);
        JetType type;
        if (contextForArgument.candidateCall.hasIncompleteTypeParameters()) {
            type = completeTypeInferenceDependentOnExpectedTypeForCall(contextForArgument, true);
        }
        else {
            completeNestedCallsInference(contextForArgument);
            JetType recordedType = context.trace.get(BindingContext.EXPRESSION_TYPE, expression);
            if (recordedType != null && !recordedType.getConstructor().isDenotable()) {
                type = ArgumentTypeResolver.updateResultArgumentTypeIfNotDenotable(context, expression);
            }
            else {
                type = contextForArgument.candidateCall.getResultingDescriptor().getReturnType();
            }
            checkValueArgumentTypes(contextForArgument);
        }
        JetType result = BindingContextUtils.updateRecordedType(
                type, expression, context.trace, isFairSafeCallExpression(expression, context.trace));

        markResultingCallAsCompleted(context, keyExpression);

        DataFlowUtils.checkType(result, expression, contextForArgument);
    }

    public void completeNestedCallsForNotResolvedInvocation(@NotNull CallResolutionContext<?> context) {
        completeNestedCallsForNotResolvedInvocation(context, context.call.getValueArguments());
    }

    public void completeUnmappedArguments(@NotNull CallResolutionContext<?> context, @NotNull Collection<? extends ValueArgument> unmappedArguments) {
        completeNestedCallsForNotResolvedInvocation(context, unmappedArguments);
    }

    private void completeNestedCallsForNotResolvedInvocation(@NotNull CallResolutionContext<?> context, @NotNull Collection<? extends ValueArgument> arguments) {
        if (context.call.getCallType() == Call.CallType.INVOKE) return;
        if (context.checkArguments == CheckValueArgumentsMode.DISABLED) return;

        for (ValueArgument argument : arguments) {
            JetExpression expression = argument.getArgumentExpression();

            JetExpression keyExpression = getDeferredComputationKeyExpression(expression);
            markResultingCallAsCompleted(context, keyExpression);

            CallCandidateResolutionContext<? extends CallableDescriptor> storedContextForArgument =
                    context.resolutionResultsCache.getDeferredComputation(keyExpression);
            if (storedContextForArgument != null) {
                completeNestedCallsForNotResolvedInvocation(storedContextForArgument);
                CallCandidateResolutionContext<? extends CallableDescriptor> newContext =
                        storedContextForArgument.replaceBindingTrace(context.trace);
                completeUnmappedArguments(newContext, storedContextForArgument.candidateCall.getUnmappedArguments());
                argumentTypeResolver.checkTypesForFunctionArgumentsWithNoCallee(newContext.replaceContextDependency(INDEPENDENT));
            }
        }
    }

    private static void markResultingCallAsCompleted(
            @NotNull CallResolutionContext<?> context,
            @Nullable JetExpression keyExpression
    ) {
        if (keyExpression == null) return;

        CallCandidateResolutionContext<? extends CallableDescriptor> storedContextForArgument =
                context.resolutionResultsCache.getDeferredComputation(keyExpression);
        if (storedContextForArgument == null) return;

        storedContextForArgument.candidateCall.markCallAsCompleted();

        // clean data for "invoke" calls
        ResolvedCallWithTrace<? extends CallableDescriptor> resolvedCall = context.resolutionResultsCache.getCallForArgument(keyExpression);
        assert resolvedCall != null : "Resolved call for '" + keyExpression + "' is not stored, but CallCandidateResolutionContext is.";
        resolvedCall.markCallAsCompleted();
    }

    @Nullable
    private JetExpression getDeferredComputationKeyExpression(@Nullable JetExpression expression) {
        if (expression == null) return null;
        return expression.accept(new JetVisitor<JetExpression, Void>() {
            @Nullable
            private JetExpression visitInnerExpression(@Nullable JetElement expression) {
                if (expression == null) return null;
                return expression.accept(this, null);
            }

            @Override
            public JetExpression visitQualifiedExpression(@NotNull JetQualifiedExpression expression, Void data) {
                return visitInnerExpression(expression.getSelectorExpression());
            }

            @Override
            public JetExpression visitExpression(@NotNull JetExpression expression, Void data) {
                return expression;
            }

            @Override
            public JetExpression visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, Void data) {
                return visitInnerExpression(expression.getExpression());
            }

            @Override
            public JetExpression visitUnaryExpression(@NotNull JetUnaryExpression expression, Void data) {
                return ExpressionTypingUtils.isUnaryExpressionDependentOnExpectedType(expression) ? expression : null;
            }

            @Override
            public JetExpression visitPrefixExpression(@NotNull JetPrefixExpression expression, Void data) {
                return visitInnerExpression(JetPsiUtil.getBaseExpressionIfLabeledExpression(expression));
            }

            @Override
            public JetExpression visitBlockExpression(@NotNull JetBlockExpression expression, Void data) {
                JetElement lastStatement = JetPsiUtil.getLastStatementInABlock(expression);
                if (lastStatement != null) {
                    return visitInnerExpression(lastStatement);
                }
                return expression;
            }

            @Override
            public JetExpression visitBinaryExpression(@NotNull JetBinaryExpression expression, Void data) {
                return ExpressionTypingUtils.isBinaryExpressionDependentOnExpectedType(expression) ? expression : null;
            }
        }, null);
    }

    private static boolean isFairSafeCallExpression(@NotNull JetExpression expression, @NotNull BindingTrace trace) {
        // We are interested in type of the last call:
        // 'a.b?.foo()' is safe call, but 'a?.b.foo()' is not.
        // Since receiver is 'a.b' and selector is 'foo()',
        // we can only check if an expression is safe call.
        if (!(expression instanceof JetSafeQualifiedExpression)) return false;

        JetSafeQualifiedExpression safeQualifiedExpression = (JetSafeQualifiedExpression) expression;
        //If a receiver type is not null, then this safe expression is useless, and we don't need to make the result type nullable.
        JetType type = trace.get(BindingContext.EXPRESSION_TYPE, safeQualifiedExpression.getReceiverExpression());
        return type != null && type.isNullable();
    }

    private static <D extends CallableDescriptor> void checkResultArgumentType(
            @Nullable JetType type,
            @NotNull ValueArgument argument,
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        JetExpression expression = argument.getArgumentExpression();
        if (expression == null) return;

        DataFlowInfo dataFlowInfoForValueArgument = context.candidateCall.getDataFlowInfoForArguments().getInfo(argument);
        ResolutionContext<?> newContext = context.replaceExpectedType(context.expectedType).replaceDataFlowInfo(
                dataFlowInfoForValueArgument);
        DataFlowUtils.checkType(type, expression, newContext);
    }

    private static <D extends CallableDescriptor> void recordReferenceForInvokeFunction(CallCandidateResolutionContext<D> context) {
        PsiElement callElement = context.call.getCallElement();
        if (!(callElement instanceof JetCallExpression)) return;

        JetCallExpression callExpression = (JetCallExpression) callElement;
        CallableDescriptor resultingDescriptor = context.candidateCall.getResultingDescriptor();
        if (BindingContextUtils.isCallExpressionWithValidReference(callExpression, context.trace.getBindingContext())) {
            context.trace.record(BindingContext.EXPRESSION_TYPE, callExpression, resultingDescriptor.getReturnType());
            context.trace.record(BindingContext.REFERENCE_TARGET, callExpression, resultingDescriptor);
        }
    }

    private <D extends CallableDescriptor> void addConstraintForFunctionLiteral(
            @NotNull ValueArgument valueArgument,
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull ConstraintSystem constraintSystem,
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        JetExpression argumentExpression = valueArgument.getArgumentExpression();
        if (argumentExpression == null) return;
        if (!ArgumentTypeResolver.isFunctionLiteralArgument(argumentExpression)) return;

        JetFunctionLiteralExpression functionLiteralExpression = ArgumentTypeResolver.getFunctionLiteralArgument(argumentExpression);

        JetType effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument);
        JetType expectedType = constraintSystem.getCurrentSubstitutor().substitute(effectiveExpectedType, Variance.INVARIANT);
        if (expectedType == null || expectedType == DONT_CARE) {
            expectedType = argumentTypeResolver.getShapeTypeOfFunctionLiteral(functionLiteralExpression, context.scope, context.trace, false);
        }
        if (expectedType == null || !KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(expectedType)
                || CallResolverUtil.hasUnknownFunctionParameter(expectedType)) {
            return;
        }
        MutableDataFlowInfoForArguments dataFlowInfoForArguments = context.candidateCall.getDataFlowInfoForArguments();
        DataFlowInfo dataFlowInfoForArgument = dataFlowInfoForArguments.getInfo(valueArgument);

        //todo analyze function literal body once in 'dependent' mode, then complete it with respect to expected type
        boolean hasExpectedReturnType = !CallResolverUtil.hasUnknownReturnType(expectedType);
        if (hasExpectedReturnType) {
            TemporaryTraceAndCache temporaryToResolveFunctionLiteral = TemporaryTraceAndCache.create(
                    context, "trace to resolve function literal with expected return type", argumentExpression);

            JetElement statementExpression = JetPsiUtil.getLastStatementInABlock(functionLiteralExpression.getBodyExpression());
            if (statementExpression == null) return;
            boolean[] mismatch = new boolean[1];
            ObservableBindingTrace errorInterceptingTrace = ExpressionTypingUtils.makeTraceInterceptingTypeMismatch(
                    temporaryToResolveFunctionLiteral.trace, statementExpression, mismatch);
            CallCandidateResolutionContext<D> newContext = context
                    .replaceBindingTrace(errorInterceptingTrace).replaceExpectedType(expectedType)
                    .replaceDataFlowInfo(dataFlowInfoForArgument).replaceResolutionResultsCache(temporaryToResolveFunctionLiteral.cache)
                    .replaceContextDependency(INDEPENDENT);
            JetType type = argumentTypeResolver.getFunctionLiteralTypeInfo(
                    argumentExpression, functionLiteralExpression, newContext, RESOLVE_FUNCTION_ARGUMENTS).getType();
            if (!mismatch[0]) {
                constraintSystem.addSubtypeConstraint(
                        type, effectiveExpectedType, ConstraintPosition.getValueParameterPosition(valueParameterDescriptor.getIndex()));
                temporaryToResolveFunctionLiteral.commit();
                return;
            }
        }
        JetType expectedTypeWithoutReturnType = hasExpectedReturnType ? CallResolverUtil.replaceReturnTypeByUnknown(expectedType) : expectedType;
        CallCandidateResolutionContext<D> newContext = context
                .replaceExpectedType(expectedTypeWithoutReturnType).replaceDataFlowInfo(dataFlowInfoForArgument)
                .replaceContextDependency(INDEPENDENT);
        JetType type = argumentTypeResolver.getFunctionLiteralTypeInfo(argumentExpression, functionLiteralExpression, newContext,
                                                                       RESOLVE_FUNCTION_ARGUMENTS).getType();
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

        Map<TypeParameterDescriptor, Variance> typeVariables = Maps.newLinkedHashMap();
        for (TypeParameterDescriptor typeParameterDescriptor : candidateWithFreshVariables.getTypeParameters()) {
            typeVariables.put(typeParameterDescriptor, Variance.INVARIANT); // TODO: variance of the occurrences
        }
        constraintSystem.registerTypeVariables(typeVariables);

        TypeSubstitutor substituteDontCare =
                makeConstantSubstitutor(candidateWithFreshVariables.getTypeParameters(), DONT_CARE);

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
                                              context, isErrorType, SHAPE_FUNCTION_ARGUMENTS);
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
            if (receiverArgument instanceof ExpressionReceiver) {
                receiverType = updateResultTypeForSmartCasts(receiverType, ((ExpressionReceiver) receiverArgument).getExpression(),
                                                             context.dataFlowInfo, context.trace);
            }
            constraintSystem.addSubtypeConstraint(receiverType, receiverParameter.getType(), ConstraintPosition.RECEIVER_POSITION);
        }

        // Restore type variables before alpha-conversion
        ConstraintSystem constraintSystemWithRightTypeParameters = constraintSystem.substituteTypeVariables(
                new Function<TypeParameterDescriptor, TypeParameterDescriptor>() {
                    @Override
                    public TypeParameterDescriptor apply(@Nullable TypeParameterDescriptor typeParameterDescriptor) {
                        assert typeParameterDescriptor != null;
                        return candidate.getTypeParameters().get(typeParameterDescriptor.getIndex());
                    }
                });
        candidateCall.setConstraintSystem(constraintSystemWithRightTypeParameters);


        // Solution
        boolean hasContradiction = constraintSystem.getStatus().hasContradiction();
        candidateCall.setHasUnknownTypeParameters(true);
        if (!hasContradiction) {
            return INCOMPLETE_TYPE_INFERENCE;
        }
        ValueArgumentsCheckingResult checkingResult = checkAllValueArguments(context, SHAPE_FUNCTION_ARGUMENTS);
        ResolutionStatus argumentsStatus = checkingResult.status;
        return OTHER_ERROR.combine(argumentsStatus);
    }

    private void addConstraintForValueArgument(
            @NotNull ValueArgument valueArgument,
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull TypeSubstitutor substitutor,
            @NotNull ConstraintSystem constraintSystem,
            @NotNull CallCandidateResolutionContext<?> context,
            @Nullable boolean[] isErrorType,
            @NotNull CallResolverUtil.ResolveArgumentsMode resolveFunctionArgumentBodies) {

        JetType effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument);
        JetExpression argumentExpression = valueArgument.getArgumentExpression();

        JetType expectedType = substitutor.substitute(effectiveExpectedType, Variance.INVARIANT);
        DataFlowInfo dataFlowInfoForArgument = context.candidateCall.getDataFlowInfoForArguments().getInfo(valueArgument);
        CallResolutionContext<?> newContext = context.replaceExpectedType(expectedType).replaceDataFlowInfo(dataFlowInfoForArgument);

        JetTypeInfo typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(
                argumentExpression, newContext, resolveFunctionArgumentBodies);
        context.candidateCall.getDataFlowInfoForArguments().updateInfo(valueArgument, typeInfoForCall.getDataFlowInfo());

        JetType type = updateResultTypeForSmartCasts(typeInfoForCall.getType(), argumentExpression, dataFlowInfoForArgument, context.trace);
        constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, ConstraintPosition.getValueParameterPosition(
                valueParameterDescriptor.getIndex()));
        if (isErrorType != null) {
            isErrorType[0] = type == null || type.isError();
        }
    }

    @Nullable
    private static JetType updateResultTypeForSmartCasts(
            @Nullable JetType type,
            @Nullable JetExpression argumentExpression,
            @NotNull DataFlowInfo dataFlowInfoForArgument,
            @NotNull BindingTrace trace
    ) {
        if (argumentExpression == null || type == null) return type;

        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(
                argumentExpression, type, trace.getBindingContext());
        if (!dataFlowValue.isStableIdentifier()) return type;

        Set<JetType> possibleTypes = dataFlowInfoForArgument.getPossibleTypes(dataFlowValue);
        if (possibleTypes.isEmpty()) return type;

        return TypeUtils.intersect(JetTypeChecker.INSTANCE, possibleTypes);
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
        resultStatus = resultStatus.combine(checkReceivers(context, trace));

        return new ValueArgumentsCheckingResult(resultStatus, checkingResult.argumentTypes);
    }

    private static <D extends CallableDescriptor> ResolutionStatus checkReceivers(
            @NotNull CallCandidateResolutionContext<D> context,
            @NotNull BindingTrace trace
    ) {
        ResolutionStatus resultStatus = SUCCESS;
        ResolvedCall<D> candidateCall = context.candidateCall;

        resultStatus = resultStatus.combine(checkReceiverTypeError(candidateCall));

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
                context.call instanceof CallForImplicitInvoke));
        return resultStatus;
    }

    public <D extends CallableDescriptor> ValueArgumentsCheckingResult checkValueArgumentTypes(
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        return checkValueArgumentTypes(context, context.candidateCall, context.trace, RESOLVE_FUNCTION_ARGUMENTS);
    }

    private <D extends CallableDescriptor, C extends CallResolutionContext<C>> ValueArgumentsCheckingResult checkValueArgumentTypes(
            @NotNull CallResolutionContext<C> context,
            @NotNull ResolvedCallImpl<D> candidateCall,
            @NotNull BindingTrace trace,
            @NotNull CallResolverUtil.ResolveArgumentsMode resolveFunctionArgumentBodies) {
        ResolutionStatus resultStatus = SUCCESS;
        List<JetType> argumentTypes = Lists.newArrayList();
        MutableDataFlowInfoForArguments infoForArguments = candidateCall.getDataFlowInfoForArguments();
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

                CallResolutionContext<?> newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument))
                        .replaceBindingTrace(trace).replaceExpectedType(expectedType);
                JetTypeInfo typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(
                        expression, newContext, resolveFunctionArgumentBodies);
                JetType type = typeInfoForCall.getType();
                infoForArguments.updateInfo(argument, typeInfoForCall.getDataFlowInfo());

                if (type == null || (type.isError() && type != PLACEHOLDER_FUNCTION_TYPE)) {
                    candidateCall.argumentHasNoType();
                    argumentTypes.add(type);
                }
                else {
                    JetType resultingType;
                    if (noExpectedType(expectedType) || ArgumentTypeResolver.isSubtypeOfForArgumentType(type, expectedType)) {
                        resultingType = type;
                    }
                    else {
                        resultingType = autocastValueArgumentTypeIfPossible(expression, expectedType, type, newContext);
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
    private static JetType autocastValueArgumentTypeIfPossible(
            @NotNull JetExpression expression,
            @NotNull JetType expectedType,
            @NotNull JetType actualType,
            @NotNull ResolutionContext<?> context
    ) {
        ExpressionReceiver receiverToCast = new ExpressionReceiver(JetPsiUtil.safeDeparenthesize(expression, false), actualType);
        List<ReceiverValue> variants =
                AutoCastUtils.getAutoCastVariantsExcludingReceiver(context.trace.getBindingContext(), context.dataFlowInfo, receiverToCast);
        for (ReceiverValue receiverValue : variants) {
            JetType possibleType = receiverValue.getType();
            if (JetTypeChecker.INSTANCE.isSubtypeOf(possibleType, expectedType)) {
                return possibleType;
            }
        }
        return null;
    }

    private static <D extends CallableDescriptor> ResolutionStatus checkReceiverTypeError(
            @NotNull ResolvedCall<D> candidateCall
    ) {
        D candidateDescriptor = candidateCall.getCandidateDescriptor();
        if (candidateDescriptor instanceof ExpressionAsFunctionDescriptor) return SUCCESS;

        ReceiverParameterDescriptor receiverDescriptor = candidateDescriptor.getReceiverParameter();
        ReceiverParameterDescriptor expectedThisObjectDescriptor = candidateDescriptor.getExpectedThisObject();
        ReceiverParameterDescriptor receiverParameterDescriptor;
        JetType receiverArgumentType;
        if (receiverDescriptor != null && candidateCall.getReceiverArgument().exists()) {
            receiverParameterDescriptor = receiverDescriptor;
            receiverArgumentType = candidateCall.getReceiverArgument().getType();
        }
        else if (expectedThisObjectDescriptor != null && candidateCall.getThisObject().exists()) {
            receiverParameterDescriptor = expectedThisObjectDescriptor;
            receiverArgumentType = candidateCall.getThisObject().getType();
        }
        else {
            return SUCCESS;
        }

        JetType effectiveReceiverArgumentType = TypeUtils.makeNotNullable(receiverArgumentType);
        JetType erasedReceiverType = CallResolverUtil.getErasedReceiverType(receiverParameterDescriptor, candidateDescriptor);

        if (!JetTypeChecker.INSTANCE.isSubtypeOf(effectiveReceiverArgumentType, erasedReceiverType)) {
            return RECEIVER_TYPE_ERROR;
        }
        return SUCCESS;
    }

    private static <D extends CallableDescriptor> ResolutionStatus checkReceiver(
            @NotNull CallCandidateResolutionContext<D> context,
            @NotNull ResolvedCall<D> candidateCall,
            @NotNull BindingTrace trace,
            @Nullable ReceiverParameterDescriptor receiverParameter,
            @NotNull ReceiverValue receiverArgument,
            boolean isExplicitReceiver,
            boolean implicitInvokeCheck
    ) {
        if (receiverParameter == null || !receiverArgument.exists()) return SUCCESS;

        JetType receiverArgumentType = receiverArgument.getType();
        JetType effectiveReceiverArgumentType = TypeUtils.makeNotNullable(receiverArgumentType);
        D candidateDescriptor = candidateCall.getCandidateDescriptor();
        if (!ArgumentTypeResolver.isSubtypeOfForArgumentType(effectiveReceiverArgumentType, receiverParameter.getType())
                && !TypeUtils.dependsOnTypeParameters(receiverParameter.getType(), candidateDescriptor.getTypeParameters())) {
            context.tracing.wrongReceiverType(trace, receiverParameter, receiverArgument);
            return OTHER_ERROR;
        }

        BindingContext bindingContext = trace.getBindingContext();
        boolean safeAccess = isExplicitReceiver && !implicitInvokeCheck && candidateCall.isSafeCall();
        if (!safeAccess && !receiverParameter.getType().isNullable() && receiverArgument.getType().isNullable()) {
            if (!AutoCastUtils.isNotNull(receiverArgument, bindingContext, context.dataFlowInfo)) {

                context.tracing.unsafeCall(trace, receiverArgumentType, implicitInvokeCheck);
                return UNSAFE_CALL_ERROR;
            }
            if (isExplicitReceiver) {
                AutoCastUtils.recordAutoCastToNotNullableType(receiverArgument, context.trace);
            }
        }
        DataFlowValue receiverValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, bindingContext);
        if (safeAccess && !context.dataFlowInfo.getNullability(receiverValue).canBeNull()) {
            context.tracing.unnecessarySafeCall(trace, receiverArgumentType);
        }
        return SUCCESS;
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
            JetType varargElementType = parameterDescriptor.getVarargElementType();
            if (varargElementType != null) {
                return varargElementType;
            }

            return parameterDescriptor.getType();
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
            context.put(typeParameter.getTypeConstructor(), new TypeProjectionImpl(typeArgument));
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
