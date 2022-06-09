/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.FunctionTypesKt;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.model.DataFlowInfoForArgumentsImpl;
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallKind;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInferenceKt;
import org.jetbrains.kotlin.resolve.calls.util.CallResolverUtilKt;
import org.jetbrains.kotlin.resolve.calls.util.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession;
import org.jetbrains.kotlin.resolve.calls.context.*;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.calls.tasks.*;
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference;
import org.jetbrains.kotlin.resolve.calls.tower.PSICallResolver;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.KotlinTypeKt;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingVisitorDispatcher;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.kotlin.util.PerformanceCounter;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

@SuppressWarnings("RedundantTypeArguments")
public class CallResolver {
    private ExpressionTypingServices expressionTypingServices;
    private TypeResolver typeResolver;
    private ArgumentTypeResolver argumentTypeResolver;
    private SyntheticScopes syntheticScopes;
    private PSICallResolver psiCallResolver;
    private final DataFlowValueFactory dataFlowValueFactory;
    private final KotlinBuiltIns builtIns;
    private final LanguageVersionSettings languageVersionSettings;

    private static final PerformanceCounter callResolvePerfCounter = PerformanceCounter.Companion.create("Call resolve", ExpressionTypingVisitorDispatcher.typeInfoPerfCounter);

    public CallResolver(
            @NotNull KotlinBuiltIns builtIns,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory
    ) {
        this.builtIns = builtIns;
        this.languageVersionSettings = languageVersionSettings;
        this.dataFlowValueFactory = dataFlowValueFactory;
    }

    // component dependency cycle
    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    // component dependency cycle
    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    // component dependency cycle
    @Inject
    public void setArgumentTypeResolver(@NotNull ArgumentTypeResolver argumentTypeResolver) {
        this.argumentTypeResolver = argumentTypeResolver;
    }

    // component dependency cycle
    @Inject
    public void setPSICallResolver(@NotNull PSICallResolver PSICallResolver) {
        this.psiCallResolver = PSICallResolver;
    }

    @Inject
    public void setSyntheticScopes(@NotNull SyntheticScopes syntheticScopes) {
        this.syntheticScopes = syntheticScopes;
    }

