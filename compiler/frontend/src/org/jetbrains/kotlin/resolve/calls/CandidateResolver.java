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
import com.google.common.collect.Sets;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CheckValueArgumentsMode;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.model.*;
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils;
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.kotlin.resolve.calls.tasks.TasksPackage;
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;
import org.jetbrains.kotlin.types.expressions.JetTypeInfo;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.diagnostics.Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT;
import static org.jetbrains.kotlin.diagnostics.Errors.SUPER_CANT_BE_EXTENSION_RECEIVER;
import static org.jetbrains.kotlin.resolve.calls.CallResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.CallResolverUtil.getEffectiveExpectedType;
import static org.jetbrains.kotlin.resolve.calls.CallTransformer.CallForImplicitInvoke;
import static org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.*;
import static org.jetbrains.kotlin.types.TypeUtils.noExpectedType;

public class CandidateResolver {
    @NotNull
    private ArgumentTypeResolver argumentTypeResolver;
    @NotNull
    private GenericCandidateResolver genericCandidateResolver;

    @Inject
    public void setArgumentTypeResolver(@NotNull ArgumentTypeResolver argumentTypeResolver) {
        this.argumentTypeResolver = argumentTypeResolver;
    }

    @Inject
    public void setGenericCandidateResolver(@NotNull GenericCandidateResolver genericCandidateResolver) {
        this.genericCandidateResolver = genericCandidateResolver;
    }

    public <D extends CallableDescriptor, F extends D> void performResolutionForCandidateCall(
            @NotNull CallCandidateResolutionContext<D> context,
            @NotNull ResolutionTask<D, F> task) {

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

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
            ValueArgumentsToParametersMapper.Status argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(
                    context.call, context.tracing, candidateCall, Sets.<ValueArgument>newLinkedHashSet()
            );
            if (!argumentMappingStatus.isSuccess()) {
                candidateCall.addStatus(OTHER_ERROR);
            }
        }

        checkExtensionReceiver(context);

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
        else if (candidateCall.getKnownTypeParametersSubstitutor() != null) {
            candidateCall.setResultingSubstitutor(candidateCall.getKnownTypeParametersSubstitutor());
        }

        if (jetTypeArguments.isEmpty() &&
            !candidate.getTypeParameters().isEmpty() &&
            candidateCall.getKnownTypeParametersSubstitutor() == null) {
            candidateCall.addStatus(genericCandidateResolver.inferTypeArguments(context));
        }
        else {
            candidateCall.addStatus(checkAllValueArguments(context, SHAPE_FUNCTION_ARGUMENTS).status);
        }

        checkAbstractAndSuper(context);

        checkNonExtensionCalledWithReceiver(context);
    }

    private static <D extends CallableDescriptor> void checkExtensionReceiver(@NotNull CallCandidateResolutionContext<D> context) {
        MutableResolvedCall<D> candidateCall = context.candidateCall;
        ReceiverParameterDescriptor receiverParameter = candidateCall.getCandidateDescriptor().getExtensionReceiverParameter();
        ReceiverValue receiverArgument = candidateCall.getExtensionReceiver();
        if (receiverParameter != null &&!receiverArgument.exists()) {
            context.tracing.missingReceiver(candidateCall.getTrace(), receiverParameter);
            candidateCall.addStatus(OTHER_ERROR);
        }
        if (receiverParameter == null && receiverArgument.exists()) {
            context.tracing.noReceiverAllowed(candidateCall.getTrace());
            if (context.call.getCalleeExpression() instanceof JetSimpleNameExpression) {
                candidateCall.addStatus(RECEIVER_PRESENCE_ERROR);
            }
            else {
                candidateCall.addStatus(OTHER_ERROR);
            }
        }
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

    private static void checkNonExtensionCalledWithReceiver(@NotNull CallCandidateResolutionContext<?> context) {
        MutableResolvedCall<?> candidateCall = context.candidateCall;

        if (TasksPackage.isSynthesizedInvoke(candidateCall.getCandidateDescriptor()) &&
            !KotlinBuiltIns.isExtensionFunctionType(candidateCall.getDispatchReceiver().getType())) {
            context.tracing.freeFunctionCalledAsExtension(context.trace);
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

                CallResolutionContext<?> newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument))
                        .replaceBindingTrace(trace).replaceExpectedType(expectedType);
                JetTypeInfo typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(
                        expression, newContext, resolveFunctionArgumentBodies);
                JetType type = typeInfoForCall.getType();
                infoForArguments.updateInfo(argument, typeInfoForCall.getDataFlowInfo());

                ArgumentMatchStatus matchStatus = ArgumentMatchStatus.SUCCESS;
                JetType resultingType = type;
                if (type == null || (type.isError() && !ErrorUtils.isFunctionPlaceholder(type))) {
                    matchStatus = ArgumentMatchStatus.ARGUMENT_HAS_NO_TYPE;
                }
                else if (!noExpectedType(expectedType)) {
                    if (!ArgumentTypeResolver.isSubtypeOfForArgumentType(type, expectedType)) {
                        JetType smartCast = smartCastValueArgumentTypeIfPossible(expression, newContext.expectedType, type, newContext);
                        if (smartCast == null) {
                            resultStatus = OTHER_ERROR;
                            matchStatus = ArgumentMatchStatus.TYPE_MISMATCH;
                        }
                        else {
                            resultingType = smartCast;
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
        Collection<JetType> variants = SmartCastUtils.getSmartCastVariantsExcludingReceiver(context, receiverToCast);
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

        boolean safeAccess = isExplicitReceiver && !implicitInvokeCheck && CallUtilPackage.isExplicitSafeCall(candidateCall.getCall());
        boolean isSubtypeBySmartCast = SmartCastUtils.isSubTypeBySmartCastIgnoringNullability(
                receiverArgument, receiverParameter.getType(), context);
        if (!isSubtypeBySmartCast) {
            context.tracing.wrongReceiverType(trace, receiverParameter, receiverArgument);
            return OTHER_ERROR;
        }
        if (!SmartCastUtils.recordSmartCastIfNecessary(receiverArgument, receiverParameter.getType(), context, safeAccess)) {
            return OTHER_ERROR;
        }

        JetType receiverArgumentType = receiverArgument.getType();

        BindingContext bindingContext = trace.getBindingContext();
        if (!safeAccess && !receiverParameter.getType().isMarkedNullable() && receiverArgumentType.isMarkedNullable()) {
            if (!SmartCastUtils.canBeSmartCast(receiverParameter, receiverArgument, context)) {
                context.tracing.unsafeCall(trace, receiverArgumentType, implicitInvokeCheck);
                return UNSAFE_CALL_ERROR;
            }
        }
        DataFlowValue receiverValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, bindingContext, context.scope.getContainingDeclaration());
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
