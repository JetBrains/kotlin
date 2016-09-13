/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.psi.PsiElement;
import kotlin.Pair;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.CallResolverUtilKt;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.context.*;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.tasks.*;
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.calls.util.FunctionTypeResolveUtilsKt;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingVisitorDispatcher;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.kotlin.util.PerformanceCounter;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

@SuppressWarnings("RedundantTypeArguments")
public class CallResolver {
    private ExpressionTypingServices expressionTypingServices;
    private TypeResolver typeResolver;
    private ArgumentTypeResolver argumentTypeResolver;
    private GenericCandidateResolver genericCandidateResolver;
    private CallCompleter callCompleter;
    private NewResolutionOldInference newCallResolver;
    private final KotlinBuiltIns builtIns;
    private final LanguageVersionSettings languageVersionSettings;

    private static final PerformanceCounter callResolvePerfCounter = PerformanceCounter.Companion.create("Call resolve", ExpressionTypingVisitorDispatcher.typeInfoPerfCounter);

    public CallResolver(
            @NotNull KotlinBuiltIns builtIns,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        this.builtIns = builtIns;
        this.languageVersionSettings = languageVersionSettings;
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
    public void setGenericCandidateResolver(GenericCandidateResolver genericCandidateResolver) {
        this.genericCandidateResolver = genericCandidateResolver;
    }

    // component dependency cycle
    @Inject
    public void setCallCompleter(@NotNull CallCompleter callCompleter) {
        this.callCompleter = callCompleter;
    }

    // component dependency cycle
    @Inject
    public void setCallCompleter(@NotNull NewResolutionOldInference newCallResolver) {
        this.newCallResolver = newCallResolver;
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
    private OverloadResolutionResults<FunctionDescriptor> resolveCallForInvoke(
            @NotNull BasicCallResolutionContext context,
            @NotNull TracingStrategy tracing
    ) {
        return computeTasksAndResolveCall(
                context, OperatorNameConventions.INVOKE, tracing,
                NewResolutionOldInference.ResolutionKind.Invoke.INSTANCE);
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksAndResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull KtReferenceExpression referenceExpression,
            @NotNull NewResolutionOldInference.ResolutionKind<D> kind
    ) {
        TracingStrategy tracing = TracingStrategyImpl.create(referenceExpression, context.call);
        return computeTasksAndResolveCall(context, name, tracing, kind);
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksAndResolveCall(
            @NotNull final BasicCallResolutionContext context,
            @NotNull final Name name,
            @NotNull final TracingStrategy tracing,
            @NotNull final NewResolutionOldInference.ResolutionKind<D> kind
    ) {
        return callResolvePerfCounter.time(new Function0<OverloadResolutionResults<D>>() {
            @Override
            public OverloadResolutionResults<D> invoke() {
                ResolutionTask<D> resolutionTask = new ResolutionTask<D>(
                        kind, name, null
                );
                return doResolveCallOrGetCachedResults(context, resolutionTask, tracing);
            }
        });
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksFromCandidatesAndResolvedCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull KtReferenceExpression referenceExpression,
            @NotNull Collection<ResolutionCandidate<D>> candidates
    ) {
        return computeTasksFromCandidatesAndResolvedCall(context, candidates,
                                                         TracingStrategyImpl.create(referenceExpression, context.call));
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksFromCandidatesAndResolvedCall(
            @NotNull final BasicCallResolutionContext context,
            @NotNull final Collection<ResolutionCandidate<D>> candidates,
            @NotNull final TracingStrategy tracing
    ) {
        return callResolvePerfCounter.time(new Function0<OverloadResolutionResults<D>>() {
            @Override
            public OverloadResolutionResults<D> invoke() {
                ResolutionTask<D> resolutionTask = new ResolutionTask<D>(
                        new NewResolutionOldInference.ResolutionKind.GivenCandidates<D>(), null, candidates
                );
                return doResolveCallOrGetCachedResults(context, resolutionTask, tracing);
            }
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
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull Call call,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            boolean isAnnotationContext
    ) {
        return resolveFunctionCall(
                BasicCallResolutionContext.create(
                        trace, scope, call, expectedType, dataFlowInfo, ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                        isAnnotationContext
                )
        );
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(@NotNull BasicCallResolutionContext context) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        Call.CallType callType = context.call.getCallType();
        if (callType == Call.CallType.ARRAY_GET_METHOD || callType == Call.CallType.ARRAY_SET_METHOD) {
            Name name = Name.identifier(callType == Call.CallType.ARRAY_GET_METHOD ? "get" : "set");
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
            return (OverloadResolutionResults) resolveCallForConstructor(context, (KtConstructorCalleeExpression) calleeExpression);
        }
        else if (calleeExpression instanceof KtConstructorDelegationReferenceExpression) {
            KtConstructorDelegationCall delegationCall = (KtConstructorDelegationCall) context.call.getCallElement();
            DeclarationDescriptor container = context.scope.getOwnerDescriptor();
            assert container instanceof ConstructorDescriptor : "Trying to resolve JetConstructorDelegationCall not in constructor. scope.ownerDescriptor = " + container;
            return (OverloadResolutionResults) resolveConstructorDelegationCall(context, delegationCall, (KtConstructorDelegationReferenceExpression) calleeExpression,
                                                    (ClassConstructorDescriptor) container);
        }
        else if (calleeExpression == null) {
            return checkArgumentTypesAndFail(context);
        }

        // Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
        KotlinType expectedType = NO_EXPECTED_TYPE;
        if (calleeExpression instanceof KtLambdaExpression) {
            int parameterNumber = ((KtLambdaExpression) calleeExpression).getValueParameters().size();
            List<KotlinType> parameterTypes = new ArrayList<KotlinType>(parameterNumber);
            for (int i = 0; i < parameterNumber; i++) {
                parameterTypes.add(NO_EXPECTED_TYPE);
            }
            expectedType = FunctionTypeResolveUtilsKt.createFunctionType(
                    builtIns, Annotations.Companion.getEMPTY(), null, parameterTypes, null, context.expectedType
            );
        }
        KotlinType calleeType = expressionTypingServices.safeGetType(
                context.scope, calleeExpression, expectedType, context.dataFlowInfo, context.trace);
        ExpressionReceiver expressionReceiver = ExpressionReceiver.Companion.create(calleeExpression, calleeType, context.trace.getBindingContext());

        Call call = new CallTransformer.CallForImplicitInvoke(context.call.getExplicitReceiver(), expressionReceiver, context.call,
                                                              false);
        TracingStrategyForInvoke tracingForInvoke = new TracingStrategyForInvoke(calleeExpression, call, calleeType);
        return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke);
    }

    private OverloadResolutionResults<ClassConstructorDescriptor> resolveCallForConstructor(
            @NotNull BasicCallResolutionContext context,
            @NotNull KtConstructorCalleeExpression expression
    ) {
        assert context.call.getExplicitReceiver() == null :
                "Constructor can't be invoked with explicit receiver: " + context.call.getCallElement().getText();

        context.trace.record(BindingContext.LEXICAL_SCOPE, context.call.getCallElement(), context.scope);

        KtReferenceExpression functionReference = expression.getConstructorReferenceExpression();
        KtTypeReference typeReference = expression.getTypeReference();
        if (functionReference == null || typeReference == null) {
            return checkArgumentTypesAndFail(context); // No type there
        }
        KotlinType constructedType = typeResolver.resolveType(context.scope, typeReference, context.trace, true);
        if (constructedType.isError()) {
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

        Pair<Collection<ResolutionCandidate<ClassConstructorDescriptor>>, BasicCallResolutionContext> candidatesAndContext =
                prepareCandidatesAndContextForConstructorCall(constructedType, context);

        Collection<ResolutionCandidate<ClassConstructorDescriptor>> candidates = candidatesAndContext.getFirst();
        context = candidatesAndContext.getSecond();

        return computeTasksFromCandidatesAndResolvedCall(context, functionReference, candidates);
    }

    @Nullable
    public OverloadResolutionResults<ClassConstructorDescriptor> resolveConstructorDelegationCall(
            @NotNull BindingTrace trace, @NotNull LexicalScope scope, @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ClassConstructorDescriptor constructorDescriptor,
            @NotNull KtConstructorDelegationCall call
    ) {
        // Method returns `null` when there is nothing to resolve in trivial cases like `null` call expression or
        // when super call should be conventional enum constructor and super call should be empty

        BasicCallResolutionContext context = BasicCallResolutionContext.create(
                trace, scope,
                CallMaker.makeCall(null, null, call),
                NO_EXPECTED_TYPE,
                dataFlowInfo, ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                false);

        if (call.getCalleeExpression() == null) return checkArgumentTypesAndFail(context);

        if (constructorDescriptor.getConstructedClass().getKind() == ClassKind.ENUM_CLASS && call.isImplicit()) {
            return null;
        }

        return resolveConstructorDelegationCall(
                context,
                call,
                call.getCalleeExpression(),
                constructorDescriptor
        );
    }

    @NotNull
    private OverloadResolutionResults<ClassConstructorDescriptor> resolveConstructorDelegationCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull KtConstructorDelegationCall call,
            @NotNull KtConstructorDelegationReferenceExpression calleeExpression,
            @NotNull ClassConstructorDescriptor calleeConstructor
    ) {
        context.trace.record(BindingContext.LEXICAL_SCOPE, call, context.scope);

        ClassDescriptor currentClassDescriptor = calleeConstructor.getContainingDeclaration();

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
                context.trace.report(PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.on(
                        (KtConstructorDelegationCall) calleeExpression.getParent()
                ));
            }
            if (call.isImplicit()) return OverloadResolutionResultsImpl.nameNotFound();
        }

        if (constructors.isEmpty()) {
            context.trace.report(NO_CONSTRUCTOR.on(CallUtilKt.getValueArgumentListOrElement(context.call)));
            return checkArgumentTypesAndFail(context);
        }


        KotlinType superType = isThisCall ?
                                  calleeConstructor.getContainingDeclaration().getDefaultType() :
                                  DescriptorUtils.getSuperClassType(currentClassDescriptor);

        Pair<Collection<ResolutionCandidate<ClassConstructorDescriptor>>, BasicCallResolutionContext> candidatesAndContext =
                prepareCandidatesAndContextForConstructorCall(superType, context);
        Collection<ResolutionCandidate<ClassConstructorDescriptor>> candidates = candidatesAndContext.getFirst();
        context = candidatesAndContext.getSecond();

        TracingStrategy tracing = call.isImplicit() ?
                                  new TracingStrategyForImplicitConstructorDelegationCall(call, context.call) :
                                  TracingStrategyImpl.create(calleeExpression, context.call);

        PsiElement reportOn = call.isImplicit() ? call : calleeExpression;

        if (delegateClassDescriptor.isInner()
                && !DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, reportOn,
                                                                  (ClassDescriptor) delegateClassDescriptor.getContainingDeclaration())) {
            return checkArgumentTypesAndFail(context);
        }

        return computeTasksFromCandidatesAndResolvedCall(context, candidates, tracing);
    }

    @NotNull
    private static Pair<Collection<ResolutionCandidate<ClassConstructorDescriptor>>, BasicCallResolutionContext> prepareCandidatesAndContextForConstructorCall(
            @NotNull KotlinType superType,
            @NotNull BasicCallResolutionContext context
    ) {
        if (!(superType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)) {
            return new Pair<Collection<ResolutionCandidate<ClassConstructorDescriptor>>, BasicCallResolutionContext>(
                    Collections.<ResolutionCandidate<ClassConstructorDescriptor>>emptyList(), context);
        }

        ClassDescriptor superClass = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();

        // If any constructor has type parameter (currently it only can be true for ones from Java), try to infer arguments for them
        // Otherwise use NO_EXPECTED_TYPE and knownTypeParametersSubstitutor
        boolean anyConstructorHasDeclaredTypeParameters =
                anyConstructorHasDeclaredTypeParameters(superType.getConstructor().getDeclarationDescriptor());

        TypeSubstitutor knownTypeParametersSubstitutor = anyConstructorHasDeclaredTypeParameters ? null : TypeSubstitutor.create(superType);
        if (anyConstructorHasDeclaredTypeParameters) {
            context = context.replaceExpectedType(superType);
        }

        Collection<ResolutionCandidate<ClassConstructorDescriptor>> candidates =
                CallResolverUtilKt.createResolutionCandidatesForConstructors(context.scope, context.call, superClass, knownTypeParametersSubstitutor);

        return new Pair<Collection<ResolutionCandidate<ClassConstructorDescriptor>>, BasicCallResolutionContext>(candidates, context);
    }

    private static boolean anyConstructorHasDeclaredTypeParameters(@Nullable ClassifierDescriptor classDescriptor) {
        if (!(classDescriptor instanceof ClassDescriptor)) return false;
        for (ConstructorDescriptor constructor : ((ClassDescriptor) classDescriptor).getConstructors()) {
            if (constructor.getTypeParameters().size() > constructor.getContainingDeclaration().getDeclaredTypeParameters().size()) return true;
        }

        return false;
    }

    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithKnownCandidate(
            @NotNull final Call call,
            @NotNull final TracingStrategy tracing,
            @NotNull final ResolutionContext<?> context,
            @NotNull final ResolutionCandidate<FunctionDescriptor> candidate,
            @Nullable final MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        return callResolvePerfCounter.time(new Function0<OverloadResolutionResults<FunctionDescriptor>>() {
            @Override
            public OverloadResolutionResults<FunctionDescriptor> invoke() {
                BasicCallResolutionContext basicCallResolutionContext =
                        BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, dataFlowInfoForArguments);

                Set<ResolutionCandidate<FunctionDescriptor>> candidates = Collections.singleton(candidate);

                ResolutionTask<FunctionDescriptor> resolutionTask =
                        new ResolutionTask<FunctionDescriptor>(
                                new NewResolutionOldInference.ResolutionKind.GivenCandidates<FunctionDescriptor>(), null, candidates
                        );


                return doResolveCallOrGetCachedResults(basicCallResolutionContext, resolutionTask, tracing);
            }
        });
    }

    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> doResolveCallOrGetCachedResults(
            @NotNull BasicCallResolutionContext context,
            @NotNull ResolutionTask<D> resolutionTask,
            @NotNull TracingStrategy tracing
    ) {
        Call call = context.call;
        tracing.bindCall(context.trace, call);

        TemporaryBindingTrace traceToResolveCall = TemporaryBindingTrace.create(context.trace, "trace to resolve call", call);
        BasicCallResolutionContext newContext = context.replaceBindingTrace(traceToResolveCall);

        BindingContextUtilsKt.recordScope(newContext.trace, newContext.scope, newContext.call.getCalleeExpression());
        BindingContextUtilsKt.recordDataFlowInfo(newContext, newContext.call.getCalleeExpression());

        OverloadResolutionResultsImpl<D> results = doResolveCall(newContext, resolutionTask, tracing);
        DelegatingBindingTrace deltasTraceForTypeInference = ((OverloadResolutionResultsImpl) results).getTrace();
        if (deltasTraceForTypeInference != null) {
            deltasTraceForTypeInference.addOwnDataTo(traceToResolveCall);
        }
        completeTypeInferenceDependentOnFunctionLiterals(newContext, results, tracing);
        if (context.contextDependency == ContextDependency.DEPENDENT) {
            cacheResults(context, results, traceToResolveCall, tracing);
        }
        traceToResolveCall.commit();

        if (context.contextDependency == ContextDependency.INDEPENDENT) {
            results = callCompleter.completeCall(context, results, tracing);
        }

        return results;
    }

    private <D extends CallableDescriptor> void completeTypeInferenceDependentOnFunctionLiterals(
            @NotNull BasicCallResolutionContext context,
            @NotNull OverloadResolutionResultsImpl<D> results,
            @NotNull TracingStrategy tracing
    ) {
        if (CallResolverUtilKt.isInvokeCallOnVariable(context.call)) return;
        if (!results.isSingleResult()) {
            if (results.getResultCode() == INCOMPLETE_TYPE_INFERENCE) {
                argumentTypeResolver.checkTypesWithNoCallee(context, RESOLVE_FUNCTION_ARGUMENTS);
            }
            return;
        }

        CallCandidateResolutionContext<D> candidateContext = CallCandidateResolutionContext.createForCallBeingAnalyzed(
                results.getResultingCall(), context, tracing);
        genericCandidateResolver.completeTypeInferenceDependentOnFunctionArgumentsForCall(candidateContext);
    }

    private static <F extends CallableDescriptor> void cacheResults(
            @NotNull BasicCallResolutionContext context,
            @NotNull OverloadResolutionResultsImpl<F> results,
            @NotNull DelegatingBindingTrace traceToResolveCall,
            @NotNull TracingStrategy tracing
    ) {
        Call call = context.call;
        if (CallResolverUtilKt.isInvokeCallOnVariable(call)) return;

        DelegatingBindingTrace deltasTraceToCacheResolve = new DelegatingBindingTrace(
                BindingContext.EMPTY, "delta trace for caching resolve of", context.call, BindingTraceFilter.Companion.getACCEPT_ALL());
        traceToResolveCall.addOwnDataTo(deltasTraceToCacheResolve);

        context.resolutionResultsCache.record(call, results, context, tracing, deltasTraceToCacheResolve);
    }

    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> checkArgumentTypesAndFail(BasicCallResolutionContext context) {
        argumentTypeResolver.checkTypesWithNoCallee(context, ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> doResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull ResolutionTask<D> resolutionTask,
            @NotNull TracingStrategy tracing
    ) {
        if (context.checkArguments == CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) {
            argumentTypeResolver.analyzeArgumentsAndRecordTypes(context, ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS);
        }

        List<KtTypeProjection> typeArguments = context.call.getTypeArguments();
        for (KtTypeProjection projection : typeArguments) {
            if (projection.getProjectionKind() != KtProjectionKind.NONE) {
                context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection));
                ModifierCheckerCore.INSTANCE.check(projection, context.trace, null, languageVersionSettings);
            }
            KotlinType type = argumentTypeResolver.resolveTypeRefWithDefault(
                    projection.getTypeReference(), context.scope, context.trace,
                    null);
            if (type != null) {
                ForceResolveUtil.forceResolveAllContents(type);
            }
        }

        if (!(resolutionTask.resolutionKind instanceof NewResolutionOldInference.ResolutionKind.GivenCandidates)) {
            assert resolutionTask.name != null;
            return newCallResolver.runResolution(context, resolutionTask.name, resolutionTask.resolutionKind, tracing);
        }
        else {
            assert resolutionTask.givenCandidates != null;
            return newCallResolver.runResolutionForGivenCandidates(context, tracing, resolutionTask.givenCandidates);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static class ResolutionTask<D extends CallableDescriptor> {

        @Nullable
        final Name name;

        @Nullable
        final Collection<ResolutionCandidate<D>> givenCandidates;

        @NotNull
        final NewResolutionOldInference.ResolutionKind<D> resolutionKind;

        private ResolutionTask(
                @NotNull NewResolutionOldInference.ResolutionKind<D> kind,
                @Nullable Name name,
                @Nullable Collection<ResolutionCandidate<D>> candidates
        ) {
            this.name = name;
            givenCandidates = candidates;
            resolutionKind = kind;
        }
    }

}
