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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.context.*;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionDebugInfo;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionResultsHandler;
import org.jetbrains.jet.lang.resolve.calls.tasks.*;
import org.jetbrains.jet.lang.resolve.calls.util.DelegatingCall;
import org.jetbrains.jet.lang.resolve.calls.util.ExpressionAsFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.calls.util.JetFakeReference;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.lang.types.expressions.LabelResolver;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.NON_DEFAULT_EXPRESSION_DATA_FLOW;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLUTION_SCOPE;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults.Code.*;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;
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

    @NotNull
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull BasicCallResolutionContext context) {
        JetExpression calleeExpression = context.call.getCalleeExpression();
        assert calleeExpression instanceof JetSimpleNameExpression;
        JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) calleeExpression;
        Name referencedName = nameExpression.getReferencedNameAsName();
        List<CallableDescriptorCollector<? extends VariableDescriptor>> callableDescriptorCollectors = Lists.newArrayList();
        if (nameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
            referencedName = Name.identifier(referencedName.asString().substring(1));
            callableDescriptorCollectors.add(CallableDescriptorCollectors.PROPERTIES);
        }
        else {
            callableDescriptorCollectors.add(CallableDescriptorCollectors.VARIABLES);
        }
        List<ResolutionTask<VariableDescriptor, VariableDescriptor>> prioritizedTasks =
                TaskPrioritizer.<VariableDescriptor, VariableDescriptor>computePrioritizedTasks(context, referencedName, nameExpression,
                                                                                                callableDescriptorCollectors);
        return doResolveCallOrGetCachedResults(ResolutionResultsCache.PROPERTY_MEMBER_TYPE,
                context, prioritizedTasks, CallTransformer.PROPERTY_CALL_TRANSFORMER, nameExpression);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull BasicCallResolutionContext context,
            @NotNull JetReferenceExpression functionReference,
            @NotNull Name name) {
        List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> tasks =
                TaskPrioritizer.<CallableDescriptor, FunctionDescriptor>computePrioritizedTasks(context, name, functionReference, CallableDescriptorCollectors.FUNCTIONS_AND_VARIABLES);
        return doResolveCallOrGetCachedResults(ResolutionResultsCache.FUNCTION_MEMBER_TYPE,
                                               context, tasks, CallTransformer.FUNCTION_CALL_TRANSFORMER, functionReference);
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
        return resolveFunctionCall(BasicCallResolutionContext.create(
                trace, scope, call, expectedType, dataFlowInfo, ContextDependency.INDEPENDENT, CheckValueArgumentsMode.ENABLED,
                ResolutionResultsCacheImpl.create(), LabelResolver.create(), null,
                expressionTypingServices.createExtension(scope, isAnnotationContext), isAnnotationContext));
    }

    @NotNull
    /*package*/ OverloadResolutionResultsImpl<FunctionDescriptor> resolveFunctionCall(@NotNull BasicCallResolutionContext context) {

        ProgressIndicatorProvider.checkCanceled();

        List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> prioritizedTasks;

        JetExpression calleeExpression = context.call.getCalleeExpression();
        JetReferenceExpression functionReference;
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression expression = (JetSimpleNameExpression) calleeExpression;
            functionReference = expression;

            ExpressionTypingUtils.checkCapturingInClosure(expression, context.trace, context.scope);

            Name name = expression.getReferencedNameAsName();

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
                            TaskPrioritizer.<CallableDescriptor>convertWithImpliedThisAndNoReceiver(context.scope, constructors);
                    prioritizedTasks = TaskPrioritizer.<CallableDescriptor, FunctionDescriptor>computePrioritizedTasksFromCandidates(
                            context, functionReference, candidates, null);
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

                if (!KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(calleeType)) {
//                    checkTypesWithNoCallee(trace, scope, call);
                    if (!calleeType.isError()) {
                        context.trace.report(CALLEE_NOT_A_FUNCTION.on(calleeExpression, calleeType));
                    }
                    return checkArgumentTypesAndFail(context);
                }

                FunctionDescriptorImpl functionDescriptor = new ExpressionAsFunctionDescriptor(context.scope.getContainingDeclaration(), Name.special("<for expression " + calleeExpression.getText() + ">"), calleeExpression);
                FunctionDescriptorUtil.initializeFromFunctionType(functionDescriptor, calleeType, NO_RECEIVER_PARAMETER, Modality.FINAL,
                                                                  Visibilities.LOCAL);
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

        return doResolveCallOrGetCachedResults(ResolutionResultsCache.FUNCTION_MEMBER_TYPE, context, prioritizedTasks,
                                               CallTransformer.FUNCTION_CALL_TRANSFORMER, functionReference);
    }

    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithKnownCandidate(
            @NotNull Call call,
            @Nullable TracingStrategy tracing,
            @NotNull JetReferenceExpression reference,
            @NotNull ResolutionContext<?> context,
            @NotNull ResolutionCandidate<CallableDescriptor> candidate,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        BasicCallResolutionContext basicCallResolutionContext =
                BasicCallResolutionContext.create(context, call, CheckValueArgumentsMode.ENABLED, dataFlowInfoForArguments);

        List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> tasks =
                TaskPrioritizer.<CallableDescriptor, FunctionDescriptor>computePrioritizedTasksFromCandidates(
                        basicCallResolutionContext, reference, Collections.singleton(candidate), tracing);
        return doResolveCallOrGetCachedResults(ResolutionResultsCache.FUNCTION_MEMBER_TYPE, basicCallResolutionContext, tasks,
                                               CallTransformer.FUNCTION_CALL_TRANSFORMER, reference);
    }

    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> doResolveCallOrGetCachedResults(
            @NotNull ResolutionResultsCache.MemberType<F> memberType,
            @NotNull BasicCallResolutionContext context,
            @NotNull List<ResolutionTask<D, F>> prioritizedTasks,
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull JetReferenceExpression reference
    ) {
        OverloadResolutionResultsImpl<F> results = null;
        TracingStrategy tracing = prioritizedTasks.isEmpty() ? TracingStrategy.EMPTY : prioritizedTasks.iterator().next().tracing;
        TemporaryBindingTrace traceToResolveCall = TemporaryBindingTrace.create(context.trace, "trace to resolve call", context.call);
        CallKey callKey = CallResolverUtil.createCallKey(context);
        if (callKey != null) {
            OverloadResolutionResultsImpl<F> cachedResults = context.resolutionResultsCache.getResolutionResults(callKey, memberType);
            if (cachedResults != null) {
                DelegatingBindingTrace deltasTraceForResolve = context.resolutionResultsCache.getResolutionTrace(callKey);
                assert deltasTraceForResolve != null;
                deltasTraceForResolve.addAllMyDataTo(traceToResolveCall);
                results = cachedResults;
            }
        }
        if (results == null) {
            BasicCallResolutionContext newContext = context.replaceBindingTrace(traceToResolveCall);
            results = doResolveCall(newContext, prioritizedTasks, callTransformer, reference);
            DelegatingBindingTrace deltasTraceForTypeInference = ((OverloadResolutionResultsImpl) results).getTrace();
            if (deltasTraceForTypeInference != null) {
                deltasTraceForTypeInference.addAllMyDataTo(traceToResolveCall);
            }
            completeTypeInferenceDependentOnFunctionLiterals(newContext, results, tracing);
            cacheResults(memberType, context, results, traceToResolveCall, tracing);
        }
        traceToResolveCall.commit();

        if (context.contextDependency == ContextDependency.INDEPENDENT) {
            results = completeTypeInferenceDependentOnExpectedType(context, results, tracing);
            if (results.isSingleResult()) {
                //todo clean internal data for several resulting calls
                results.getResultingCall().markCallAsCompleted();
            }
            else {
                argumentTypeResolver.checkTypesForFunctionArgumentsWithNoCallee(context);
                candidateResolver.completeNestedCallsForNotResolvedInvocation(context);
            }
        }

        if (results.isSingleResult()) {
            context.callResolverExtension.run(results.getResultingCall(), context);
        }

        return results;
    }

    private <D extends CallableDescriptor> void completeTypeInferenceDependentOnFunctionLiterals(
            @NotNull BasicCallResolutionContext context,
            @NotNull OverloadResolutionResultsImpl<D> results,
            @NotNull TracingStrategy tracing
    ) {
        if (context.call.getCallType() == Call.CallType.INVOKE) return;
        if (!results.isSingleResult()) {
            if (results.getResultCode() == INCOMPLETE_TYPE_INFERENCE) {
                argumentTypeResolver.checkTypesWithNoCallee(context, RESOLVE_FUNCTION_ARGUMENTS);
            }
            return;
        }

        CallCandidateResolutionContext<D> candidateContext = CallCandidateResolutionContext.createForCallBeingAnalyzed(
                results.getResultingCall().getCallToCompleteTypeArgumentInference(), context, tracing);
        candidateResolver.completeTypeInferenceDependentOnFunctionLiteralsForCall(candidateContext);
    }

    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> completeTypeInferenceDependentOnExpectedType(
            @NotNull BasicCallResolutionContext context,
            @NotNull OverloadResolutionResultsImpl<D> results,
            @NotNull TracingStrategy tracing
    ) {
        if (context.call.getCallType() == Call.CallType.INVOKE) return results;

        if (results.isSingleResult()) {
            Set<ValueArgument> unmappedArguments = results.getResultingCall().getCallToCompleteTypeArgumentInference().getUnmappedArguments();
            argumentTypeResolver.checkUnmappedArgumentTypes(context, unmappedArguments);
            candidateResolver.completeUnmappedArguments(context, unmappedArguments);
        }

        if (!results.isSingleResult()) return results;

        ResolvedCallWithTrace<D> resolvedCall = results.getResultingCall();
        ResolvedCallImpl<D> callToCompleteInference = resolvedCall.getCallToCompleteTypeArgumentInference();

        if (!callToCompleteInference.hasIncompleteTypeParameters()) {
            CallCandidateResolutionContext<D> callCandidateResolutionContext =
                    CallCandidateResolutionContext.createForCallBeingAnalyzed(callToCompleteInference, context, tracing);
            candidateResolver.completeNestedCallsInference(callCandidateResolutionContext);
            candidateResolver.checkValueArgumentTypes(callCandidateResolutionContext);
            return results;
        }

        CallCandidateResolutionContext<D> callCandidateResolutionContext =
                CallCandidateResolutionContext.createForCallBeingAnalyzed(callToCompleteInference, context, tracing);
        candidateResolver.completeTypeInferenceDependentOnExpectedTypeForCall(callCandidateResolutionContext, false);

        if (callToCompleteInference.getStatus().isSuccess()) {
            return OverloadResolutionResultsImpl.success(resolvedCall);
        }
        return OverloadResolutionResultsImpl.incompleteTypeInference(resolvedCall);
    }

    private static <F extends CallableDescriptor> void cacheResults(
            @NotNull ResolutionResultsCache.MemberType<F> memberType,
            @NotNull BasicCallResolutionContext context,
            @NotNull OverloadResolutionResultsImpl<F> results,
            @NotNull DelegatingBindingTrace traceToResolveCall,
            @NotNull TracingStrategy tracing
    ) {
        CallKey callKey = CallResolverUtil.createCallKey(context);
        if (callKey == null) return;

        DelegatingBindingTrace deltasTraceToCacheResolve = new DelegatingBindingTrace(
                BindingContext.EMPTY, "delta trace for caching resolve of", context.call);
        traceToResolveCall.addAllMyDataTo(deltasTraceToCacheResolve);

        context.resolutionResultsCache.recordResolutionResults(callKey, memberType, results);
        context.resolutionResultsCache.recordResolutionTrace(callKey, deltasTraceToCacheResolve);

        if (results.isSingleResult()) {
            ResolvedCallWithTrace<F> resultingCall = results.getResultingCall();
            CallCandidateResolutionContext<F> contextForCallToCompleteTypeArgumentInference = CallCandidateResolutionContext.createForCallBeingAnalyzed(
                    results.getResultingCall().getCallToCompleteTypeArgumentInference(), context, tracing);
            context.resolutionResultsCache.recordDeferredComputationForCall(callKey, resultingCall, contextForCallToCompleteTypeArgumentInference);
        }
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
            @NotNull JetReferenceExpression reference) {

        ResolutionDebugInfo.Data debugInfo = ResolutionDebugInfo.create();
        context.trace.record(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, context.call.getCallElement(), debugInfo);
        context.trace.record(RESOLUTION_SCOPE, context.call.getCalleeExpression(), context.scope);

        if (context.dataFlowInfo.hasTypeInfoConstraints()) {
            context.trace.record(NON_DEFAULT_EXPRESSION_DATA_FLOW, context.call.getCalleeExpression(), context.dataFlowInfo);
        }

        debugInfo.set(ResolutionDebugInfo.TASKS, prioritizedTasks);

        if (context.checkArguments == CheckValueArgumentsMode.ENABLED) {
            argumentTypeResolver.analyzeArgumentsAndRecordTypes(context);
        }

        TemporaryBindingTrace traceForFirstNonemptyCandidateSet = null;
        OverloadResolutionResultsImpl<F> resultsForFirstNonemptyCandidateSet = null;
        for (ResolutionTask<D, F> task : prioritizedTasks) {
            TemporaryBindingTrace taskTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve a task for", task.reference);
            OverloadResolutionResultsImpl<F> results = performResolutionGuardedForExtraFunctionLiteralArguments(
                    task.replaceBindingTrace(taskTrace), callTransformer);
            if (results.isSuccess() || results.isAmbiguity()) {
                taskTrace.commit();

                if (results.isSuccess()) {
                    debugInfo.set(ResolutionDebugInfo.RESULT, results.getResultingCall());
                }
                return results;
            }
            if (results.getResultCode() == INCOMPLETE_TYPE_INFERENCE) {
                results.setTrace(taskTrace);
                return results;
            }
            boolean updateResults = traceForFirstNonemptyCandidateSet == null
                        || (resultsForFirstNonemptyCandidateSet.getResultCode() == CANDIDATES_WITH_WRONG_RECEIVER &&
                            results.getResultCode() != CANDIDATES_WITH_WRONG_RECEIVER);
            if (!task.getCandidates().isEmpty() && !results.isNothing() && updateResults) {
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
            context.trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
            argumentTypeResolver.checkTypesWithNoCallee(context, SHAPE_FUNCTION_ARGUMENTS);
        }
        return resultsForFirstNonemptyCandidateSet != null ? resultsForFirstNonemptyCandidateSet : OverloadResolutionResultsImpl.<F>nameNotFound();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    private <D extends CallableDescriptor, F extends D> OverloadResolutionResultsImpl<F> performResolutionGuardedForExtraFunctionLiteralArguments(
            @NotNull ResolutionTask<D, F> task,
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
                public List<JetExpression> getFunctionLiteralArguments() {
                    return Collections.emptyList();
                }
            };
            TemporaryBindingTrace temporaryTrace =
                    TemporaryBindingTrace.create(task.trace, "trace for resolution guarded for extra function literal arguments");
            ResolutionTask<D, F> newTask = task.replaceBindingTrace(temporaryTrace).replaceCall(callWithoutFLArgs);

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

                Collection<ResolvedCallWithTrace<F>> calls = callTransformer.transformCall(context, this, task);

                for (ResolvedCallWithTrace<F> call : calls) {
                    task.tracing.bindReference(call.getTrace(), call);
                    task.tracing.bindResolvedCall(call.getTrace(), call);
                    task.getResolvedCalls().add(call);
                }
            }
        }

        OverloadResolutionResultsImpl<F> results = ResolutionResultsHandler.INSTANCE.computeResultAndReportErrors(
                task.trace, task.tracing, task.getResolvedCalls());
        if (!results.isSingleResult() && !results.isIncomplete()) {
            argumentTypeResolver.checkTypesWithNoCallee(task.toBasic());
        }
        return results;
    }
}
