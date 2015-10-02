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
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.CallResolverUtilPackage;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker;
import org.jetbrains.kotlin.resolve.calls.context.*;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.kotlin.resolve.calls.results.ResolutionResultsHandler;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.tasks.*;
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.CallableDescriptorCollectors;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingVisitorDispatcher;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;
import org.jetbrains.kotlin.util.PerformanceCounter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage.recordDataFlowInfo;
import static org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage.recordScope;
import static org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS;
import static org.jetbrains.kotlin.resolve.calls.context.CandidateResolveMode.EXIT_ON_FIRST_ERROR;
import static org.jetbrains.kotlin.resolve.calls.context.CandidateResolveMode.FULLY;
import static org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER;
import static org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

@SuppressWarnings("RedundantTypeArguments")
public class CallResolver {
    private ExpressionTypingServices expressionTypingServices;
    private TypeResolver typeResolver;
    private CandidateResolver candidateResolver;
    private ArgumentTypeResolver argumentTypeResolver;
    private GenericCandidateResolver genericCandidateResolver;
    private CallCompleter callCompleter;
    private final TaskPrioritizer taskPrioritizer;
    private final ResolutionResultsHandler resolutionResultsHandler;
    @NotNull private KotlinBuiltIns builtIns;

    private static final PerformanceCounter callResolvePerfCounter = PerformanceCounter.Companion.create("Call resolve", ExpressionTypingVisitorDispatcher.typeInfoPerfCounter);
    private static final PerformanceCounter candidatePerfCounter = PerformanceCounter.Companion.create("Call resolve candidate analysis", true);

    public CallResolver(
            @NotNull TaskPrioritizer taskPrioritizer,
            @NotNull ResolutionResultsHandler resolutionResultsHandler,
            @NotNull KotlinBuiltIns builtIns
    ) {
        this.taskPrioritizer = taskPrioritizer;
        this.resolutionResultsHandler = resolutionResultsHandler;
        this.builtIns = builtIns;
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
    public void setCandidateResolver(@NotNull CandidateResolver candidateResolver) {
        this.candidateResolver = candidateResolver;
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

    @NotNull
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull BasicCallResolutionContext context) {
        JetExpression calleeExpression = context.call.getCalleeExpression();
        assert calleeExpression instanceof JetSimpleNameExpression;
        JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) calleeExpression;
        Name referencedName = nameExpression.getReferencedNameAsName();
        CallableDescriptorCollectors<VariableDescriptor> callableDescriptorCollectors;
        if (nameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
            referencedName = Name.identifier(referencedName.asString().substring(1));
            callableDescriptorCollectors = CallableDescriptorCollectors.PROPERTIES;
        }
        else {
            callableDescriptorCollectors = CallableDescriptorCollectors.VARIABLES;
        }
        return computeTasksAndResolveCall(
                context, referencedName, nameExpression,
                callableDescriptorCollectors, CallTransformer.VARIABLE_CALL_TRANSFORMER);
    }

