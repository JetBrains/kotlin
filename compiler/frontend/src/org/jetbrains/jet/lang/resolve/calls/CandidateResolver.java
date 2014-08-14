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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastUtils;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValue;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValueFactory;
import org.jetbrains.jet.lang.resolve.calls.context.*;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl;
import org.jetbrains.jet.lang.resolve.calls.model.*;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.jet.lang.resolve.calls.tasks.TaskPrioritizer;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        MutableResolvedCall<D> candidateCall = context.candidateCall;
        D candidate = candidateCall.getCandidateDescriptor();

        candidateCall.addStatus(checkReceiverTypeError(context));

        if (ErrorUtils.isError(candidate)) {
            candidateCall.addStatus(SUCCESS);
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
        }

        if (task.checkArguments == CheckValueArgumentsMode.ENABLED) {
            Set<ValueArgument> unmappedArguments = Sets.newLinkedHashSet();
            ValueArgumentsToParametersMapper.Status argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(
                    context.call, context.tracing, candidateCall, unmappedArguments);
            if (!argumentMappingStatus.isSuccess()) {
                //For the expressions like '42.(f)()' where f: () -> Unit we'd like to generate an error 'no receiver admitted',
                //not to throw away the candidate.
                if (argumentMappingStatus == ValueArgumentsToParametersMapper.Status.STRONG_ERROR
                            && !CallResolverUtil.isInvokeCallOnExpressionWithBothReceivers(context.call)) {
                    candidateCall.addStatus(RECEIVER_PRESENCE_ERROR);
                    return;
                }
                else {
                    candidateCall.addStatus(OTHER_ERROR);
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
        MutableResolvedCall<D> resolvedCall = context.candidateCall;
        ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
        if (constraintSystem == null) return;

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
        MutableResolvedCall<D> candidateCall = context.candidateCall;
        final D candidate = candidateCall.getCandidateDescriptor();

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
                addConstraintForValueArgument(valueArgument, valueParameterDescriptor, substituteDontCare, constraintSystem,
                                              context, SHAPE_FUNCTION_ARGUMENTS);
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
                new Function1<TypeParameterDescriptor, TypeParameterDescriptor>() {
                    @Override
                    public TypeParameterDescriptor invoke(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
                        return candidate.getTypeParameters().get(typeParameterDescriptor.getIndex());
                    }
                }
        );
        candidateCall.setConstraintSystem(constraintSystemWithRightTypeParameters);


        // Solution
        boolean hasContradiction = constraintSystem.getStatus().hasContradiction();
        if (!hasContradiction) {
            return INCOMPLETE_TYPE_INFERENCE;
        }
        return OTHER_ERROR;
    }

    private void addConstraintForValueArgument(
            @NotNull ValueArgument valueArgument,
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull TypeSubstitutor substitutor,
            @NotNull ConstraintSystem constraintSystem,
            @NotNull CallCandidateResolutionContext<?> context,
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

        return TypeUtils.intersect(JetTypeChecker.DEFAULT, possibleTypes);
    }

    @NotNull
    private <D extends CallableDescriptor> ValueArgumentsCheckingResult checkAllValueArguments(
            @NotNull CallCandidateResolutionContext<D> context,
            @NotNull CallResolverUtil.ResolveArgumentsMode resolveFunctionArgumentBodies) {
        return checkAllValueArguments(context, context.candidateCall.getTrace(), resolveFunctionArgumentBodies);
    }

    @NotNull
    public <D extends CallableDescriptor> ValueArgumentsCheckingResult checkAllValueArguments(
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

        resultStatus = resultStatus.combine(checkReceiverTypeError(context));

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

    @NotNull
    private <D extends CallableDescriptor, C extends CallResolutionContext<C>> ValueArgumentsCheckingResult checkValueArgumentTypes(
            @NotNull CallResolutionContext<C> context,
            @NotNull MutableResolvedCall<D> candidateCall,
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

                ArgumentMatchStatus matchStatus = ArgumentMatchStatus.SUCCESS;
                JetType resultingType = type;
                if (type == null || (type.isError() && type != PLACEHOLDER_FUNCTION_TYPE)) {
                    matchStatus = ArgumentMatchStatus.ARGUMENT_HAS_NO_TYPE;
                }
                else if (!noExpectedType(expectedType)) {
                    if (!ArgumentTypeResolver.isSubtypeOfForArgumentType(type, expectedType)) {
                        JetType autocastType = autocastValueArgumentTypeIfPossible(expression, expectedType, type, newContext);
                        if (autocastType == null) {
                            resultStatus = OTHER_ERROR;
                            matchStatus = ArgumentMatchStatus.TYPE_MISMATCH;
                        }
                        else {
                            resultingType = autocastType;
                        }
                    }
                    else if (ErrorUtils.containsUninferredParameter(expectedType)) {
                        matchStatus = ArgumentMatchStatus.MATCH_MODULO_UNINFERRED_TYPES;
                    }
                }
                argumentTypes.add(resultingType);
                candidateCall.recordArgumentMatchStatus(argument, matchStatus);
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
        List<JetType> variants =
                AutoCastUtils.getAutoCastVariantsExcludingReceiver(context.trace.getBindingContext(), context.dataFlowInfo, receiverToCast);
        for (JetType possibleType : variants) {
            if (JetTypeChecker.DEFAULT.isSubtypeOf(possibleType, expectedType)) {
                return possibleType;
            }
        }
        return null;
    }

    private static <D extends CallableDescriptor> ResolutionStatus checkReceiverTypeError(
            @NotNull CallCandidateResolutionContext<D> context
    ) {
        MutableResolvedCall<D> candidateCall = context.candidateCall;
        D candidateDescriptor = candidateCall.getCandidateDescriptor();

        ReceiverParameterDescriptor receiverDescriptor = candidateDescriptor.getReceiverParameter();
        ReceiverParameterDescriptor expectedThisObjectDescriptor = candidateDescriptor.getExpectedThisObject();
        ResolutionStatus status = SUCCESS;
        // For the expressions like '42.(f)()' where f: String.() -> Unit we'd like to generate a type mismatch error on '1',
        // not to throw away the candidate, so the following check is skipped.
        if (!CallResolverUtil.isInvokeCallOnExpressionWithBothReceivers(context.call)) {
            status = status.combine(checkReceiverTypeError(context, receiverDescriptor, candidateCall.getReceiverArgument()));
        }
        status = status.combine(checkReceiverTypeError(context, expectedThisObjectDescriptor, candidateCall.getThisObject()));
        return status;
    }

    private static <D extends CallableDescriptor> ResolutionStatus checkReceiverTypeError(
            @NotNull CallCandidateResolutionContext<D> context,
            @Nullable ReceiverParameterDescriptor receiverParameterDescriptor,
            @NotNull ReceiverValue receiverArgument
    ) {
        if (receiverParameterDescriptor == null || !receiverArgument.exists()) return SUCCESS;

        D candidateDescriptor = context.candidateCall.getCandidateDescriptor();

        JetType erasedReceiverType = CallResolverUtil.getErasedReceiverType(receiverParameterDescriptor, candidateDescriptor);

        boolean isSubtypeByAutoCast = AutoCastUtils.isSubTypeByAutoCastIgnoringNullability(receiverArgument, erasedReceiverType, context);
        if (!isSubtypeByAutoCast) {
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
        D candidateDescriptor = candidateCall.getCandidateDescriptor();
        if (TypeUtils.dependsOnTypeParameters(receiverParameter.getType(), candidateDescriptor.getTypeParameters())) return SUCCESS;

        boolean safeAccess = isExplicitReceiver && !implicitInvokeCheck && candidateCall.isSafeCall();
        boolean isSubtypeByAutoCast = AutoCastUtils.isSubTypeByAutoCastIgnoringNullability(
                receiverArgument, receiverParameter.getType(), context);
        if (!isSubtypeByAutoCast) {
            context.tracing.wrongReceiverType(trace, receiverParameter, receiverArgument);
            return OTHER_ERROR;
        }
        AutoCastUtils.recordAutoCastIfNecessary(receiverArgument, receiverParameter.getType(), context, safeAccess);

        JetType receiverArgumentType = receiverArgument.getType();

        BindingContext bindingContext = trace.getBindingContext();
        if (!safeAccess && !receiverParameter.getType().isNullable() && receiverArgumentType.isNullable()) {
            if (!AutoCastUtils.isNotNull(receiverArgument, bindingContext, context.dataFlowInfo)) {

                context.tracing.unsafeCall(trace, receiverArgumentType, implicitInvokeCheck);
                return UNSAFE_CALL_ERROR;
            }
        }
        DataFlowValue receiverValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, bindingContext);
        if (safeAccess && !context.dataFlowInfo.getNullability(receiverValue).canBeNull()) {
            context.tracing.unnecessarySafeCall(trace, receiverArgumentType);
        }
        return SUCCESS;
    }

    public static class ValueArgumentsCheckingResult {
        @NotNull
        public final List<JetType> argumentTypes;
        @NotNull
        public final ResolutionStatus status;

        private ValueArgumentsCheckingResult(@NotNull ResolutionStatus status, @NotNull List<JetType> argumentTypes) {
            this.status = status;
            this.argumentTypes = argumentTypes;
        }
    }

    @NotNull
    public static JetType getEffectiveExpectedType(ValueParameterDescriptor parameterDescriptor, ValueArgument argument) {
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
