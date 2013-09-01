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
import org.jetbrains.jet.lang.resolve.calls.context.*;
import org.jetbrains.jet.lang.resolve.calls.inference.*;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionDebugInfo;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.jet.lang.resolve.calls.tasks.TaskPrioritizer;
import org.jetbrains.jet.lang.resolve.calls.util.ExpressionAsFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantResolver;
import org.jetbrains.jet.lang.resolve.constants.ErrorValue;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.DataFlowUtils;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.DONT_CARE;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.PLACEHOLDER_FUNCTION_TYPE;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.SKIP_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.CallTransformer.CallForImplicitInvoke;
import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus.*;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.jet.lang.types.TypeUtils.noExpectedType;

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
            argumentTypeResolver.checkTypesWithNoCallee(context.toBasic());
            return;
        }

        if (!checkOuterClassMemberIsAccessible(context)) {
            candidateCall.addStatus(OTHER_ERROR);
            return;
        }


        DeclarationDescriptorWithVisibility invisibleMember =
                Visibilities.findInvisibleMember(candidate, context.scope.getContainingDeclaration());
        if (invisibleMember != null) {
            candidateCall.addStatus(OTHER_ERROR);
            context.tracing.invisibleMember(context.trace, invisibleMember);
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
                if ((argumentMappingStatus == ValueArgumentsToParametersMapper.Status.ERROR && candidate.getTypeParameters().isEmpty()) ||
                    argumentMappingStatus == ValueArgumentsToParametersMapper.Status.STRONG_ERROR) {
                    argumentTypeResolver.checkTypesWithNoCallee(context.toBasic());
                    return;
                }
                candidateCall.setUnmappedArguments(unmappedArguments);
            }
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
        D descriptor = resolvedCall.getCandidateDescriptor();
        ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
        assert constraintSystem != null;

        constraintSystem.addSupertypeConstraint(context.expectedType, descriptor.getReturnType(), ConstraintPosition.EXPECTED_TYPE_POSITION);

        ConstraintSystemCompleter constraintSystemCompleter = context.trace.get(
                BindingContext.CONSTRAINT_SYSTEM_COMPLETER, context.call.getCalleeExpression());
        if (constraintSystemCompleter != null) {
            ConstraintSystemImpl backup = (ConstraintSystemImpl) constraintSystem.copy();

            //todo improve error reporting with errors in constraints from completer
            constraintSystemCompleter.completeConstraintSystem(constraintSystem, resolvedCall);
            if (constraintSystem.hasTypeConstructorMismatchAt(ConstraintPosition.FROM_COMPLETER) ||
                (constraintSystem.hasContradiction() && !backup.hasContradiction())) {

                constraintSystem = backup;
                resolvedCall.setConstraintSystem(backup);
            }
        }

        if (constraintSystem.hasContradiction()) {
            return reportInferenceError(context);
        }
        if (!constraintSystem.isSuccessful() && context.expectedType == TypeUtils.UNIT_EXPECTED_TYPE) {
            ConstraintSystemImpl copy = (ConstraintSystemImpl) constraintSystem.copy();
            copy.addSupertypeConstraint(KotlinBuiltIns.getInstance().getUnitType(), descriptor.getReturnType(), ConstraintPosition.EXPECTED_TYPE_POSITION);
            if (copy.isSuccessful()) {
                constraintSystem = copy;
                resolvedCall.setConstraintSystem(constraintSystem);
            }
        }
        boolean boundsAreSatisfied = ConstraintsUtil.checkBoundsAreSatisfied(constraintSystem, /*substituteOtherTypeParametersInBounds=*/true);
        if (!boundsAreSatisfied || constraintSystem.hasUnknownParameters()) {
            ConstraintSystemImpl copy = (ConstraintSystemImpl) constraintSystem.copy();
            copy.processDeclaredBoundConstraints();
            boundsAreSatisfied = copy.isSuccessful() && ConstraintsUtil.checkBoundsAreSatisfied(copy, /*substituteOtherTypeParametersInBounds=*/true);
            if (boundsAreSatisfied) {
                constraintSystem = copy;
                resolvedCall.setConstraintSystem(constraintSystem);
            }
        }
        if (!constraintSystem.isSuccessful()) {
            return reportInferenceError(context);
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
        JetType returnType = resolvedCall.getResultingDescriptor().getReturnType();
        if (isInnerCall) {
            PsiElement callElement = context.call.getCallElement();
            if (callElement instanceof JetCallExpression) {
                DataFlowUtils.checkType(returnType, (JetCallExpression) callElement, context, context.dataFlowInfo);
            }
        }
        return returnType;
    }

    private <D extends CallableDescriptor> JetType reportInferenceError(
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        ResolvedCallImpl<D> resolvedCall = context.candidateCall;
        ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();

        resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor());
        completeNestedCallsInference(context);
        List<JetType> argumentTypes = checkValueArgumentTypes(
                context, resolvedCall, context.trace, RESOLVE_FUNCTION_ARGUMENTS).argumentTypes;
        JetType receiverType = resolvedCall.getReceiverArgument().exists() ? resolvedCall.getReceiverArgument().getType() : null;
        InferenceErrorData.ExtendedInferenceErrorData errorData = InferenceErrorData
                .create(resolvedCall.getCandidateDescriptor(), constraintSystem, argumentTypes, receiverType, context.expectedType);

        context.tracing.typeInferenceFailed(context.trace, errorData);
        resolvedCall.addStatus(ResolutionStatus.OTHER_ERROR);
        if (!CallResolverUtil.hasInferredReturnType(resolvedCall)) return null;
        return resolvedCall.getResultingDescriptor().getReturnType();
    }

    @Nullable
    public <D extends CallableDescriptor> JetType completeNestedCallsInference(
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        ResolvedCallImpl<D> resolvedCall = context.candidateCall;
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet()) {
            ValueParameterDescriptor parameterDescriptor = entry.getKey();
            ResolvedValueArgument resolvedArgument = entry.getValue();

            for (ValueArgument argument : resolvedArgument.getArguments()) {
                completeInferenceForArgument(argument, parameterDescriptor, context);
            }
        }
        recordReferenceForInvokeFunction(context);
        return resolvedCall.getResultingDescriptor().getReturnType();
    }

    private <D extends CallableDescriptor> void completeInferenceForArgument(
            @NotNull ValueArgument argument,
            @NotNull ValueParameterDescriptor parameterDescriptor,
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        ConstraintSystem constraintSystem = context.candidateCall.getConstraintSystem();

        JetExpression expression = argument.getArgumentExpression();
        if (expression == null) return;

        JetType effectiveExpectedType = getEffectiveExpectedType(parameterDescriptor, argument);
        JetType expectedType = constraintSystem != null
                               ? constraintSystem.getCurrentSubstitutor().substitute(effectiveExpectedType, Variance.INVARIANT)
                               : effectiveExpectedType;
        context = context.replaceExpectedType(expectedType);

        JetExpression keyExpression = getDeferredComputationKeyExpression(expression);
        CallCandidateResolutionContext<FunctionDescriptor> storedContextForArgument =
                context.resolutionResultsCache.getDeferredComputation(keyExpression);

        if (storedContextForArgument == null) {
            PsiElement parent = expression.getParent();
            if (parent instanceof JetWhenExpression && expression == ((JetWhenExpression) parent).getSubjectExpression()
                || (expression instanceof JetFunctionLiteralExpression)) {
                return;
            }
            JetType type = argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(context, expression);
            checkResultArgumentType(type, argument, context);
            return;
        }

        CallCandidateResolutionContext<FunctionDescriptor> contextForArgument = storedContextForArgument
                .replaceContextDependency(ContextDependency.INDEPENDENT).replaceBindingTrace(context.trace).replaceExpectedType(
                        expectedType);
        JetType type;
        if (contextForArgument.candidateCall.hasIncompleteTypeParameters()) {
            type = completeTypeInferenceDependentOnExpectedTypeForCall(contextForArgument, true);
        }
        else {
            type = completeNestedCallsInference(contextForArgument);
            checkValueArgumentTypes(contextForArgument);
        }
        JetType result = BindingContextUtils.updateRecordedType(
                type, expression, context.trace, isFairSafeCallExpression(expression, context.trace));
        DataFlowUtils.checkType(result, expression, contextForArgument);
    }

    @Nullable
    private JetExpression getDeferredComputationKeyExpression(@NotNull JetExpression expression) {
        JetVisitor<JetExpression, Void> selectorExpressionFinder = new JetVisitor<JetExpression, Void>() {
            @Override
            public JetExpression visitQualifiedExpression(JetQualifiedExpression expression, Void data) {
                JetExpression selector = expression.getSelectorExpression();
                return selector != null ? selector.accept(this, null) : null;
            }

            @Override
            public JetExpression visitExpression(JetExpression expression, Void data) {
                return expression;
            }

            @Override
            public JetExpression visitParenthesizedExpression(JetParenthesizedExpression expression, Void data) {
                return expression.getExpression();
            }

            @Override
            public JetExpression visitPrefixExpression(JetPrefixExpression expression, Void data) {
                JetExpression baseExpression = JetPsiUtil.getBaseExpressionIfLabeledExpression(expression);
                return baseExpression != null ? baseExpression : expression;
            }

            @Override
            public JetExpression visitBlockExpression(JetBlockExpression expression, Void data) {
                JetElement lastStatement = JetPsiUtil.getLastStatementInABlock(expression);
                if (lastStatement != null) {
                    return lastStatement.accept(this, data);
                }
                return expression;
            }

            @Override
            public JetExpression visitBinaryExpression(JetBinaryExpression expression, Void data) {
                return ExpressionTypingUtils.isBinaryExpressionDependentOnExpectedType(expression) ? expression : null;
            }
        };
        return expression.accept(selectorExpressionFinder, null);
    }

    private boolean isFairSafeCallExpression(@NotNull JetExpression expression, @NotNull BindingTrace trace) {
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

    private <D extends CallableDescriptor> void checkResultArgumentType(
            @Nullable JetType type,
            @NotNull ValueArgument argument,
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        JetExpression expression = argument.getArgumentExpression();
        if (expression == null) return;

        if (expression instanceof JetConstantExpression && !KotlinBuiltIns.getInstance().isUnit(context.expectedType)) {
            CompileTimeConstant<?> value =
                    new CompileTimeConstantResolver().getCompileTimeConstant((JetConstantExpression) expression, context.expectedType);
            if (value instanceof ErrorValue) {
                context.trace.report(ERROR_COMPILE_TIME_VALUE.on(expression, ((ErrorValue) value).getMessage()));
            }
            return;
        }
        DataFlowInfo dataFlowInfoForValueArgument = context.candidateCall.getDataFlowInfoForArguments().getInfo(argument);
        ResolutionContext<?> newContext = context.replaceExpectedType(context.expectedType).replaceDataFlowInfo(dataFlowInfoForValueArgument);
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
        JetExpression deparenthesizedExpression = JetPsiUtil.deparenthesizeWithNoTypeResolution(argumentExpression, false);
        if (!(deparenthesizedExpression instanceof JetFunctionLiteralExpression)) return;
        JetFunctionLiteralExpression functionLiteralExpression = (JetFunctionLiteralExpression) deparenthesizedExpression;

        JetType effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument);
        JetType expectedType = constraintSystem.getCurrentSubstitutor().substitute(effectiveExpectedType, Variance.INVARIANT);
        if (expectedType == null || !KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(expectedType)
                || CallResolverUtil.hasUnknownFunctionParameter(expectedType)) {
            return;
        }
        MutableDataFlowInfoForArguments dataFlowInfoForArguments = context.candidateCall.getDataFlowInfoForArguments();
        DataFlowInfo dataFlowInfoForArgument = dataFlowInfoForArguments.getInfo(valueArgument);

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
                    .replaceDataFlowInfo(dataFlowInfoForArgument).replaceResolutionResultsCache(temporaryToResolveFunctionLiteral.cache);
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
        CallCandidateResolutionContext<D> newContext = 
                context.replaceExpectedType(expectedTypeWithoutReturnType).replaceDataFlowInfo(dataFlowInfoForArgument);
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

        ConstraintSystem constraintSystemWithRightTypeParameters = constraintSystem.replaceTypeVariables(
                new Function<TypeParameterDescriptor, TypeParameterDescriptor>() {
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
            isErrorType[0] = type == null || ErrorUtils.isErrorType(type);
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

        DataFlowValue dataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(
                JetPsiUtil.unwrapFromBlock(argumentExpression), type, trace.getBindingContext());
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
        resultStatus = resultStatus.combine(checkReceiver(context, trace, false));

        return new ValueArgumentsCheckingResult(resultStatus, checkingResult.argumentTypes);
    }

    private static <D extends CallableDescriptor> ResolutionStatus checkReceiver(
            @NotNull CallCandidateResolutionContext<D> context,
            @NotNull BindingTrace trace,
            boolean checkOnlyReceiverTypeError
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

                if (type == null || (ErrorUtils.isErrorType(type) && type != PLACEHOLDER_FUNCTION_TYPE)) {
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
        ExpressionReceiver receiverToCast = new ExpressionReceiver(JetPsiUtil.unwrapFromBlock(expression), actualType);
        List<ReceiverValue> variants =
                AutoCastUtils.getAutoCastVariants(context.trace.getBindingContext(), context.dataFlowInfo, receiverToCast);
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
        AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(context.dataFlowInfo, bindingContext);
        if (!safeAccess && !receiverParameter.getType().isNullable() && !autoCastService.isNotNull(receiverArgument)) {

            context.tracing.unsafeCall(trace, receiverArgumentType, implicitInvokeCheck);
            return UNSAFE_CALL_ERROR;
        }
        DataFlowValue receiverValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(receiverArgument, bindingContext);
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