    @NotNull
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull BasicCallResolutionContext context) {
        KtExpression calleeExpression = context.call.getCalleeExpression();
        assert calleeExpression instanceof KtSimpleNameExpression;
        KtSimpleNameExpression nameExpression = (KtSimpleNameExpression) calleeExpression;
        Name referencedName = nameExpression.getReferencedNameAsName();
        return computeTasksAndResolveCall(
                context, referencedName, nameExpression,
                NewResolutionOldInference.ResolutionKind.Variable.INSTANCE);
    }

    @NotNull
    public OverloadResolutionResults<CallableDescriptor> resolveCallForMember(
            @NotNull KtSimpleNameExpression nameExpression,
            @NotNull BasicCallResolutionContext context
    ) {
        return computeTasksAndResolveCall(
                context, nameExpression.getReferencedNameAsName(), nameExpression,
                NewResolutionOldInference.ResolutionKind.CallableReference.INSTANCE);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull ResolutionContext<?> context,
            @NotNull Call call,
            @NotNull KtReferenceExpression functionReference,
            @NotNull Name name
    ) {
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        return computeTasksAndResolveCall(
                callResolutionContext, name, functionReference,
                NewResolutionOldInference.ResolutionKind.Function.INSTANCE);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull ResolutionContext<?> context,
            @NotNull Call call,
            @NotNull Name name,
            @NotNull TracingStrategy tracing
    ) {
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        return computeTasksAndResolveCall(callResolutionContext, name, tracing, NewResolutionOldInference.ResolutionKind.Function.INSTANCE);
    }

    @NotNull
    private OverloadResolutionResults<FunctionDescriptor> resolveCallForInvoke(
            @NotNull BasicCallResolutionContext context,
            @NotNull TracingStrategy tracing
    ) {
        return computeTasksAndResolveCall(
                context, OperatorNameConventions.INVOKE, tracing,
                NewResolutionOldInference.ResolutionKind.Invoke.INSTANCE);
    }

    // this declaration is used by compiler plugins
    @SuppressWarnings("WeakerAccess")
    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksAndResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull KtReferenceExpression referenceExpression,
            @NotNull NewResolutionOldInference.ResolutionKind kind
    ) {
        TracingStrategy tracing = TracingStrategyImpl.create(referenceExpression, context.call);
        return computeTasksAndResolveCall(context, name, tracing, kind);
    }

    // this declaration is used by compiler plugins
    @SuppressWarnings("WeakerAccess")
    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksAndResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull TracingStrategy tracing,
            @NotNull NewResolutionOldInference.ResolutionKind kind
    ) {
        return callResolvePerfCounter.<OverloadResolutionResults<D>>time(() -> {
            ResolutionTask<D> resolutionTask = new ResolutionTask<>(kind, name);
            return doResolveCallOrGetCachedResults(context, resolutionTask, tracing);
        });
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveBinaryCall(
            ExpressionTypingContext context,
            ExpressionReceiver receiver,
            KtBinaryExpression binaryExpression,
            Name name
    ) {
        return resolveCallWithGivenName(
                context,
                CallMaker.makeCall(receiver, binaryExpression),
                binaryExpression.getOperationReference(),
                name
        );
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCollectionLiteralCallWithGivenDescriptor(
            @NotNull ExpressionTypingContext context,
            @NotNull KtCollectionLiteralExpression expression,
            @NotNull Call call,
            @NotNull Collection<FunctionDescriptor> functionDescriptors
    ) {
        TracingStrategy tracingStrategy = TracingStrategyImpl.create(expression, call);
        return resolveCallWithGivenDescriptors(
                context, call, functionDescriptors, tracingStrategy, null, null, null
        );
    }

    public OverloadResolutionResults<FunctionDescriptor> resolveSetterCall(
            @NotNull ExpressionTypingContext context,
            @NotNull ResolvedCall<?> propertyResolvedCall,
            @NotNull PropertySetterDescriptor descriptor
    ) {
        KtReferenceExpression propertyElement = (KtReferenceExpression)propertyResolvedCall.getCall().getCallElement();
        KtOperationExpression setterCall = PsiUtilsKt.getParentOfTypes(propertyElement, true, KtOperationExpression.class);

        assert setterCall != null;

        ReceiverParameterDescriptor receiverDescriptor = descriptor.getDispatchReceiverParameter();

        ReceiverValue dispatchReceiver = receiverDescriptor != null ? receiverDescriptor.getValue() : null;
        Call call = CallMaker.makeCallWithExpressions(propertyElement, null, null, propertyElement, Collections.emptyList(), Call.CallType.DEFAULT);
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(
                context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                new DataFlowInfoForArgumentsImpl(propertyResolvedCall.getDataFlowInfoForArguments().getResultInfo(), call)
        );

        return psiCallResolver.runResolutionAndInferenceForGivenDescriptors(
                callResolutionContext,
                Collections.singletonList(descriptor),
                TracingStrategy.EMPTY,
                KotlinCallKind.VARIABLE,
                null,
                dispatchReceiver != null ? NewResolutionOldInferenceKt.transformToReceiverWithSmartCastInfo(context, dispatchReceiver) : null
        );
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveEqualsCallWithGivenDescriptors(
            @NotNull ExpressionTypingContext context,
            @NotNull KtReferenceExpression expression,
            @NotNull ExpressionReceiver receiver,
            @NotNull Call call,
            @NotNull Collection<FunctionDescriptor> functionDescriptors
    ) {
        TracingStrategy tracingStrategy = TracingStrategyImpl.create(expression, call);
        ReceiverValueWithSmartCastInfo dispatchReceiverValue =
                NewResolutionOldInferenceKt.transformToReceiverWithSmartCastInfo(context, receiver);
        return resolveCallWithGivenDescriptors(
                context, call, functionDescriptors, tracingStrategy, null, null, dispatchReceiverValue
        );
    }

    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResults<D> resolveCallWithGivenDescriptors(
            @NotNull ExpressionTypingContext context,
            @NotNull Call call,
            @NotNull Collection<D> descriptors,
            @NotNull TracingStrategy tracingStrategy,
            @Nullable TypeSubstitutor substitutor,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments,
            @Nullable ReceiverValueWithSmartCastInfo dispatchReceiverValue
    ) {
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(
                context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, dataFlowInfoForArguments
        );

        OverloadResolutionResults<D> resolutionResults = psiCallResolver.runResolutionAndInferenceForGivenDescriptors(
                callResolutionContext,
                descriptors,
                tracingStrategy,
                KotlinCallKind.FUNCTION,
                substitutor,
                dispatchReceiverValue
        );

        if (resolutionResults.isSingleResult()) {
            context.trace.record(BindingContext.RESOLVED_CALL, call, resolutionResults.getResultingCall());
            context.trace.record(BindingContext.CALL, call.getCalleeExpression(), call);
        }

        return resolutionResults;
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull Call call,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            boolean isAnnotationContext,
            @Nullable InferenceSession inferenceSession
    ) {
        return resolveFunctionCall(
                BasicCallResolutionContext.create(
                        trace, scope, call, expectedType, dataFlowInfo, ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                        isAnnotationContext, languageVersionSettings, dataFlowValueFactory,
                        inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault()
                )
        );
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(@NotNull BasicCallResolutionContext context) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        Call.CallType callType = context.call.getCallType();
        if (callType == Call.CallType.ARRAY_GET_METHOD || callType == Call.CallType.ARRAY_SET_METHOD) {
            Name name = callType == Call.CallType.ARRAY_GET_METHOD ? OperatorNameConventions.GET : OperatorNameConventions.SET;
            KtArrayAccessExpression arrayAccessExpression = (KtArrayAccessExpression) context.call.getCallElement();
            return computeTasksAndResolveCall(
                    context, name, arrayAccessExpression,
                    NewResolutionOldInference.ResolutionKind.Function.INSTANCE);
        }

        KtExpression calleeExpression = context.call.getCalleeExpression();
        if (calleeExpression instanceof KtSimpleNameExpression) {
            KtSimpleNameExpression expression = (KtSimpleNameExpression) calleeExpression;
            return computeTasksAndResolveCall(
                    context, expression.getReferencedNameAsName(), expression,
                    NewResolutionOldInference.ResolutionKind.Function.INSTANCE);
        }
        else if (calleeExpression instanceof KtConstructorCalleeExpression) {
            return (OverloadResolutionResults) resolveConstructorCall(context, (KtConstructorCalleeExpression) calleeExpression);
        }
        else if (calleeExpression instanceof KtConstructorDelegationReferenceExpression) {
            KtConstructorDelegationCall delegationCall = (KtConstructorDelegationCall) context.call.getCallElement();
            DeclarationDescriptor container = context.scope.getOwnerDescriptor();
            assert container instanceof ConstructorDescriptor : "Trying to resolve JetConstructorDelegationCall not in constructor. scope.ownerDescriptor = " + container;
            return (OverloadResolutionResults) resolveConstructorDelegationCall(
                    context,
                    delegationCall,
                    (KtConstructorDelegationReferenceExpression) calleeExpression,
                    (ClassDescriptor) container.getContainingDeclaration()
            );
        }
        else if (calleeExpression == null) {
            return checkArgumentTypesAndFail(context);
        }

        // Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
        KotlinType expectedType = NO_EXPECTED_TYPE;
        if (calleeExpression instanceof KtLambdaExpression) {
            int parameterNumber = ((KtLambdaExpression) calleeExpression).getValueParameters().size();
            List<KotlinType> parameterTypes = new ArrayList<>(parameterNumber);
            for (int i = 0; i < parameterNumber; i++) {
                parameterTypes.add(NO_EXPECTED_TYPE);
            }
            expectedType = FunctionTypesKt.createFunctionType(
                    builtIns, Annotations.Companion.getEMPTY(), null, Collections.emptyList(), parameterTypes, null, context.expectedType
            );
        }
        KotlinType calleeType = expressionTypingServices.safeGetType(
                context.scope, calleeExpression, expectedType, context.dataFlowInfo, context.inferenceSession, context.trace);
        ExpressionReceiver expressionReceiver = ExpressionReceiver.Companion.create(calleeExpression, calleeType, context.trace.getBindingContext());

        Call call = new CallTransformer.CallForImplicitInvoke(context.call.getExplicitReceiver(), expressionReceiver, context.call,
                                                              false);
        TracingStrategyForInvoke tracingForInvoke = new TracingStrategyForInvoke(calleeExpression, call, calleeType);
        return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke);
    }

    private OverloadResolutionResults<ConstructorDescriptor> resolveConstructorCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull KtConstructorCalleeExpression expression
    ) {
        assert context.call.getExplicitReceiver() == null :
                "Constructor can't be invoked with explicit receiver: " + context.call.getCallElement().getText();

        context.trace.record(BindingContext.LEXICAL_SCOPE, context.call.getCallElement(), context.scope);

        KtReferenceExpression functionReference = expression.getConstructorReferenceExpression();
        KtTypeReference typeReference = expression.getTypeReference();
        if (functionReference == null || typeReference == null) {
            CallResolverUtilKt.checkForConstructorCallOnFunctionalType(typeReference, context);
            return checkArgumentTypesAndFail(context); // No type there
        }
        KotlinType constructedType = typeResolver.resolveType(context.scope, typeReference, context.trace, true);
        if (KotlinTypeKt.isError(constructedType)) {
            return checkArgumentTypesAndFail(context);
        }

        DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
        if (!(declarationDescriptor instanceof ClassDescriptor)) {
            context.trace.report(NOT_A_CLASS.on(expression));
            return checkArgumentTypesAndFail(context);
        }

        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;

        Collection<ClassConstructorDescriptor> constructors = classDescriptor.getConstructors();
        if (constructors.isEmpty()) {
            context.trace.report(NO_CONSTRUCTOR.on(CallUtilKt.getValueArgumentListOrElement(context.call)));
            return checkArgumentTypesAndFail(context);
        }

        return resolveConstructorCallAwareTypeParameters(context, constructedType, TracingStrategyImpl.create(functionReference, context.call));
    }

    @NotNull
    private OverloadResolutionResults<ConstructorDescriptor> resolveConstructorCallAwareTypeParameters(
            @NotNull BasicCallResolutionContext context,
            @NotNull KotlinType constructedType,
            @NotNull TracingStrategy tracingStrategy
    ) {
        // If any constructor has type parameter (currently it only can be true for ones from Java), try to infer arguments for them
        // Otherwise use NO_EXPECTED_TYPE and known type substitutor
        boolean anyConstructorHasDeclaredTypeParameters =
                anyConstructorHasDeclaredTypeParameters(constructedType.getConstructor().getDeclarationDescriptor());

        if (anyConstructorHasDeclaredTypeParameters) {
            context = context.replaceExpectedType(constructedType);
        }

        return CallResolverUtilKt.resolveConstructorCallWithGivenDescriptors(
                psiCallResolver, context, constructedType, !anyConstructorHasDeclaredTypeParameters, syntheticScopes, tracingStrategy
        );
    }

    @Nullable
    public OverloadResolutionResults<ConstructorDescriptor> resolveConstructorDelegationCall(
            @NotNull BindingTrace trace, @NotNull LexicalScope scope, @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ClassConstructorDescriptor constructorDescriptor,
            @NotNull KtConstructorDelegationCall call,
            @Nullable InferenceSession inferenceSession
    ) {
        // Method returns `null` when there is nothing to resolve in trivial cases like `null` call expression or
        // when super call should be conventional enum constructor and super call should be empty

        BasicCallResolutionContext context = BasicCallResolutionContext.create(
                trace, scope,
                CallMaker.makeCall(null, null, call),
                NO_EXPECTED_TYPE,
                dataFlowInfo, ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                false,
                languageVersionSettings,
                dataFlowValueFactory,
                inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault());

        KtConstructorDelegationReferenceExpression calleeExpression = call.getCalleeExpression();

        if (calleeExpression == null) return checkArgumentTypesAndFail(context);

        ClassDescriptor currentClassDescriptor = constructorDescriptor.getContainingDeclaration();

        if (constructorDescriptor.getConstructedClass().getKind() == ClassKind.ENUM_CLASS && call.isImplicit()) {
            if (currentClassDescriptor.getUnsubstitutedPrimaryConstructor() != null) {
                DiagnosticFactory0<PsiElement> warningOrError;

                if (languageVersionSettings.supportsFeature(LanguageFeature.RequiredPrimaryConstructorDelegationCallInEnums)) {
                    warningOrError = PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED; // error
                } else {
                    warningOrError = PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED_IN_ENUM; // warning
                }
                PsiElement reportOn = calcReportOn(calleeExpression);
                context.trace.report(warningOrError.on(reportOn));
            }
            return null;
        }

        return resolveConstructorDelegationCall(context, call, call.getCalleeExpression(), currentClassDescriptor);
    }

    @NotNull
    private OverloadResolutionResults<ConstructorDescriptor> resolveConstructorDelegationCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull KtConstructorDelegationCall callElement,
            @NotNull KtConstructorDelegationReferenceExpression calleeExpression,
            @NotNull ClassDescriptor currentClassDescriptor
    ) {
        context.trace.record(BindingContext.LEXICAL_SCOPE, callElement, context.scope);

        boolean isThisCall = calleeExpression.isThis();
        if (currentClassDescriptor.getKind() == ClassKind.ENUM_CLASS && !isThisCall) {
            context.trace.report(DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR.on(calleeExpression));
            return checkArgumentTypesAndFail(context);
        }

        ClassDescriptor delegateClassDescriptor = isThisCall ? currentClassDescriptor :
                                                  DescriptorUtilsKt.getSuperClassOrAny(currentClassDescriptor);
        Collection<ClassConstructorDescriptor> constructors = delegateClassDescriptor.getConstructors();

        if (!isThisCall && currentClassDescriptor.getUnsubstitutedPrimaryConstructor() != null) {
            if (DescriptorUtils.canHaveDeclaredConstructors(currentClassDescriptor)) {
                // Diagnostic is meaningless when reporting on interfaces and object
                PsiElement reportOn = calcReportOn(calleeExpression);
                context.trace.report(PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.on(reportOn));
            }
            if (callElement.isImplicit()) return OverloadResolutionResultsImpl.nameNotFound();
        }

        if (constructors.isEmpty()) {
            context.trace.report(NO_CONSTRUCTOR.on(CallUtilKt.getValueArgumentListOrElement(context.call)));
            return checkArgumentTypesAndFail(context);
        }


        KotlinType superType =
                isThisCall ? currentClassDescriptor.getDefaultType() : DescriptorUtils.getSuperClassType(currentClassDescriptor);

        TracingStrategy tracingStrategy = callElement.isImplicit() ?
                                  new TracingStrategyForImplicitConstructorDelegationCall(callElement, context.call) :
                                  TracingStrategyImpl.create(calleeExpression, context.call);

        OverloadResolutionResults<ConstructorDescriptor> resolutionResults =
                resolveConstructorCallAwareTypeParameters(context, superType, tracingStrategy);

        PsiElement reportOn = callElement.isImplicit() ? callElement : calleeExpression;

        if (delegateClassDescriptor.isInner()
                && !DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, reportOn,
                                                                  (ClassDescriptor) delegateClassDescriptor.getContainingDeclaration())) {
            return checkArgumentTypesAndFail(context);
        }

        return resolutionResults;
    }

    @NotNull
    private static PsiElement calcReportOn(@NotNull KtConstructorDelegationReferenceExpression calleeExpression) {
        PsiElement delegationCall = calleeExpression.getParent();
        return CallResolverUtilKt.reportOnElement(delegationCall);
    }

    private static boolean anyConstructorHasDeclaredTypeParameters(@Nullable ClassifierDescriptor classDescriptor) {
        if (!(classDescriptor instanceof ClassDescriptor)) return false;
        for (ConstructorDescriptor constructor : ((ClassDescriptor) classDescriptor).getConstructors()) {
            if (constructor.getTypeParameters().size() > constructor.getContainingDeclaration().getDeclaredTypeParameters().size()) return true;
        }

        return false;
    }

    private <D extends CallableDescriptor> OverloadResolutionResults<D> doResolveCallOrGetCachedResults(
            @NotNull BasicCallResolutionContext context,
            @NotNull ResolutionTask<D> resolutionTask,
            @NotNull TracingStrategy tracing
    ) {
        Call call = context.call;
        tracing.bindCall(context.trace, call);

        NewResolutionOldInference.ResolutionKind resolutionKind = resolutionTask.resolutionKind;

        assert resolutionTask.name != null;
        BindingContextUtilsKt.recordScope(context.trace, context.scope, context.call.getCalleeExpression());
        return psiCallResolver.runResolutionAndInference(context, resolutionTask.name, resolutionKind, tracing);
    }

    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> checkArgumentTypesAndFail(BasicCallResolutionContext context) {
        argumentTypeResolver.checkTypesWithNoCallee(context);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static class ResolutionTask<D extends CallableDescriptor> {

        @Nullable
        final Name name;
        @NotNull
        final NewResolutionOldInference.ResolutionKind resolutionKind;

        private ResolutionTask(
                @NotNull NewResolutionOldInference.ResolutionKind kind,
                @Nullable Name name
        ) {
            this.name = name;
            resolutionKind = kind;
        }
    }

}