    @NotNull
    public OverloadResolutionResults<CallableDescriptor> resolveCallForMember(
            @NotNull JetSimpleNameExpression nameExpression,
            @NotNull BasicCallResolutionContext context
    ) {
        return computeTasksAndResolveCall(
                context, nameExpression.getReferencedNameAsName(), nameExpression,
                CallableDescriptorCollectors.FUNCTIONS_AND_VARIABLES, CallTransformer.MEMBER_CALL_TRANSFORMER);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull ExpressionTypingContext context,
            @NotNull Call call,
            @NotNull JetReferenceExpression functionReference,
            @NotNull Name name
    ) {
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        return computeTasksAndResolveCall(
                callResolutionContext, name, functionReference,
                CallableDescriptorCollectors.FUNCTIONS_AND_VARIABLES, CallTransformer.FUNCTION_CALL_TRANSFORMER);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallForInvoke(
            @NotNull BasicCallResolutionContext context,
            @NotNull TracingStrategy tracing
    ) {
        return computeTasksAndResolveCall(
                context, OperatorConventions.INVOKE, tracing,
                CallableDescriptorCollectors.FUNCTIONS, CallTransformer.FUNCTION_CALL_TRANSFORMER);
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResults<F> computeTasksAndResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull JetReferenceExpression referenceExpression,
            @NotNull CallableDescriptorCollectors<D> collectors,
            @NotNull CallTransformer<D, F> callTransformer
    ) {
        TracingStrategy tracing = TracingStrategyImpl.create(referenceExpression, context.call);
        return computeTasksAndResolveCall(context, name, tracing, collectors, callTransformer);
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResults<F> computeTasksAndResolveCall(
            @NotNull final BasicCallResolutionContext context,
            @NotNull final Name name,
            @NotNull final TracingStrategy tracing,
            @NotNull final CallableDescriptorCollectors<D> collectors,
            @NotNull final CallTransformer<D, F> callTransformer
    ) {
        return callResolvePerfCounter.time(new Function0<OverloadResolutionResults<F>>() {
            @Override
            public OverloadResolutionResults<F> invoke() {
                List<ResolutionTask<D, F>> tasks = taskPrioritizer.<D, F>computePrioritizedTasks(context, name, tracing, collectors);
                return doResolveCallOrGetCachedResults(context, tasks, callTransformer, tracing);
            }
        });
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResults<F> computeTasksFromCandidatesAndResolvedCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull JetReferenceExpression referenceExpression,
            @NotNull Collection<ResolutionCandidate<D>> candidates,
            @NotNull CallTransformer<D, F> callTransformer
    ) {
        return computeTasksFromCandidatesAndResolvedCall(context, candidates, callTransformer,
                                                         TracingStrategyImpl.create(referenceExpression, context.call));
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResults<F> computeTasksFromCandidatesAndResolvedCall(
            @NotNull final BasicCallResolutionContext context,
            @NotNull final Collection<ResolutionCandidate<D>> candidates,
            @NotNull final CallTransformer<D, F> callTransformer,
            @NotNull final TracingStrategy tracing
    ) {
        return callResolvePerfCounter.time(new Function0<OverloadResolutionResults<F>>() {
            @Override
            public OverloadResolutionResults<F> invoke() {
                List<ResolutionTask<D, F>> prioritizedTasks =
                        taskPrioritizer.<D, F>computePrioritizedTasksFromCandidates(context, candidates, tracing);
                return doResolveCallOrGetCachedResults(context, prioritizedTasks, callTransformer, tracing);
            }
        });
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveBinaryCall(
            ExpressionTypingContext context,
            ExpressionReceiver receiver,
            JetBinaryExpression binaryExpression,
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
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            boolean isAnnotationContext
    ) {
        return resolveFunctionCall(
                BasicCallResolutionContext.create(
                        trace, scope, call, expectedType, dataFlowInfo, ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                        CallChecker.DoNothing.INSTANCE$, isAnnotationContext
                )
        );
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(@NotNull BasicCallResolutionContext context) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        JetExpression calleeExpression = context.call.getCalleeExpression();
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression expression = (JetSimpleNameExpression) calleeExpression;
            return computeTasksAndResolveCall(
                    context, expression.getReferencedNameAsName(), expression,
                    CallableDescriptorCollectors.FUNCTIONS_AND_VARIABLES, CallTransformer.FUNCTION_CALL_TRANSFORMER);
        }
        if (calleeExpression instanceof JetConstructorCalleeExpression) {
            return resolveCallForConstructor(context, (JetConstructorCalleeExpression) calleeExpression);
        }
        else if (calleeExpression == null) {
            return checkArgumentTypesAndFail(context);
        }

        // Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
        JetType expectedType = NO_EXPECTED_TYPE;
        if (calleeExpression instanceof JetFunctionLiteralExpression) {
            int parameterNumber = ((JetFunctionLiteralExpression) calleeExpression).getValueParameters().size();
            List<JetType> parameterTypes = new ArrayList<JetType>(parameterNumber);
            for (int i = 0; i < parameterNumber; i++) {
                parameterTypes.add(NO_EXPECTED_TYPE);
            }
            expectedType = builtIns.getFunctionType(Annotations.EMPTY, null, parameterTypes, context.expectedType);
        }
        JetType calleeType = expressionTypingServices.safeGetType(
                context.scope, calleeExpression, expectedType, context.dataFlowInfo, context.trace);
        ExpressionReceiver expressionReceiver = new ExpressionReceiver(calleeExpression, calleeType);

        Call call = new CallTransformer.CallForImplicitInvoke(context.call.getExplicitReceiver(), expressionReceiver, context.call);
        TracingStrategyForInvoke tracingForInvoke = new TracingStrategyForInvoke(calleeExpression, call, calleeType);
        return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke);
    }

    private OverloadResolutionResults<FunctionDescriptor> resolveCallForConstructor(
            @NotNull BasicCallResolutionContext context,
            @NotNull JetConstructorCalleeExpression expression
    ) {
        assert !context.call.getExplicitReceiver().exists() :
                "Constructor can't be invoked with explicit receiver: " + context.call.getCallElement().getText();

        JetReferenceExpression functionReference = expression.getConstructorReferenceExpression();
        JetTypeReference typeReference = expression.getTypeReference();
        if (functionReference == null || typeReference == null) {
            return checkArgumentTypesAndFail(context); // No type there
        }
        JetType constructedType = typeResolver.resolveType(context.scope, typeReference, context.trace, true);
        if (constructedType.isError()) {
            return checkArgumentTypesAndFail(context);
        }

        DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
        if (!(declarationDescriptor instanceof ClassDescriptor)) {
            context.trace.report(NOT_A_CLASS.on(expression));
            return checkArgumentTypesAndFail(context);
        }
        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
        Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
        if (constructors.isEmpty()) {
            context.trace.report(NO_CONSTRUCTOR.on(CallUtilPackage.getValueArgumentListOrElement(context.call)));
            return checkArgumentTypesAndFail(context);
        }
        Collection<ResolutionCandidate<CallableDescriptor>> candidates =
                taskPrioritizer.<CallableDescriptor>convertWithImpliedThisAndNoReceiver(context.scope, constructors, context.call);

        return computeTasksFromCandidatesAndResolvedCall(context, functionReference, candidates, CallTransformer.FUNCTION_CALL_TRANSFORMER);
    }

    @Nullable
    public OverloadResolutionResults<FunctionDescriptor> resolveConstructorDelegationCall(
            @NotNull BindingTrace trace, @NotNull LexicalScope scope, @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull JetConstructorDelegationCall call, @NotNull CallChecker callChecker
    ) {
        // Method returns `null` when there is nothing to resolve in trivial cases like `null` call expression or
        // when super call should be conventional enum constructor and super call should be empty

        BasicCallResolutionContext context = BasicCallResolutionContext.create(
                trace, scope,
                CallMaker.makeCall(ReceiverValue.NO_RECEIVER, null, call),
                NO_EXPECTED_TYPE,
                dataFlowInfo, ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                callChecker, false);

        if (call.getCalleeExpression() == null) return checkArgumentTypesAndFail(context);

        if (constructorDescriptor.getContainingDeclaration().getKind() == ClassKind.ENUM_CLASS && call.isImplicit()) {
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
    private OverloadResolutionResults<FunctionDescriptor> resolveConstructorDelegationCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull JetConstructorDelegationCall call,
            @NotNull JetConstructorDelegationReferenceExpression calleeExpression,
            @NotNull ConstructorDescriptor calleeConstructor
    ) {
        ClassDescriptor currentClassDescriptor = calleeConstructor.getContainingDeclaration();

        boolean isThisCall = calleeExpression.isThis();
        if (currentClassDescriptor.getKind() == ClassKind.ENUM_CLASS && !isThisCall) {
            context.trace.report(DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR.on(calleeExpression));
            return checkArgumentTypesAndFail(context);
        }

        ClassDescriptor delegateClassDescriptor = isThisCall ? currentClassDescriptor :
                                                   DescriptorUtilPackage.getSuperClassOrAny(currentClassDescriptor);
        Collection<ConstructorDescriptor> constructors = delegateClassDescriptor.getConstructors();

        if (!isThisCall && currentClassDescriptor.getUnsubstitutedPrimaryConstructor() != null) {
            if (DescriptorUtils.canHaveDeclaredConstructors(currentClassDescriptor)) {
                // Diagnostic is meaningless when reporting on interfaces and object
                context.trace.report(PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.on(
                        (JetConstructorDelegationCall) calleeExpression.getParent()
                ));
            }
            if (call.isImplicit()) return OverloadResolutionResultsImpl.nameNotFound();
        }

        if (constructors.isEmpty()) {
            context.trace.report(NO_CONSTRUCTOR.on(CallUtilPackage.getValueArgumentListOrElement(context.call)));
            return checkArgumentTypesAndFail(context);
        }

        List<ResolutionCandidate<CallableDescriptor>> candidates = Lists.newArrayList();
        ReceiverValue constructorDispatchReceiver = !delegateClassDescriptor.isInner() ? ReceiverValue.NO_RECEIVER :
                                                    ((ClassDescriptor) delegateClassDescriptor.getContainingDeclaration()).
                                                            getThisAsReceiverParameter().getValue();

        JetType expectedType = isThisCall ?
                               calleeConstructor.getContainingDeclaration().getDefaultType() :
                               DescriptorUtils.getSuperClassType(currentClassDescriptor);

        TypeSubstitutor knownTypeParametersSubstitutor = TypeSubstitutor.create(expectedType);
        for (CallableDescriptor descriptor : constructors) {
            candidates.add(ResolutionCandidate.create(
                    context.call, descriptor, constructorDispatchReceiver, ReceiverValue.NO_RECEIVER,
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                    knownTypeParametersSubstitutor));
        }

        TracingStrategy tracing = call.isImplicit() ?
                                  new TracingStrategyForImplicitConstructorDelegationCall(call, context.call) :
                                  TracingStrategyImpl.create(calleeExpression, context.call);

        return computeTasksFromCandidatesAndResolvedCall(context, candidates, CallTransformer.FUNCTION_CALL_TRANSFORMER, tracing);
    }

    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithKnownCandidate(
            @NotNull final Call call,
            @NotNull final TracingStrategy tracing,
            @NotNull final ResolutionContext<?> context,
            @NotNull final ResolutionCandidate<CallableDescriptor> candidate,
            @Nullable final MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        return callResolvePerfCounter.time(new Function0<OverloadResolutionResults<FunctionDescriptor>>() {
            @Override
            public OverloadResolutionResults<FunctionDescriptor> invoke() {
                BasicCallResolutionContext basicCallResolutionContext =
                        BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, dataFlowInfoForArguments);

                List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> tasks =
                        taskPrioritizer.<CallableDescriptor, FunctionDescriptor>computePrioritizedTasksFromCandidates(
                                basicCallResolutionContext, Collections.singleton(candidate), tracing);
                return doResolveCallOrGetCachedResults(basicCallResolutionContext, tasks, CallTransformer.FUNCTION_CALL_TRANSFORMER, tracing);
            }
        });
    }

    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> doResolveCallOrGetCachedResults(
            @NotNull BasicCallResolutionContext context,
            @NotNull List<ResolutionTask<D, F>> prioritizedTasks,
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull TracingStrategy tracing
    ) {
        Call call = context.call;
        tracing.bindCall(context.trace, call);

        TemporaryBindingTrace traceToResolveCall = TemporaryBindingTrace.create(context.trace, "trace to resolve call", call);
        BasicCallResolutionContext newContext = context.replaceBindingTrace(traceToResolveCall);

        recordScope(newContext.trace, newContext.scope, newContext.call.getCalleeExpression());
        recordDataFlowInfo(newContext, newContext.call.getCalleeExpression());

        OverloadResolutionResultsImpl<F> results = doResolveCall(newContext, prioritizedTasks, callTransformer, tracing);
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
        if (CallResolverUtilPackage.isInvokeCallOnVariable(context.call)) return;
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
        if (CallResolverUtilPackage.isInvokeCallOnVariable(call)) return;

        DelegatingBindingTrace deltasTraceToCacheResolve = new DelegatingBindingTrace(
                BindingContext.EMPTY, "delta trace for caching resolve of", context.call);
        traceToResolveCall.addOwnDataTo(deltasTraceToCacheResolve);

        context.resolutionResultsCache.record(call, results, context, tracing, deltasTraceToCacheResolve);
    }

    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> checkArgumentTypesAndFail(BasicCallResolutionContext context) {
        argumentTypeResolver.checkTypesWithNoCallee(context);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> doResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull List<ResolutionTask<D, F>> prioritizedTasks, // high to low priority
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull TracingStrategy tracing
    ) {
        if (context.checkArguments == CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) {
            argumentTypeResolver.analyzeArgumentsAndRecordTypes(context);
        }
        Collection<ResolvedCall<F>> allCandidates = Lists.newArrayList();
        OverloadResolutionResultsImpl<F> successfulResults = null;
        TemporaryBindingTrace traceForFirstNonemptyCandidateSet = null;
        OverloadResolutionResultsImpl<F> resultsForFirstNonemptyCandidateSet = null;
        for (ResolutionTask<D, F> task : prioritizedTasks) {
            if (task.getCandidates().isEmpty()) continue;

            TemporaryBindingTrace taskTrace =
                    TemporaryBindingTrace.create(context.trace, "trace to resolve a task for", task.call.getCalleeExpression());
            OverloadResolutionResultsImpl<F> results = performResolution(task.replaceBindingTrace(taskTrace), callTransformer);


            allCandidates.addAll(task.getResolvedCalls());

            if (successfulResults != null) continue;

            if (results.isSuccess() || results.isAmbiguity()) {
                taskTrace.commit();
                successfulResults = results;
            }
            if (results.getResultCode() == INCOMPLETE_TYPE_INFERENCE) {
                results.setTrace(taskTrace);
                successfulResults = results;
            }
            boolean updateResults = traceForFirstNonemptyCandidateSet == null
                                    || (resultsForFirstNonemptyCandidateSet.getResultCode() == CANDIDATES_WITH_WRONG_RECEIVER
                                        && results.getResultCode() != CANDIDATES_WITH_WRONG_RECEIVER);
            if (!task.getCandidates().isEmpty() && !results.isNothing() && updateResults) {
                traceForFirstNonemptyCandidateSet = taskTrace;
                resultsForFirstNonemptyCandidateSet = results;
            }

            if (successfulResults != null && !context.collectAllCandidates) break;
        }
        OverloadResolutionResultsImpl<F> results;
        if (successfulResults != null) {
            results = successfulResults;
        }
        else if (traceForFirstNonemptyCandidateSet == null) {
            tracing.unresolvedReference(context.trace);
            argumentTypeResolver.checkTypesWithNoCallee(context, SHAPE_FUNCTION_ARGUMENTS);
            results = OverloadResolutionResultsImpl.<F>nameNotFound();
        }
        else {
            traceForFirstNonemptyCandidateSet.commit();
            results = resultsForFirstNonemptyCandidateSet;
        }
        results.setAllCandidates(context.collectAllCandidates ? allCandidates : null);
        return results;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> performResolution(
            @NotNull ResolutionTask<D, F> task,
            @NotNull CallTransformer<D, F> callTransformer
    ) {
        List<CallCandidateResolutionContext<D>> contexts = collectCallCandidateContext(task, callTransformer, EXIT_ON_FIRST_ERROR);
        boolean isSuccess = ContainerUtil.exists(contexts, new Condition<CallCandidateResolutionContext<D>>() {
            @Override
            public boolean value(CallCandidateResolutionContext<D> context) {
                return context.candidateCall.getStatus().possibleTransformToSuccess();
            }
        });
        if (!isSuccess) {
            contexts = collectCallCandidateContext(task, callTransformer, FULLY);
        }

        for (CallCandidateResolutionContext<D> context : contexts) {
            addResolvedCall(task, callTransformer, context);
        }

        OverloadResolutionResultsImpl<F> results = resolutionResultsHandler.computeResultAndReportErrors(
                task, task.getResolvedCalls());
        if (!results.isSingleResult() && !results.isIncomplete()) {
            argumentTypeResolver.checkTypesWithNoCallee(task.toBasic());
        }
        return results;
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> List<CallCandidateResolutionContext<D>> collectCallCandidateContext(
            @NotNull final ResolutionTask<D, F> task,
            @NotNull final CallTransformer<D, F> callTransformer,
            @NotNull final CandidateResolveMode candidateResolveMode
    ) {
        final List<CallCandidateResolutionContext<D>> candidateResolutionContexts = ContainerUtil.newArrayList();
        for (final ResolutionCandidate<D> resolutionCandidate : task.getCandidates()) {
            if (DescriptorUtilPackage.isAnnotatedAsHidden(resolutionCandidate.getDescriptor())) continue;

            candidatePerfCounter.time(new Function0<Unit>() {
                @Override
                public Unit invoke() {
                    TemporaryBindingTrace candidateTrace = TemporaryBindingTrace.create(
                            task.trace, "trace to resolve candidate");
                    Collection<CallCandidateResolutionContext<D>> contexts =
                            callTransformer.createCallContexts(resolutionCandidate, task, candidateTrace, candidateResolveMode);
                    for (CallCandidateResolutionContext<D> context : contexts) {
                        candidateResolver.performResolutionForCandidateCall(context, task);
                        candidateResolutionContexts.add(context);
                    }
                    return Unit.INSTANCE$;
                }
            });
        }
        return candidateResolutionContexts;
    }

    private <D extends CallableDescriptor, F extends D> void addResolvedCall(
            @NotNull ResolutionTask<D, F> task,
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull CallCandidateResolutionContext<D> context) {
        /* important for 'variable as function case': temporary bind reference to descriptor (will be rewritten)
        to have a binding to variable while 'invoke' call resolve */
        task.tracing.bindReference(context.candidateCall.getTrace(), context.candidateCall);

        Collection<MutableResolvedCall<F>> resolvedCalls = callTransformer.transformCall(context, this, task);

        for (MutableResolvedCall<F> resolvedCall : resolvedCalls) {
            BindingTrace trace = resolvedCall.getTrace();
            task.tracing.bindReference(trace, resolvedCall);
            task.tracing.bindResolvedCall(trace, resolvedCall);
            task.addResolvedCall(resolvedCall);
        }
    }
}
