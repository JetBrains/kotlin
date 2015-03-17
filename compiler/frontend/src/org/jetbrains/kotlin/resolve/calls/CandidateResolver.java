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

package org.jetbrains.kotlin.resolve.calls;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.context.*;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl;
import org.jetbrains.kotlin.resolve.calls.model.*;
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils;
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT;
import static org.jetbrains.kotlin.diagnostics.Errors.SUPER_CANT_BE_EXTENSION_RECEIVER;
import static org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver.getLastElementDeparenthesized;
import static org.jetbrains.kotlin.resolve.calls.CallResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.CallResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.CallTransformer.CallForImplicitInvoke;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.RECEIVER_POSITION;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.VALUE_PARAMETER_POSITION;
import static org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.*;
import static org.jetbrains.kotlin.types.TypeUtils.*;

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


        ReceiverValue receiverValue = ExpressionTypingUtils.normalizeReceiverValueForVisibility(candidateCall.getDispatchReceiver(), context.trace.getBindingContext());
        DeclarationDescriptorWithVisibility invisibleMember =
                Visibilities.findInvisibleMember(receiverValue, candidate, context.scope.getContainingDeclaration());
        if (invisibleMember != null) {
            candidateCall.addStatus(OTHER_ERROR);
            context.tracing.invisibleMember(context.trace, invisibleMember);
        }

        if (task.checkArguments == CheckValueArgumentsMode.ENABLED) {
            Set<ValueArgument> unmappedArguments = Sets.newLinkedHashSet();
            ValueArgumentsToParametersMapper.Status argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(
                    context.call, context.tracing, candidateCall, unmappedArguments);
            if (!argumentMappingStatus.isSuccess()) {
                if (argumentMappingStatus == ValueArgumentsToParametersMapper.Status.STRONG_ERROR) {
                    candidateCall.addStatus(RECEIVER_PRESENCE_ERROR);
                }
                else {
                    candidateCall.addStatus(OTHER_ERROR);
                }
            }
        }
        if (!checkDispatchReceiver(context)) {
            candidateCall.addStatus(OTHER_ERROR);
        }

        List<JetTypeProjection> jetTypeArguments = context.call.getTypeArguments();
        if (!jetTypeArguments.isEmpty()) {
            // Explicit type arguments passed

            List<JetType> typeArguments = new ArrayList<JetType>();
            for (JetTypeProjection projection : jetTypeArguments) {
                if (projection.getProjectionKind() != JetProjectionKind.NONE) {
                    context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection));
                    ModifiersChecker.checkIncompatibleVarianceModifiers(projection.getModifierList(), context.trace);
                }
                JetType type = argumentTypeResolver.resolveTypeRefWithDefault(
                        projection.getTypeReference(), context.scope, context.trace,
                        ErrorUtils.createErrorType("Star projection in a call")
                );
                ForceResolveUtil.forceResolveAllContents(type);
                typeArguments.add(type);
            }
            int expectedTypeArgumentCount = candidate.getTypeParameters().size();
            for (int index = jetTypeArguments.size(); index < expectedTypeArgumentCount; index++) {
                typeArguments.add(ErrorUtils.createErrorType(
                        "Explicit type argument expected for " + candidate.getTypeParameters().get(index).getName()));
            }
            Map<TypeConstructor, TypeProjection> substitutionContext =
                    FunctionDescriptorUtil.createSubstitutionContext((FunctionDescriptor) candidate, typeArguments);
            TypeSubstitutor substitutor = TypeSubstitutor.create(substitutionContext);

            if (expectedTypeArgumentCount != jetTypeArguments.size()) {
                candidateCall.addStatus(OTHER_ERROR);
                context.tracing.wrongNumberOfTypeArguments(context.trace, expectedTypeArgumentCount);
            }
            else {
                checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate, substitutor, context.trace);
            }

            candidateCall.setResultingSubstitutor(substitutor);
        }

        if (jetTypeArguments.isEmpty() && !candidate.getTypeParameters().isEmpty()) {
            candidateCall.addStatus(inferTypeArguments(context));
        }
        else {
            candidateCall.addStatus(checkAllValueArguments(context, SHAPE_FUNCTION_ARGUMENTS).status);
        }

        checkAbstractAndSuper(context);
    }

    private static boolean checkDispatchReceiver(@NotNull CallCandidateResolutionContext<?> context) {
        MutableResolvedCall<? extends CallableDescriptor> candidateCall = context.candidateCall;
        CallableDescriptor candidateDescriptor = candidateCall.getCandidateDescriptor();
        ReceiverValue dispatchReceiver = candidateCall.getDispatchReceiver();
        if (dispatchReceiver.exists()) {
            ClassDescriptor nestedClass = null;
            if (candidateDescriptor instanceof ConstructorDescriptor
                && DescriptorUtils.isStaticNestedClass(candidateDescriptor.getContainingDeclaration())) {
                nestedClass = (ClassDescriptor) candidateDescriptor.getContainingDeclaration();
            }
            else if (candidateDescriptor instanceof FakeCallableDescriptorForObject) {
                nestedClass = ((FakeCallableDescriptorForObject) candidateDescriptor).getReferencedDescriptor();
            }
            if (nestedClass != null) {
                context.tracing.nestedClassAccessViaInstanceReference(context.trace, nestedClass, candidateCall.getExplicitReceiverKind());
                return false;
            }
        }

        assert (dispatchReceiver.exists() == (candidateCall.getResultingDescriptor().getDispatchReceiverParameter() != null))
                : "Shouldn't happen because of TaskPrioritizer: " + candidateDescriptor;

        return true;
    }

    private static boolean checkOuterClassMemberIsAccessible(@NotNull CallCandidateResolutionContext<?> context) {
        // In "this@Outer.foo()" the error will be reported on "this@Outer" instead
        if (context.call.getExplicitReceiver().exists() || context.call.getDispatchReceiver().exists()) return true;

        ClassDescriptor candidateThis = getDeclaringClass(context.candidateCall.getCandidateDescriptor());
        if (candidateThis == null || candidateThis.getKind().isSingleton()) return true;

        return DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, context.call.getCallElement(), candidateThis);
    }

    private static <D extends CallableDescriptor> void checkAbstractAndSuper(@NotNull CallCandidateResolutionContext<D> context) {
        MutableResolvedCall<D> candidateCall = context.candidateCall;
        CallableDescriptor descriptor = candidateCall.getCandidateDescriptor();
        JetExpression expression = context.candidateCall.getCall().getCalleeExpression();

        if (expression instanceof JetSimpleNameExpression) {
            // 'B' in 'class A: B()' is JetConstructorCalleeExpression
            if (descriptor instanceof ConstructorDescriptor) {
                Modality modality = ((ConstructorDescriptor) descriptor).getContainingDeclaration().getModality();
                if (modality == Modality.ABSTRACT) {
                    context.tracing.instantiationOfAbstractClass(context.trace);
                }
            }
        }

        JetSuperExpression superDispatchReceiver = getReceiverSuper(candidateCall.getDispatchReceiver());
        if (superDispatchReceiver != null) {
            if (descriptor instanceof MemberDescriptor && ((MemberDescriptor) descriptor).getModality() == Modality.ABSTRACT) {
                context.tracing.abstractSuperCall(context.trace);
                candidateCall.addStatus(OTHER_ERROR);
            }
        }

        // 'super' cannot be passed as an argument, for receiver arguments expression typer does not track this
        // See TaskPrioritizer for more
        JetSuperExpression superExtensionReceiver = getReceiverSuper(candidateCall.getExtensionReceiver());
        if (superExtensionReceiver != null) {
            context.trace.report(SUPER_CANT_BE_EXTENSION_RECEIVER.on(superExtensionReceiver, superExtensionReceiver.getText()));
            candidateCall.addStatus(OTHER_ERROR);
        }
    }

    @Nullable
    private static JetSuperExpression getReceiverSuper(@NotNull ReceiverValue receiver) {
        if (receiver instanceof ExpressionReceiver) {
            ExpressionReceiver expressionReceiver = (ExpressionReceiver) receiver;
            JetExpression expression = expressionReceiver.getExpression();
            if (expression instanceof JetSuperExpression) {
                return (JetSuperExpression) expression;
            }
        }
        return null;
    }

    @Nullable
    private static ClassDescriptor getDeclaringClass(@NotNull CallableDescriptor candidate) {
        ReceiverParameterDescriptor expectedThis = candidate.getDispatchReceiverParameter();
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
        if (!ArgumentTypeResolver.isFunctionLiteralArgument(argumentExpression, context)) return;

        JetFunction functionLiteral = ArgumentTypeResolver.getFunctionLiteralArgument(argumentExpression, context);

        JetType effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument);
        JetType expectedType = constraintSystem.getCurrentSubstitutor().substitute(effectiveExpectedType, Variance.INVARIANT);
        if (expectedType == null || TypeUtils.isDontCarePlaceholder(expectedType)) {
            expectedType = argumentTypeResolver.getShapeTypeOfFunctionLiteral(functionLiteral, context.scope, context.trace, false);
        }
        if (expectedType == null || !KotlinBuiltIns.isFunctionOrExtensionFunctionType(expectedType)
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

            JetElement statementExpression = JetPsiUtil.getExpressionOrLastStatementInBlock(functionLiteral.getBodyExpression());
            if (statementExpression == null) return;
            boolean[] mismatch = new boolean[1];
            ObservableBindingTrace errorInterceptingTrace = ExpressionTypingUtils.makeTraceInterceptingTypeMismatch(
                    temporaryToResolveFunctionLiteral.trace, statementExpression, mismatch);
            CallCandidateResolutionContext<D> newContext = context
                    .replaceBindingTrace(errorInterceptingTrace).replaceExpectedType(expectedType)
                    .replaceDataFlowInfo(dataFlowInfoForArgument).replaceResolutionResultsCache(temporaryToResolveFunctionLiteral.cache)
                    .replaceContextDependency(INDEPENDENT);
            JetType type = argumentTypeResolver.getFunctionLiteralTypeInfo(
                    argumentExpression, functionLiteral, newContext, RESOLVE_FUNCTION_ARGUMENTS).getType();
            if (!mismatch[0]) {
                constraintSystem.addSubtypeConstraint(
                        type, effectiveExpectedType, VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex()));
                temporaryToResolveFunctionLiteral.commit();
                return;
            }
        }
        JetType expectedTypeWithoutReturnType = hasExpectedReturnType ? CallResolverUtil.replaceReturnTypeByUnknown(expectedType) : expectedType;
        CallCandidateResolutionContext<D> newContext = context
                .replaceExpectedType(expectedTypeWithoutReturnType).replaceDataFlowInfo(dataFlowInfoForArgument)
                .replaceContextDependency(INDEPENDENT);
        JetType type = argumentTypeResolver.getFunctionLiteralTypeInfo(argumentExpression, functionLiteral, newContext,
                                                                       RESOLVE_FUNCTION_ARGUMENTS).getType();
        constraintSystem.addSubtypeConstraint(
                type, effectiveExpectedType, VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex()));
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
        ReceiverValue receiverArgument = candidateCall.getExtensionReceiver();
        ReceiverParameterDescriptor receiverParameter = candidateWithFreshVariables.getExtensionReceiverParameter();
        if (receiverArgument.exists() && receiverParameter != null) {
            JetType receiverType =
                    context.candidateCall.isSafeCall()
                    ? TypeUtils.makeNotNullable(receiverArgument.getType())
                    : receiverArgument.getType();
            if (receiverArgument instanceof ExpressionReceiver) {
                receiverType = updateResultTypeForSmartCasts(
                        receiverType, ((ExpressionReceiver) receiverArgument).getExpression(), context);
            }
            constraintSystem.addSubtypeConstraint(receiverType, receiverParameter.getType(), RECEIVER_POSITION.position());
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

        JetType type = updateResultTypeForSmartCasts(
                typeInfoForCall.getType(), argumentExpression, context.replaceDataFlowInfo(dataFlowInfoForArgument));
        constraintSystem.addSubtypeConstraint(
                type, effectiveExpectedType, VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex()));
    }

    @Nullable
    private static JetType updateResultTypeForSmartCasts(
            @Nullable JetType type,
            @Nullable JetExpression argumentExpression,
            @NotNull ResolutionContext context
    ) {
        JetExpression deparenthesizedArgument = getLastElementDeparenthesized(argumentExpression, context);
        if (deparenthesizedArgument == null || type == null) return type;

        DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(deparenthesizedArgument, type, context.trace.getBindingContext());
        if (!dataFlowValue.isStableIdentifier()) return type;

        Set<JetType> possibleTypes = context.dataFlowInfo.getPossibleTypes(dataFlowValue);
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
                candidateCall.getResultingDescriptor().getExtensionReceiverParameter(),
                candidateCall.getExtensionReceiver(), candidateCall.getExplicitReceiverKind().isExtensionReceiver(), false));

        resultStatus = resultStatus.combine(checkReceiver(
                context, candidateCall, trace,
                candidateCall.getResultingDescriptor().getDispatchReceiverParameter(), candidateCall.getDispatchReceiver(),
                candidateCall.getExplicitReceiverKind().isDispatchReceiver(),
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
                        JetType smartCastType = smartCastValueArgumentTypeIfPossible(expression, expectedType, type, newContext);
                        if (smartCastType == null) {
                            resultStatus = OTHER_ERROR;
                            matchStatus = ArgumentMatchStatus.TYPE_MISMATCH;
                        }
                        else {
                            resultingType = smartCastType;
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
    private static JetType smartCastValueArgumentTypeIfPossible(
            @NotNull JetExpression expression,
            @NotNull JetType expectedType,
            @NotNull JetType actualType,
            @NotNull ResolutionContext<?> context
    ) {
        ExpressionReceiver receiverToCast = new ExpressionReceiver(JetPsiUtil.safeDeparenthesize(expression, false), actualType);
        Collection<JetType> variants = SmartCastUtils.getSmartCastVariantsExcludingReceiver(
                context.trace.getBindingContext(), context.dataFlowInfo, receiverToCast);
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

        ReceiverParameterDescriptor extensionReceiver = candidateDescriptor.getExtensionReceiverParameter();
        ReceiverParameterDescriptor dispatchReceiver = candidateDescriptor.getDispatchReceiverParameter();
        ResolutionStatus status = SUCCESS;
        // For the expressions like '42.(f)()' where f: String.() -> Unit we'd like to generate a type mismatch error on '1',
        // not to throw away the candidate, so the following check is skipped.
        if (!CallResolverUtil.isInvokeCallOnExpressionWithBothReceivers(context.call)) {
            status = status.combine(checkReceiverTypeError(context, extensionReceiver, candidateCall.getExtensionReceiver()));
        }
        status = status.combine(checkReceiverTypeError(context, dispatchReceiver, candidateCall.getDispatchReceiver()));
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

        boolean isSubtypeBySmartCast = SmartCastUtils.isSubTypeBySmartCastIgnoringNullability(receiverArgument, erasedReceiverType, context);
        if (!isSubtypeBySmartCast) {
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

        boolean safeAccess = isExplicitReceiver && !implicitInvokeCheck && PsiUtilPackage.isExplicitSafeCall(candidateCall.getCall());
        boolean isSubtypeBySmartCast = SmartCastUtils.isSubTypeBySmartCastIgnoringNullability(
                receiverArgument, receiverParameter.getType(), context);
        if (!isSubtypeBySmartCast) {
            context.tracing.wrongReceiverType(trace, receiverParameter, receiverArgument);
            return OTHER_ERROR;
        }
        SmartCastUtils.recordSmartCastIfNecessary(receiverArgument, receiverParameter.getType(), context, safeAccess);

        JetType receiverArgumentType = receiverArgument.getType();

        BindingContext bindingContext = trace.getBindingContext();
        if (!safeAccess && !receiverParameter.getType().isMarkedNullable() && receiverArgumentType.isMarkedNullable()) {
            if (!SmartCastUtils.canBeSmartCast(receiverParameter, receiverArgument, bindingContext, context.dataFlowInfo)) {
                context.tracing.unsafeCall(trace, receiverArgumentType, implicitInvokeCheck);
                return UNSAFE_CALL_ERROR;
            }
        }
        DataFlowValue receiverValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, bindingContext);
        if (safeAccess && !context.dataFlowInfo.getNullability(receiverValue).canBeNull()) {
            context.tracing.unnecessarySafeCall(trace, receiverArgumentType);
        }

        context.additionalTypeChecker.checkReceiver(receiverParameter, receiverArgument, safeAccess, context);

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
            @NotNull TypeSubstitutor substitutor,
            @NotNull BindingTrace trace
    ) {
        List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
        for (int i = 0; i < Math.min(typeParameters.size(), jetTypeArguments.size()); i++) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            JetType typeArgument = typeArguments.get(i);
            JetTypeReference typeReference = jetTypeArguments.get(i).getTypeReference();
            if (typeReference != null) {
                DescriptorResolver.checkBounds(typeReference, typeArgument, typeParameterDescriptor, substitutor, trace);
            }
        }
    }
}
