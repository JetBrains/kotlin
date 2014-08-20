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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiElement;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.context.*;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionResultsHandler;
import org.jetbrains.jet.lang.resolve.calls.tasks.*;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.util.DelegatingCall;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.lexer.JetTokens;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.NOT_A_CLASS;
import static org.jetbrains.jet.lang.diagnostics.Errors.NO_CONSTRUCTOR;
import static org.jetbrains.jet.lang.resolve.BindingContext.NON_DEFAULT_EXPRESSION_DATA_FLOW;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLUTION_SCOPE;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults.Code.*;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

@SuppressWarnings("RedundantTypeArguments")
public class CallResolver {
    @NotNull
    private ExpressionTypingServices expressionTypingServices;
    @NotNull
    private TypeResolver typeResolver;
    @NotNull
    private CandidateResolver candidateResolver;
    @NotNull
    private ArgumentTypeResolver argumentTypeResolver;
    @NotNull
    private CallCompleter callCompleter;
    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setCandidateResolver(@NotNull CandidateResolver candidateResolver) {
        this.candidateResolver = candidateResolver;
    }

    @Inject
    public void setArgumentTypeResolver(@NotNull ArgumentTypeResolver argumentTypeResolver) {
        this.argumentTypeResolver = argumentTypeResolver;
    }

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
        TracingStrategy tracing = TracingStrategyImpl.create(nameExpression, context.call);
        List<ResolutionTask<VariableDescriptor, VariableDescriptor>> prioritizedTasks =
                TaskPrioritizer.<VariableDescriptor, VariableDescriptor>computePrioritizedTasks(
                        context, referencedName, tracing, callableDescriptorCollectors);
        return doResolveCallOrGetCachedResults(context, prioritizedTasks, CallTransformer.PROPERTY_CALL_TRANSFORMER, tracing);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull ExpressionTypingContext context,
            @NotNull Call call,
            @NotNull JetReferenceExpression functionReference,
            @NotNull Name name
    ) {
        return resolveCallWithGivenName(
                BasicCallResolutionContext.create(context, call, CheckValueArgumentsMode.ENABLED),
                functionReference,
                name
        );
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull BasicCallResolutionContext context,
            @NotNull JetReferenceExpression functionReference,
            @NotNull Name name
    ) {
        TracingStrategy tracing = TracingStrategyImpl.create(functionReference, context.call);
        return resolveCallWithGivenName(context, name, tracing, CallableDescriptorCollectors.FUNCTIONS_AND_VARIABLES);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallForInvoke(
            @NotNull BasicCallResolutionContext context,
            @NotNull TracingStrategy tracing
    ) {
        return resolveCallWithGivenName(context, Name.identifier("invoke"), tracing, CallableDescriptorCollectors.FUNCTIONS);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull TracingStrategy tracing,
            @NotNull CallableDescriptorCollectors<CallableDescriptor> collectors
    ) {
        List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> tasks =
                TaskPrioritizer.<CallableDescriptor, FunctionDescriptor>computePrioritizedTasks(context, name, tracing, collectors);
        return doResolveCallOrGetCachedResults(context, tasks, CallTransformer.FUNCTION_CALL_TRANSFORMER, tracing);
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
            @NotNull JetScope scope,
            @NotNull Call call,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            boolean isAnnotationContext
    ) {
        return resolveFunctionCall(
                BasicCallResolutionContext.create(
                        trace, scope, call, expectedType, dataFlowInfo, ContextDependency.INDEPENDENT, CheckValueArgumentsMode.ENABLED,
                        expressionTypingServices.createExtension(scope, isAnnotationContext), isAnnotationContext)
        );
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(@NotNull BasicCallResolutionContext context) {

        ProgressIndicatorProvider.checkCanceled();

        List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> prioritizedTasks;

        JetExpression calleeExpression = context.call.getCalleeExpression();
        JetReferenceExpression functionReference;
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression expression = (JetSimpleNameExpression) calleeExpression;
            functionReference = expression;

            Name name = expression.getReferencedNameAsName();

            TracingStrategy tracing = TracingStrategyImpl.create(expression, context.call);
            prioritizedTasks = TaskPrioritizer.<CallableDescriptor, FunctionDescriptor>computePrioritizedTasks(
                    context, name, tracing, CallableDescriptorCollectors.FUNCTIONS_AND_VARIABLES);
            ResolutionTask.DescriptorCheckStrategy abstractConstructorCheck = new ResolutionTask.DescriptorCheckStrategy() {
                @Override
                public <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing) {
                    if (descriptor instanceof ConstructorDescriptor) {
                        Modality modality = ((ConstructorDescriptor) descriptor).getContainingDeclaration().getModality();
                        if (modality == Modality.ABSTRACT) {
                            tracing.instantiationOfAbstractClass(trace);
                            return false;
                        }
                    }
                    return true;
                }
            };
            for (ResolutionTask task : prioritizedTasks) {
                task.setCheckingStrategy(abstractConstructorCheck);
            }
        }
        else {
            JetValueArgumentList valueArgumentList = context.call.getValueArgumentList();
            PsiElement reportAbsenceOn = valueArgumentList == null ? context.call.getCallElement() : valueArgumentList;
            if (calleeExpression instanceof JetConstructorCalleeExpression) {
                assert !context.call.getExplicitReceiver().exists();

                JetConstructorCalleeExpression expression = (JetConstructorCalleeExpression) calleeExpression;
                functionReference = expression.getConstructorReferenceExpression();
                if (functionReference == null) {
                    return checkArgumentTypesAndFail(context); // No type there
                }
                JetTypeReference typeReference = expression.getTypeReference();
                assert typeReference != null;
                JetType constructedType = typeResolver.resolveType(context.scope, typeReference, context.trace, true);

                if (constructedType.isError()) {
                    return checkArgumentTypesAndFail(context);
                }

                DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
                if (declarationDescriptor instanceof ClassDescriptor) {
                    ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                    Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
                    if (constructors.isEmpty()) {
                        context.trace.report(NO_CONSTRUCTOR.on(reportAbsenceOn));
                        return checkArgumentTypesAndFail(context);
                    }
                    Collection<ResolutionCandidate<CallableDescriptor>> candidates =
                            TaskPrioritizer.<CallableDescriptor>convertWithImpliedThisAndNoReceiver(
                                    context.scope, constructors, context.call);
                    prioritizedTasks = TaskPrioritizer.<CallableDescriptor, FunctionDescriptor>computePrioritizedTasksFromCandidates(
                            context, candidates, TracingStrategyImpl.create(functionReference, context.call));
                }
                else {
                    context.trace.report(NOT_A_CLASS.on(calleeExpression));
                    return checkArgumentTypesAndFail(context);
                }
            }
            else if (calleeExpression instanceof JetThisReferenceExpression) {
                functionReference = (JetThisReferenceExpression) calleeExpression;
                DeclarationDescriptor containingDeclaration = context.scope.getContainingDeclaration();
                if (containingDeclaration instanceof ConstructorDescriptor) {
                    containingDeclaration = containingDeclaration.getContainingDeclaration();
                }
                assert containingDeclaration instanceof ClassDescriptor;
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

                Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
                if (constructors.isEmpty()) {
                    context.trace.report(NO_CONSTRUCTOR.on(reportAbsenceOn));
                    return checkArgumentTypesAndFail(context);
                }
                List<ResolutionCandidate<CallableDescriptor>> candidates = ResolutionCandidate.<CallableDescriptor>convertCollection(
                        context.call, constructors, JetPsiUtil.isSafeCall(context.call));
                prioritizedTasks = Collections.singletonList(new ResolutionTask<CallableDescriptor, FunctionDescriptor>(candidates, functionReference, context)); // !! DataFlowInfo.EMPTY
            }
            else if (calleeExpression != null) {

                // Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
                JetType calleeType = expressionTypingServices.safeGetType(context.scope, calleeExpression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace); // We are actually expecting a function, but there seems to be no easy way of expressing this
                ExpressionReceiver expressionReceiver = new ExpressionReceiver(calleeExpression, calleeType);

                Call call = new CallTransformer.CallForImplicitInvoke(
                        context.call.getExplicitReceiver(), expressionReceiver, context.call);
                TracingStrategyForInvoke tracingForInvoke = new TracingStrategyForInvoke(
                        calleeExpression, call, calleeType);
                return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke);
            }
            else {
                return checkArgumentTypesAndFail(context);
            }
        }

        TracingStrategy tracing = TracingStrategyImpl.create(functionReference, context.call);
        OverloadResolutionResultsImpl<FunctionDescriptor> results = doResolveCallOrGetCachedResults(
                context, prioritizedTasks, CallTransformer.FUNCTION_CALL_TRANSFORMER, tracing);
        if (calleeExpression instanceof JetSimpleNameExpression) {
            ExpressionTypingUtils.checkCapturingInClosure((JetSimpleNameExpression) calleeExpression, context.trace, context.scope);
        }
        return results;
    }

    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithKnownCandidate(
            @NotNull Call call,
            @NotNull TracingStrategy tracing,
            @NotNull ResolutionContext<?> context,
            @NotNull ResolutionCandidate<CallableDescriptor> candidate,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        BasicCallResolutionContext basicCallResolutionContext =
                BasicCallResolutionContext.create(context, call, CheckValueArgumentsMode.ENABLED, dataFlowInfoForArguments);

        List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> tasks =
                TaskPrioritizer.<CallableDescriptor, FunctionDescriptor>computePrioritizedTasksFromCandidates(
                        basicCallResolutionContext, Collections.singleton(candidate), tracing);
        return doResolveCallOrGetCachedResults(
                basicCallResolutionContext, tasks, CallTransformer.FUNCTION_CALL_TRANSFORMER, tracing);
    }

    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> doResolveCallOrGetCachedResults(
            @NotNull BasicCallResolutionContext context,
            @NotNull List<ResolutionTask<D, F>> prioritizedTasks,
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull TracingStrategy tracing
    ) {
        Call call = context.call;
        tracing.bindCall(context.trace, call);

        OverloadResolutionResultsImpl<F> results = null;
        TemporaryBindingTrace traceToResolveCall = TemporaryBindingTrace.create(context.trace, "trace to resolve call", call);
        if (!CallResolverUtil.isInvokeCallOnVariable(call)) {
            ResolutionResultsCache.CachedData data = context.resolutionResultsCache.get(call);
            if (data != null) {
                DelegatingBindingTrace deltasTraceForResolve = data.getResolutionTrace();
                deltasTraceForResolve.addAllMyDataTo(traceToResolveCall);
                //noinspection unchecked
                results = (OverloadResolutionResultsImpl<F>) data.getResolutionResults();
            }
        }
        if (results == null) {
            BasicCallResolutionContext newContext = context.replaceBindingTrace(traceToResolveCall);
            results = doResolveCallAndRecordDebugInfo(newContext, prioritizedTasks, callTransformer, tracing);
            DelegatingBindingTrace deltasTraceForTypeInference = ((OverloadResolutionResultsImpl) results).getTrace();
            if (deltasTraceForTypeInference != null) {
                deltasTraceForTypeInference.addAllMyDataTo(traceToResolveCall);
            }
            completeTypeInferenceDependentOnFunctionLiterals(newContext, results, tracing);
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
        if (CallResolverUtil.isInvokeCallOnVariable(context.call)) return;
        if (!results.isSingleResult()) {
            if (results.getResultCode() == INCOMPLETE_TYPE_INFERENCE) {
                argumentTypeResolver.checkTypesWithNoCallee(context, RESOLVE_FUNCTION_ARGUMENTS);
            }
            return;
        }

        CallCandidateResolutionContext<D> candidateContext = CallCandidateResolutionContext.createForCallBeingAnalyzed(
                results.getResultingCall(), context, tracing);
        candidateResolver.completeTypeInferenceDependentOnFunctionLiteralsForCall(candidateContext);
    }

    private static <F extends CallableDescriptor> void cacheResults(
            @NotNull BasicCallResolutionContext context,
            @NotNull OverloadResolutionResultsImpl<F> results,
            @NotNull DelegatingBindingTrace traceToResolveCall,
            @NotNull TracingStrategy tracing
    ) {
        Call call = context.call;
        if (CallResolverUtil.isInvokeCallOnVariable(call)) return;

        DelegatingBindingTrace deltasTraceToCacheResolve = new DelegatingBindingTrace(
                BindingContext.EMPTY, "delta trace for caching resolve of", context.call);
        traceToResolveCall.addAllMyDataTo(deltasTraceToCacheResolve);

        context.resolutionResultsCache.record(call, results, context, tracing, deltasTraceToCacheResolve);
    }

    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> checkArgumentTypesAndFail(BasicCallResolutionContext context) {
        argumentTypeResolver.checkTypesWithNoCallee(context);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> doResolveCallAndRecordDebugInfo(
            @NotNull BasicCallResolutionContext context,
            @NotNull List<ResolutionTask<D, F>> prioritizedTasks, // high to low priority
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull TracingStrategy tracing
    ) {
        context.trace.record(RESOLUTION_SCOPE, context.call.getCalleeExpression(), context.scope);

        if (context.dataFlowInfo.hasTypeInfoConstraints()) {
            context.trace.record(NON_DEFAULT_EXPRESSION_DATA_FLOW, context.call.getCalleeExpression(), context.dataFlowInfo);
        }

        return doResolveCall(context, prioritizedTasks, callTransformer, tracing);
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> doResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull List<ResolutionTask<D, F>> prioritizedTasks, // high to low priority
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull TracingStrategy tracing
    ) {
        if (context.checkArguments == CheckValueArgumentsMode.ENABLED) {
            argumentTypeResolver.analyzeArgumentsAndRecordTypes(context);
        }
        Collection<ResolvedCall<F>> allCandidates = Lists.newArrayList();
        OverloadResolutionResultsImpl<F> successfulResults = null;
        TemporaryBindingTrace traceForFirstNonemptyCandidateSet = null;
        OverloadResolutionResultsImpl<F> resultsForFirstNonemptyCandidateSet = null;
        for (ResolutionTask<D, F> task : prioritizedTasks) {
            if (successfulResults != null && !context.collectAllCandidates) continue;

            TemporaryBindingTrace taskTrace =
                    TemporaryBindingTrace.create(context.trace, "trace to resolve a task for", task.call.getCalleeExpression());
            OverloadResolutionResultsImpl<F> results = performResolutionGuardedForExtraFunctionLiteralArguments(
                    task.replaceBindingTrace(taskTrace), callTransformer);

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
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> performResolutionGuardedForExtraFunctionLiteralArguments(
            @NotNull final ResolutionTask<D, F> task,
            @NotNull CallTransformer<D, F> callTransformer
    ) {
        OverloadResolutionResultsImpl<F> results = performResolution(task, callTransformer);

        // If resolution fails, we should check for some of the following situations:
        //   class A {
        //     val foo = Bar() // The following is intended to be an anonymous initializer,
        //                     // but is treated as a function literal argument
        //     {
        //       ...
        //     }
        //  }
        //
        //  fun foo() {
        //    bar {
        //      buzz()
        //      {...} // intended to be a returned from the outer literal
        //    }
        //  }
        ImmutableSet<OverloadResolutionResults.Code> someFailed = ImmutableSet.of(MANY_FAILED_CANDIDATES,
                                                                        SINGLE_CANDIDATE_ARGUMENT_MISMATCH);
        if (someFailed.contains(results.getResultCode()) && !task.call.getFunctionLiteralArguments().isEmpty()
                && task.contextDependency == ContextDependency.INDEPENDENT) { //For nested calls there are no such cases
            // We have some candidates that failed for some reason
            // And we have a suspect: the function literal argument
            // Now, we try to remove this argument and see if it helps
            DelegatingCall callWithoutFLArgs = new DelegatingCall(task.call) {
                @NotNull
                @Override
                public List<? extends ValueArgument> getValueArguments() {
                    return CallUtilPackage.getValueArgumentsInParentheses(task.call);
                }

                @NotNull
                @Override
                public List<JetFunctionLiteralArgument> getFunctionLiteralArguments() {
                    return Collections.emptyList();
                }
            };
            TemporaryBindingTrace temporaryTrace =
                    TemporaryBindingTrace.create(task.trace, "trace for resolution guarded for extra function literal arguments");
            ResolutionTask<D, F> newTask = new ResolutionTask<D, F>(task.getCandidates(), task.toBasic(), task.tracing).
                    replaceBindingTrace(temporaryTrace).replaceCall(callWithoutFLArgs);

            OverloadResolutionResultsImpl<F> resultsWithFunctionLiteralsStripped = performResolution(newTask, callTransformer);
            if (resultsWithFunctionLiteralsStripped.isSuccess() || resultsWithFunctionLiteralsStripped.isAmbiguity()) {
                task.tracing.danglingFunctionLiteralArgumentSuspected(task.trace, task.call.getFunctionLiteralArguments());
            }
        }

        return results;
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> performResolution(
            @NotNull ResolutionTask<D, F> task,
            @NotNull CallTransformer<D, F> callTransformer
    ) {

        for (ResolutionCandidate<D> resolutionCandidate : task.getCandidates()) {
            TemporaryBindingTrace candidateTrace = TemporaryBindingTrace.create(
                    task.trace, "trace to resolve candidate");
            Collection<CallCandidateResolutionContext<D>> contexts = callTransformer.createCallContexts(resolutionCandidate, task, candidateTrace);
            for (CallCandidateResolutionContext<D> context : contexts) {

                candidateResolver.performResolutionForCandidateCall(context, task);

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

        OverloadResolutionResultsImpl<F> results = ResolutionResultsHandler.INSTANCE.computeResultAndReportErrors(
                task, task.getResolvedCalls());
        if (!results.isSingleResult() && !results.isIncomplete()) {
            argumentTypeResolver.checkTypesWithNoCallee(task.toBasic());
        }
        return results;
    }
}
