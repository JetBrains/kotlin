/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.results.*;
import org.jetbrains.jet.lang.resolve.calls.tasks.*;
import org.jetbrains.jet.lang.resolve.calls.util.DelegatingCall;
import org.jetbrains.jet.lang.resolve.calls.util.ExpressionAsFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.calls.util.JetFakeReference;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus.*;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author abreslav
 */
public class CallResolver {
    private final JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;

    @NotNull
    private ResolutionResultsHandler resolutionResultsHandler;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;
    @NotNull
    private TypeResolver typeResolver;
    @NotNull
    private CandidateResolver candidateResolver;

    @Inject
    public void setResolutionResultsHandler(@NotNull ResolutionResultsHandler resolutionResultsHandler) {
        this.resolutionResultsHandler = resolutionResultsHandler;
    }

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

    @NotNull
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull BasicResolutionContext context) {
        JetExpression calleeExpression = context.call.getCalleeExpression();
        assert calleeExpression instanceof JetSimpleNameExpression;
        JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) calleeExpression;
        Name referencedName = nameExpression.getReferencedNameAsName();
        if (referencedName == null) {
            return OverloadResolutionResultsImpl.nameNotFound();
        }
        List<CallableDescriptorCollector<? extends VariableDescriptor>> callableDescriptorCollectors = Lists.newArrayList();
        if (nameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
            referencedName = Name.identifier(referencedName.getName().substring(1));
            callableDescriptorCollectors.add(CallableDescriptorCollectors.PROPERTIES);
        }
        else {
            callableDescriptorCollectors.add(CallableDescriptorCollectors.VARIABLES);
        }
        List<ResolutionTask<VariableDescriptor, VariableDescriptor>> prioritizedTasks =
                TaskPrioritizer.<VariableDescriptor, VariableDescriptor>computePrioritizedTasks(context, referencedName, nameExpression,
                                                                                                callableDescriptorCollectors);
        return doResolveCallOrGetCachedResults(RESOLUTION_RESULTS_FOR_PROPERTY, context, prioritizedTasks,
                                               CallTransformer.PROPERTY_CALL_TRANSFORMER, nameExpression);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull BasicResolutionContext context,
            @NotNull final JetReferenceExpression functionReference,
            @NotNull Name name) {
        List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> tasks =
                TaskPrioritizer.<CallableDescriptor, FunctionDescriptor>computePrioritizedTasks(context, name, functionReference, CallableDescriptorCollectors.FUNCTIONS_AND_VARIABLES);
        return doResolveCallOrGetCachedResults(RESOLUTION_RESULTS_FOR_FUNCTION, context, tasks, CallTransformer.FUNCTION_CALL_TRANSFORMER,
                                               functionReference);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull Call call,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo) {

        return resolveFunctionCall(BasicResolutionContext.create(trace, scope, call, expectedType, dataFlowInfo));
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(@NotNull BasicResolutionContext context) {

        ProgressIndicatorProvider.checkCanceled();

        List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> prioritizedTasks;
        
        JetExpression calleeExpression = context.call.getCalleeExpression();
        final JetReferenceExpression functionReference;
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression expression = (JetSimpleNameExpression) calleeExpression;
            functionReference = expression;

            ExpressionTypingUtils.checkWrappingInRef(expression, context.trace, context.scope);

            Name name = expression.getReferencedNameAsName();
            if (name == null) return checkArgumentTypesAndFail(context);

            prioritizedTasks = TaskPrioritizer.<CallableDescriptor, FunctionDescriptor>computePrioritizedTasks(context, name, functionReference, CallableDescriptorCollectors.FUNCTIONS_AND_VARIABLES);
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

                prioritizedTasks = Lists.newArrayList();

                JetConstructorCalleeExpression expression = (JetConstructorCalleeExpression) calleeExpression;
                functionReference = expression.getConstructorReferenceExpression();
                if (functionReference == null) {
                    return checkArgumentTypesAndFail(context); // No type there
                }
                JetTypeReference typeReference = expression.getTypeReference();
                assert typeReference != null;
                JetType constructedType = typeResolver.resolveType(context.scope, typeReference, context.trace, true);

                if (ErrorUtils.isErrorType(constructedType)) {
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
                    Collection<ResolutionCandidate<CallableDescriptor>> candidates = TaskPrioritizer.<CallableDescriptor>convertWithImpliedThis(context.scope, Collections.<ReceiverValue>singletonList(NO_RECEIVER), constructors);
                    for (ResolutionCandidate<CallableDescriptor> candidate : candidates) {
                        candidate.setSafeCall(JetPsiUtil.isSafeCall(context.call));
                    }
                    prioritizedTasks.add(new ResolutionTask<CallableDescriptor, FunctionDescriptor>(candidates, functionReference, context));  // !! DataFlowInfo.EMPTY
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
                List<ResolutionCandidate<CallableDescriptor>> candidates = ResolutionCandidate.<CallableDescriptor>convertCollection(constructors, JetPsiUtil.isSafeCall(context.call));
                prioritizedTasks = Collections.singletonList(new ResolutionTask<CallableDescriptor, FunctionDescriptor>(candidates, functionReference, context)); // !! DataFlowInfo.EMPTY
            }
            else if (calleeExpression != null) {
                // Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
                JetType calleeType = expressionTypingServices.safeGetType(context.scope, calleeExpression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace); // We are actually expecting a function, but there seems to be no easy way of expressing this

                if (!KotlinBuiltIns.getInstance().isFunctionType(calleeType)) {
//                    checkTypesWithNoCallee(trace, scope, call);
                    if (!ErrorUtils.isErrorType(calleeType)) {
                        context.trace.report(CALLEE_NOT_A_FUNCTION.on(calleeExpression, calleeType));
                    }
                    return checkArgumentTypesAndFail(context);
                }
                
                FunctionDescriptorImpl functionDescriptor = new ExpressionAsFunctionDescriptor(context.scope.getContainingDeclaration(), Name.special("<for expression " + calleeExpression.getText() + ">"));
                FunctionDescriptorUtil.initializeFromFunctionType(functionDescriptor, calleeType, NO_RECEIVER_PARAMETER, Modality.FINAL, Visibilities.LOCAL);
                ResolutionCandidate<CallableDescriptor> resolutionCandidate = ResolutionCandidate.<CallableDescriptor>create(functionDescriptor, JetPsiUtil.isSafeCall(context.call));
                resolutionCandidate.setReceiverArgument(context.call.getExplicitReceiver());
                resolutionCandidate.setExplicitReceiverKind(ExplicitReceiverKind.RECEIVER_ARGUMENT);

                // strictly speaking, this is a hack:
                // we need to pass a reference, but there's no reference in the PSI,
                // so we wrap what we have into a fake reference and pass it on (unwrap on the other end)
                functionReference = new JetFakeReference(calleeExpression);

                prioritizedTasks = Collections.singletonList(new ResolutionTask<CallableDescriptor, FunctionDescriptor>(Collections.singleton(resolutionCandidate), functionReference, context));
            }
            else {
//                checkTypesWithNoCallee(trace, scope, call);
                return checkArgumentTypesAndFail(context);
            }
        }

        return doResolveCallOrGetCachedResults(RESOLUTION_RESULTS_FOR_FUNCTION, context, prioritizedTasks,
                                               CallTransformer.FUNCTION_CALL_TRANSFORMER, functionReference);
    }

    private <D extends CallableDescriptor, F extends D> OverloadResolutionResults<F> doResolveCallOrGetCachedResults(
            @NotNull WritableSlice<CallKey, OverloadResolutionResults<F>> resolutionResultsSlice,
            @NotNull final BasicResolutionContext context,
            @NotNull final List<ResolutionTask<D, F>> prioritizedTasks,
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull final JetReferenceExpression reference) {
        PsiElement element = context.call.getCallElement();
        OverloadResolutionResults<F> results = null;
        TemporaryBindingTrace traceToResolveCall = TemporaryBindingTrace.create(context.trace, "trace to resolve call", context.call);
        if (element instanceof JetExpression) {
            CallKey key = CallKey.create(context.call.getCallType(), (JetExpression) element);
            OverloadResolutionResults<F> cachedResults = context.trace.get(resolutionResultsSlice, key);
            if (cachedResults != null) {
                DelegatingBindingTrace deltasTraceForResolve = context.trace.get(TRACE_DELTAS_CACHE, (JetExpression) element);
                assert deltasTraceForResolve != null;
                deltasTraceForResolve.addAllMyDataTo(traceToResolveCall);
                results = cachedResults;
            }
        }
        if (results == null) {
            results = doResolveCall(context.replaceTrace(traceToResolveCall), prioritizedTasks, callTransformer, reference);
            if (results instanceof OverloadResolutionResultsImpl) {
                DelegatingBindingTrace deltasTraceForTypeInference = ((OverloadResolutionResultsImpl) results).getTrace();
                if (deltasTraceForTypeInference != null) {
                    deltasTraceForTypeInference.addAllMyDataTo(traceToResolveCall);
                }
            }
            cacheResults(resolutionResultsSlice, context, results, traceToResolveCall);
        }

        if (prioritizedTasks.isEmpty()) {
            traceToResolveCall.commit();
            return results;
        }
        TracingStrategy tracing = prioritizedTasks.iterator().next().tracing;
        OverloadResolutionResults<F> completeResults = completeTypeInferenceDependentOnExpectedType(
                context.replaceTrace(traceToResolveCall), results, tracing);
        traceToResolveCall.commit();
        return completeResults;
    }

    private <D extends CallableDescriptor> OverloadResolutionResults<D> completeTypeInferenceDependentOnExpectedType(
            @NotNull BasicResolutionContext context,
            @NotNull OverloadResolutionResults<D> resultsWithIncompleteTypeInference,
            @NotNull TracingStrategy tracing
    ) {
        if (resultsWithIncompleteTypeInference.getResultCode() != OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE)
            return resultsWithIncompleteTypeInference;
        Set<ResolvedCallWithTrace<D>> successful = Sets.newLinkedHashSet();
        Set<ResolvedCallWithTrace<D>> failed = Sets.newLinkedHashSet();
        for (ResolvedCall<? extends D> call : resultsWithIncompleteTypeInference.getResultingCalls()) {
            if (!(call instanceof ResolvedCallImpl)) continue;
            ResolvedCallImpl<D> resolvedCall = CallResolverUtil.copy((ResolvedCallImpl<D>) call, context);
            if (!resolvedCall.hasUnknownTypeParameters()) {
                if (resolvedCall.getStatus().isSuccess()) {
                    successful.add(resolvedCall);
                }
                else {
                    failed.add(resolvedCall);
                }
                continue;
            }
            candidateResolver.completeTypeInferenceDependentOnExpectedTypeForCall(
                    CallResolutionContext.create(context, tracing, resolvedCall), successful, failed);
        }
        OverloadResolutionResultsImpl<D> results = resolutionResultsHandler.computeResultAndReportErrors(context.trace, tracing, successful,
                                                                                                         failed);
        if (!results.isSingleResult()) {
            candidateResolver.checkTypesWithNoCallee(context);
        }
        return results;
    }

    private <F extends CallableDescriptor> void cacheResults(@NotNull WritableSlice<CallKey, OverloadResolutionResults<F>> resolutionResultsSlice,
            @NotNull BasicResolutionContext context, @NotNull OverloadResolutionResults<F> results,
            @NotNull DelegatingBindingTrace traceToResolveCall) {
        //boolean canBeCached = true;
        //for (ResolvedCall<? extends CallableDescriptor> call : results.getResultingCalls()) {
        //    if (!call.getCandidateDescriptor().getTypeParameters().isEmpty()) {
        //        canBeCached = false;
        //    }
        //}
        //if (!canBeCached) return;
        PsiElement callElement = context.call.getCallElement();
        if (!(callElement instanceof JetExpression)) return;

        DelegatingBindingTrace deltasTraceToCacheResolve = new DelegatingBindingTrace(
                new BindingTraceContext().getBindingContext(), "delta trace for caching resolve of", context.call);
        traceToResolveCall.addAllMyDataTo(deltasTraceToCacheResolve);


        context.trace.record(resolutionResultsSlice, CallKey.create(context.call.getCallType(), (JetExpression)callElement), results);
        context.trace.record(TRACE_DELTAS_CACHE, (JetExpression) callElement, deltasTraceToCacheResolve);
    }

    private <D extends CallableDescriptor> OverloadResolutionResults<D> checkArgumentTypesAndFail(BasicResolutionContext context) {
        candidateResolver.checkTypesWithNoCallee(context);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResults<F> doResolveCall(
            @NotNull final BasicResolutionContext context,
            @NotNull final List<ResolutionTask<D, F>> prioritizedTasks, // high to low priority
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull final JetReferenceExpression reference) {

        ResolutionDebugInfo.Data debugInfo = ResolutionDebugInfo.create();
        context.trace.record(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, context.call.getCallElement(), debugInfo);
        context.trace.record(RESOLUTION_SCOPE, context.call.getCalleeExpression(), context.scope);

        if (context.dataFlowInfo.hasTypeInfoConstraints()) {
            context.trace.record(NON_DEFAULT_EXPRESSION_DATA_FLOW, context.call.getCalleeExpression(), context.dataFlowInfo);
        }

        debugInfo.set(ResolutionDebugInfo.TASKS, prioritizedTasks);

        TemporaryBindingTrace traceForFirstNonemptyCandidateSet = null;
        OverloadResolutionResultsImpl<F> resultsForFirstNonemptyCandidateSet = null;
        for (ResolutionTask<D, F> task : prioritizedTasks) {
            TemporaryBindingTrace taskTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve a task for", task.reference);
            OverloadResolutionResultsImpl<F> results = performResolutionGuardedForExtraFunctionLiteralArguments(task.withTrace(taskTrace),
                                                                                                                callTransformer, context.trace);
            if (results.isSuccess() || results.isAmbiguity()) {
                taskTrace.commit();

                if (results.isSuccess()) {
                    debugInfo.set(ResolutionDebugInfo.RESULT, results.getResultingCall());
                }

                return results;
            }
            if (results.getResultCode() == OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE) {
                results.setTrace(taskTrace);
                return results;
            }
            if (traceForFirstNonemptyCandidateSet == null && !task.getCandidates().isEmpty() && !results.isNothing()) {
                traceForFirstNonemptyCandidateSet = taskTrace;
                resultsForFirstNonemptyCandidateSet = results;
            }
        }
        if (traceForFirstNonemptyCandidateSet != null) {
            traceForFirstNonemptyCandidateSet.commit();
            if (resultsForFirstNonemptyCandidateSet.isSingleResult()) {

                debugInfo.set(ResolutionDebugInfo.RESULT, resultsForFirstNonemptyCandidateSet.getResultingCall());
            }
        }
        else {
            context.trace.report(UNRESOLVED_REFERENCE.on(reference));
            candidateResolver.checkTypesWithNoCallee(context);
        }
        return resultsForFirstNonemptyCandidateSet != null ? resultsForFirstNonemptyCandidateSet : OverloadResolutionResultsImpl.<F>nameNotFound();
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> performResolutionGuardedForExtraFunctionLiteralArguments(
            @NotNull ResolutionTask<D, F> task,
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull BindingTrace traceForResolutionCache) {
        OverloadResolutionResultsImpl<F> results = performResolution(task, callTransformer, traceForResolutionCache);

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
        EnumSet<OverloadResolutionResults.Code> someFailed = EnumSet.of(OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES,
                                                                        OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH);
        if (someFailed.contains(results.getResultCode()) && !task.call.getFunctionLiteralArguments().isEmpty()) {
            // We have some candidates that failed for some reason
            // And we have a suspect: the function literal argument
            // Now, we try to remove this argument and see if it helps
            ResolutionTask<D, F> newTask = new ResolutionTask<D, F>(task.getCandidates(), task.reference,
                        TemporaryBindingTrace.create(task.trace, "trace for resolution guarded for extra function literal arguments"),
                        task.scope, new DelegatingCall(task.call) {
                            @NotNull
                            @Override
                            public List<JetExpression> getFunctionLiteralArguments() {
                                return Collections.emptyList();
                            }
                        }, task.expectedType, task.dataFlowInfo);
            OverloadResolutionResultsImpl<F> resultsWithFunctionLiteralsStripped = performResolution(newTask, callTransformer, traceForResolutionCache);
            if (resultsWithFunctionLiteralsStripped.isSuccess() || resultsWithFunctionLiteralsStripped.isAmbiguity()) {
                task.tracing.danglingFunctionLiteralArgumentSuspected(task.trace, task.call.getFunctionLiteralArguments());
            }
        }

        return results;
    }

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> performResolution(
            @NotNull ResolutionTask<D, F> task,
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull BindingTrace traceForResolutionCache) {

        for (ResolutionCandidate<D> resolutionCandidate : task.getCandidates()) {
            TemporaryBindingTrace candidateTrace = TemporaryBindingTrace.create(
                    task.trace, "trace to resolve candidate");
            Collection<CallResolutionContext<D, F>> contexts = callTransformer.createCallContexts(resolutionCandidate, task, candidateTrace);
            for (CallResolutionContext<D, F> context : contexts) {

                candidateResolver.performResolutionForCandidateCall(context, task);

                /* important for 'variable as function case': temporary bind reference to descriptor (will be rewritten)
                to have a binding to variable while 'invoke' call resolve */
                task.tracing.bindReference(context.candidateCall.getTrace(), context.candidateCall);

                Collection<ResolvedCallWithTrace<F>> calls = callTransformer.transformCall(context, this, task);

                for (ResolvedCallWithTrace<F> call : calls) {
                    task.tracing.bindReference(call.getTrace(), call);
                    task.tracing.bindResolvedCall(call.getTrace(), call);
                    task.getResolvedCalls().add(call);
                }

                context.candidateCall.getTrace().addAllMyDataTo(traceForResolutionCache, new TraceEntryFilter() {
                    @Override
                    public boolean accept(@NotNull WritableSlice<?, ?> slice, Object key) {
                        return slice == BindingContext.RESOLUTION_RESULTS_FOR_FUNCTION || slice == BindingContext.RESOLUTION_RESULTS_FOR_PROPERTY ||
                               slice == BindingContext.TRACE_DELTAS_CACHE;
                    }
                }, false);
            }
        }

        Set<ResolvedCallWithTrace<F>> successfulCandidates = Sets.newLinkedHashSet();
        Set<ResolvedCallWithTrace<F>> failedCandidates = Sets.newLinkedHashSet();
        for (ResolvedCallWithTrace<F> candidateCall : task.getResolvedCalls()) {
            ResolutionStatus status = candidateCall.getStatus();
            if (status.isSuccess()) {
                successfulCandidates.add(candidateCall);
            }
            else {
                assert status != UNKNOWN_STATUS : "No resolution for " + candidateCall.getCandidateDescriptor();
                if (candidateCall.getStatus() != STRONG_ERROR) {
                    failedCandidates.add(candidateCall);
                }
            }
        }
        
        OverloadResolutionResultsImpl<F> results = resolutionResultsHandler.computeResultAndReportErrors(task.trace, task.tracing,
                                                                                                         successfulCandidates,
                                                                                                         failedCandidates);
        if (!results.isSingleResult() && !results.isIncomplete()) {
            candidateResolver.checkTypesWithNoCallee(task.toBasic());
        }
        return results;
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// to be removed

    @Deprecated // Creates wrong resolved calls, should be removed
    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveExactSignature(@NotNull JetScope scope, @NotNull ReceiverValue receiver, @NotNull Name name, @NotNull List<JetType> parameterTypes) {
        List<ResolutionCandidate<FunctionDescriptor>> candidates = findCandidatesByExactSignature(scope, receiver, name, parameterTypes);

        BindingTraceContext trace = new BindingTraceContext();
        TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(trace, "trace for resolve exact signature call", name);
        Set<ResolvedCallWithTrace<FunctionDescriptor>> calls = Sets.newLinkedHashSet();
        for (ResolutionCandidate<FunctionDescriptor> candidate : candidates) {
            ResolvedCallImpl<FunctionDescriptor> call = ResolvedCallImpl.create(candidate, temporaryBindingTrace);
            calls.add(call);
        }
        return resolutionResultsHandler.computeResultAndReportErrors(trace, TracingStrategy.EMPTY, calls,
                                                                     Collections.<ResolvedCallWithTrace<FunctionDescriptor>>emptySet());
    }

    private List<ResolutionCandidate<FunctionDescriptor>> findCandidatesByExactSignature(JetScope scope, ReceiverValue receiver,
                                                                                      Name name, List<JetType> parameterTypes) {
        List<ResolutionCandidate<FunctionDescriptor>> result = Lists.newArrayList();
        if (receiver.exists()) {
            Collection<ResolutionCandidate<FunctionDescriptor>> extensionFunctionDescriptors = ResolutionCandidate.convertCollection(scope.getFunctions(name), false);
            List<ResolutionCandidate<FunctionDescriptor>> nonlocal = Lists.newArrayList();
            List<ResolutionCandidate<FunctionDescriptor>> local = Lists.newArrayList();
            TaskPrioritizer.splitLexicallyLocalDescriptors(extensionFunctionDescriptors, scope.getContainingDeclaration(), local, nonlocal);


            if (findExtensionFunctions(local, receiver, parameterTypes, result)) {
                return result;
            }

            Collection<ResolutionCandidate<FunctionDescriptor>> functionDescriptors = ResolutionCandidate.convertCollection(receiver.getType().getMemberScope().getFunctions(name), false);
            if (lookupExactSignature(functionDescriptors, parameterTypes, result)) {
                return result;

            }
            findExtensionFunctions(nonlocal, receiver, parameterTypes, result);
            return result;
        }
        else {
            lookupExactSignature(ResolutionCandidate.convertCollection(scope.getFunctions(name), false), parameterTypes, result);
            return result;
        }
    }

    private static boolean lookupExactSignature(Collection<ResolutionCandidate<FunctionDescriptor>> candidates, List<JetType> parameterTypes,
                                                List<ResolutionCandidate<FunctionDescriptor>> result) {
        boolean found = false;
        for (ResolutionCandidate<FunctionDescriptor> resolvedCall : candidates) {
            FunctionDescriptor functionDescriptor = resolvedCall.getDescriptor();
            if (functionDescriptor.getReceiverParameter().exists()) continue;
            if (!functionDescriptor.getTypeParameters().isEmpty()) continue;
            if (!checkValueParameters(functionDescriptor, parameterTypes)) continue;
            result.add(resolvedCall);
            found = true;
        }
        return found;
    }

    private boolean findExtensionFunctions(Collection<ResolutionCandidate<FunctionDescriptor>> candidates, ReceiverValue receiver,
                                           List<JetType> parameterTypes, List<ResolutionCandidate<FunctionDescriptor>> result) {
        boolean found = false;
        for (ResolutionCandidate<FunctionDescriptor> candidate : candidates) {
            FunctionDescriptor functionDescriptor = candidate.getDescriptor();
            ReceiverParameterDescriptor functionReceiver = functionDescriptor.getReceiverParameter();
            if (!functionReceiver.exists()) continue;
            if (!functionDescriptor.getTypeParameters().isEmpty()) continue;
            if (!typeChecker.isSubtypeOf(receiver.getType(), functionReceiver.getType())) continue;
            if (!checkValueParameters(functionDescriptor, parameterTypes))continue;
            result.add(candidate);
            found = true;
        }
        return found;
    }

    private static boolean checkValueParameters(@NotNull FunctionDescriptor functionDescriptor, @NotNull List<JetType> parameterTypes) {
        List<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
        if (valueParameters.size() != parameterTypes.size()) return false;
        for (int i = 0; i < valueParameters.size(); i++) {
            ValueParameterDescriptor valueParameter = valueParameters.get(i);
            JetType expectedType = parameterTypes.get(i);
            if (!TypeUtils.equalTypes(expectedType, valueParameter.getType())) return false;
        }
        return true;
    }
}
