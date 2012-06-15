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

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastServiceImpl;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemSolution;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemWithPriorities;
import org.jetbrains.jet.lang.resolve.calls.inference.DebugConstraintResolutionListener;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.calls.ResolutionStatus.*;
import static org.jetbrains.jet.lang.resolve.calls.ResolvedCallImpl.MAP_TO_CANDIDATE;
import static org.jetbrains.jet.lang.resolve.calls.ResolvedCallImpl.MAP_TO_RESULT;
import static org.jetbrains.jet.lang.resolve.calls.inference.ConstraintType.*;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author abreslav
 */
public class CallResolver {
    private static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");

    private final JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;

    @NotNull
    private OverloadingConflictResolver overloadingConflictResolver;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;
    @NotNull
    private TypeResolver typeResolver;
    @NotNull
    private DescriptorResolver descriptorResolver;


    @Inject
    public void setOverloadingConflictResolver(@NotNull OverloadingConflictResolver overloadingConflictResolver) {
        this.overloadingConflictResolver = overloadingConflictResolver;
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
    public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
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
        List<ResolutionTask<VariableDescriptor, VariableDescriptor>> prioritizedTasks = TaskPrioritizer.computePrioritizedTasks(context, referencedName, nameExpression, callableDescriptorCollectors);
        return doResolveCallOrGetCachedResults(RESOLUTION_RESULTS_FOR_PROPERTY, context, prioritizedTasks, CallTransformer.PROPERTY_CALL_TRANSFORMER, nameExpression);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull BasicResolutionContext context,
            @NotNull final JetReferenceExpression functionReference,
            @NotNull Name name) {
        List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> tasks = TaskPrioritizer.computePrioritizedTasks(context, name, functionReference, CallableDescriptorCollectors.FUNCTIONS_AND_VARIABLES);
        return doResolveCallOrGetCachedResults(RESOLUTION_RESULTS_FOR_FUNCTION, context, tasks, CallTransformer.FUNCTION_CALL_TRANSFORMER, functionReference);
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
        List<ResolutionTask<CallableDescriptor, FunctionDescriptor>> prioritizedTasks;
        
        JetExpression calleeExpression = context.call.getCalleeExpression();
        final JetReferenceExpression functionReference;
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression expression = (JetSimpleNameExpression) calleeExpression;
            functionReference = expression;

            Name name = expression.getReferencedNameAsName();
            if (name == null) return checkArgumentTypesAndFail(context);

            prioritizedTasks = TaskPrioritizer.computePrioritizedTasks(context, name, functionReference, CallableDescriptorCollectors.FUNCTIONS_AND_VARIABLES);
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
                DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
                if (declarationDescriptor instanceof ClassDescriptor) {
                    ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                    Collection<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
                    if (constructors.isEmpty()) {
                        context.trace.report(NO_CONSTRUCTOR.on(reportAbsenceOn));
                        return checkArgumentTypesAndFail(context);
                    }
                    Collection<ResolutionCandidate<CallableDescriptor>> candidates = TaskPrioritizer.<CallableDescriptor>convertWithImpliedThis(context.scope, Collections.<ReceiverDescriptor>singletonList(NO_RECEIVER), constructors);
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

                if (!JetStandardClasses.isFunctionType(calleeType)) {
//                    checkTypesWithNoCallee(trace, scope, call);
                    if (!ErrorUtils.isErrorType(calleeType)) {
                        context.trace.report(CALLEE_NOT_A_FUNCTION.on(calleeExpression, calleeType));
                    }
                    return checkArgumentTypesAndFail(context);
                }
                
                FunctionDescriptorImpl functionDescriptor = new ExpressionAsFunctionDescriptor(context.scope.getContainingDeclaration(), Name.special("<for expression " + calleeExpression.getText() + ">"));
                FunctionDescriptorUtil.initializeFromFunctionType(functionDescriptor, calleeType, NO_RECEIVER, Modality.FINAL, Visibilities.LOCAL);
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

        return doResolveCallOrGetCachedResults(RESOLUTION_RESULTS_FOR_FUNCTION, context, prioritizedTasks, CallTransformer.FUNCTION_CALL_TRANSFORMER, functionReference);
    }

    private <D extends CallableDescriptor, F extends D> OverloadResolutionResults<F> doResolveCallOrGetCachedResults(
            @NotNull WritableSlice<CallKey, OverloadResolutionResults<F>> resolutionResultsSlice,
            @NotNull final BasicResolutionContext context,
            @NotNull final List<ResolutionTask<D, F>> prioritizedTasks,
            @NotNull CallTransformer<D, F> callTransformer,
            @NotNull final JetReferenceExpression reference) {
        PsiElement element = context.call.getCallElement();
        if (element instanceof JetExpression) {
            OverloadResolutionResults<F> cachedResults = context.trace.get(resolutionResultsSlice, CallKey.create(context.call.getCallType(), (JetExpression) element));
            if (cachedResults != null) {
                DelegatingBindingTrace delegatingTrace = context.trace.get(TRACE_DELTAS_CACHE, (JetExpression) element);
                assert delegatingTrace != null;
                delegatingTrace.addAllMyDataTo(context.trace);
                return cachedResults;
            }
        }
        TemporaryBindingTrace delegatingBindingTrace = TemporaryBindingTrace.create(context.trace);
        OverloadResolutionResults<F> results = doResolveCall(context.replaceTrace(delegatingBindingTrace),
                                                             prioritizedTasks,
                                                             callTransformer, reference);
        DelegatingBindingTrace cloneDelta = new DelegatingBindingTrace(new BindingTraceContext().getBindingContext());
        delegatingBindingTrace.addAllMyDataTo(cloneDelta);
        cacheResults(resolutionResultsSlice, context, results, cloneDelta);
        delegatingBindingTrace.commit();
        return results;
    }

    private <F extends CallableDescriptor> void cacheResults(@NotNull WritableSlice<CallKey, OverloadResolutionResults<F>> resolutionResultsSlice,
            @NotNull BasicResolutionContext context, @NotNull OverloadResolutionResults<F> results,
            @NotNull DelegatingBindingTrace delegatingBindingTrace) {
        boolean canBeCached = true;
        for (ResolvedCall<? extends CallableDescriptor> call : results.getResultingCalls()) {
            if (!call.getCandidateDescriptor().getTypeParameters().isEmpty()) {
                canBeCached = false;
            }
        }
        if (!canBeCached) return;
        PsiElement callElement = context.call.getCallElement();
        if (!(callElement instanceof JetExpression)) return;

        context.trace.record(resolutionResultsSlice, CallKey.create(context.call.getCallType(), (JetExpression)callElement), results);
        context.trace.record(TRACE_DELTAS_CACHE, (JetExpression) callElement, delegatingBindingTrace);
    }

    private <D extends CallableDescriptor> OverloadResolutionResults<D> checkArgumentTypesAndFail(BasicResolutionContext context) {
        checkTypesWithNoCallee(context);
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
            TemporaryBindingTrace taskTrace = TemporaryBindingTrace.create(context.trace);
            OverloadResolutionResultsImpl<F> results = performResolutionGuardedForExtraFunctionLiteralArguments(task.withTrace(taskTrace),
                                                                                                                callTransformer, context.trace);
            if (results.isSuccess() || results.isAmbiguity()) {
                taskTrace.commit();

                if (results.isSuccess()) {
                    debugInfo.set(ResolutionDebugInfo.RESULT, results.getResultingCall());
                }

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
            checkTypesWithNoCallee(context);
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
            ResolutionTask<D, F> newTask = new ResolutionTask<D, F>(task.getCandidates(), task.reference, TemporaryBindingTrace.create(task.trace), task.scope,
                                                                    new DelegatingCall(task.call) {
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
            TemporaryBindingTrace candidateTrace = TemporaryBindingTrace.create(task.trace);
            Collection<CallResolutionContext<D, F>> contexts = callTransformer.createCallContexts(resolutionCandidate, task, candidateTrace);
            for (CallResolutionContext<D, F> context : contexts) {

                performResolutionForCandidateCall(context, task);

                /* important for 'variable as function case': temporary bind reference to descriptor (will be rewritten)
                to have a binding to variable while 'invoke' call resolve */
                task.tracing.bindReference(context.candidateCall.getTrace(), context.candidateCall);

                Collection<ResolvedCallWithTrace<F>> calls = callTransformer.transformCall(context, this, task);

                for (ResolvedCallWithTrace<F> call : calls) {
                    task.tracing.bindReference(call.getTrace(), call);
                    task.tracing.bindResolvedCall(call.getTrace(), call);
                    task.getResolvedCalls().add(call);
                }

                context.candidateCall.getTrace().addAllMyDataTo(traceForResolutionCache, new Predicate<WritableSlice>() {
                    @Override
                    public boolean apply(@Nullable WritableSlice slice) {
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
        
        OverloadResolutionResultsImpl<F> results = computeResultAndReportErrors(task.trace, task.tracing, successfulCandidates, failedCandidates);
        if (!results.isSingleResult()) {
            checkTypesWithNoCallee(task.toBasic());
        }
        return results;
    }

    private <D extends CallableDescriptor, F extends D> void performResolutionForCandidateCall(@NotNull CallResolutionContext<D, F> context,
            @NotNull ResolutionTask<D, F> task) {

        ResolvedCallImpl<D> candidateCall = context.candidateCall;
        D candidate = candidateCall.getCandidateDescriptor();

        if (ErrorUtils.isError(candidate)) {
            candidateCall.addStatus(SUCCESS);
            checkTypesWithNoCallee(context.toBasic());
            return;
        }

        if (!Visibilities.isVisible(candidate, context.scope.getContainingDeclaration())) {
            candidateCall.addStatus(OTHER_ERROR);
            context.tracing.invisibleMember(context.trace, candidate);
            return;
        }

        Set<ValueArgument> unmappedArguments = Sets.newLinkedHashSet();
        ValueArgumentsToParametersMapper.Status
                argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(context.call, context.tracing,
                                                                                                        candidateCall, unmappedArguments);
        if (!argumentMappingStatus.isSuccess()) {
            if (argumentMappingStatus == ValueArgumentsToParametersMapper.Status.STRONG_ERROR) {
                candidateCall.addStatus(STRONG_ERROR);
            }
            else {
                candidateCall.addStatus(OTHER_ERROR);
            }
            if (argumentMappingStatus != ValueArgumentsToParametersMapper.Status.WEAK_ERROR) {
                checkTypesWithNoCallee(context.toBasic());
                return;
            }
            checkUnmappedArgumentTypes(context.toBasic(), unmappedArguments);
        }

        List<JetTypeProjection> jetTypeArguments = context.call.getTypeArguments();
        if (jetTypeArguments.isEmpty()) {
            if (!candidate.getTypeParameters().isEmpty()) {
                candidateCall.addStatus(inferTypeArguments(context));
            }
            else {
                candidateCall.addStatus(checkAllValueArguments(context));
            }
        }
        else {
            // Explicit type arguments passed

            List<JetType> typeArguments = new ArrayList<JetType>();
            for (JetTypeProjection projection : jetTypeArguments) {
                if (projection.getProjectionKind() != JetProjectionKind.NONE) {
                    context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection));
                }
                JetTypeReference typeReference = projection.getTypeReference();
                if (typeReference != null) {
                    typeArguments.add(typeResolver.resolveType(context.scope, typeReference, context.trace, true));
                }
                else {
                    typeArguments.add(ErrorUtils.createErrorType("Star projection in a call"));
                }
            }
            int expectedTypeArgumentCount = candidate.getTypeParameters().size();
            if (expectedTypeArgumentCount == jetTypeArguments.size()) {

                checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate, context.trace);

                Map<TypeConstructor, TypeProjection>
                        substitutionContext = FunctionDescriptorUtil.createSubstitutionContext((FunctionDescriptor)candidate, typeArguments);
                D substitutedDescriptor = (D) candidate.substitute(TypeSubstitutor.create(substitutionContext));

                candidateCall.setResultingDescriptor(substitutedDescriptor);
                replaceValueParametersWithSubstitutedOnes(candidateCall, substitutedDescriptor);

                List<TypeParameterDescriptor> typeParameters = candidateCall.getCandidateDescriptor().getTypeParameters();
                for (int i = 0; i < typeParameters.size(); i++) {
                    TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
                    candidateCall.recordTypeArgument(typeParameterDescriptor, typeArguments.get(i));
                }
                candidateCall.addStatus(checkAllValueArguments(context));
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

        recordAutoCastIfNecessary(candidateCall.getReceiverArgument(), candidateCall.getTrace());
        recordAutoCastIfNecessary(candidateCall.getThisObject(), candidateCall.getTrace());
    }

    private <D extends CallableDescriptor, F extends D> ResolutionStatus inferTypeArguments(CallResolutionContext<D, F> context) {
        ResolvedCallImpl<D> candidateCall = context.candidateCall;
        D candidate = candidateCall.getCandidateDescriptor();

        ResolutionDebugInfo.Data debugInfo = context.trace.get(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, context.call.getCallElement());

        ConstraintSystem constraintSystem = new ConstraintSystemWithPriorities(new DebugConstraintResolutionListener(candidateCall, debugInfo));

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

                JetType effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument);

                // Here we type check expecting an error type (that is a subtype of any type and a supertype of any type
                // and throw the results away
                // We'll type check the arguments later, with the inferred types expected
                TemporaryBindingTrace traceForUnknown = TemporaryBindingTrace.create(context.trace);
                JetExpression argumentExpression = valueArgument.getArgumentExpression();
                JetType type = argumentExpression != null ? expressionTypingServices.getType(
                        context.scope, argumentExpression, substituteDontCare.substitute(valueParameterDescriptor.getType(), Variance.INVARIANT),
                        context.dataFlowInfo, traceForUnknown) : null;
                if (type != null && !ErrorUtils.isErrorType(type)) {
                    constraintSystem.addSubtypingConstraint(VALUE_ARGUMENT.assertSubtyping(type, effectiveExpectedType));
                }
                else {
                    candidateCall.argumentHasNoType();
                }
            }
        }

        // Receiver
        // Error is already reported if something is missing
        ReceiverDescriptor receiverArgument = candidateCall.getReceiverArgument();
        ReceiverDescriptor receiverParameter = candidateWithFreshVariables.getReceiverParameter();
        if (receiverArgument.exists() && receiverParameter.exists()) {
            constraintSystem.addSubtypingConstraint(RECEIVER.assertSubtyping(receiverArgument.getType(), receiverParameter.getType()));
        }

        // Return type
        if (context.expectedType != NO_EXPECTED_TYPE) {
            constraintSystem.addSubtypingConstraint(EXPECTED_TYPE.assertSubtyping(candidateWithFreshVariables.getReturnType(), context.expectedType));
        }

        // Solution
        ConstraintSystemSolution solution = constraintSystem.solve();
        if (solution.getStatus().isSuccessful()) {
            D substitute = (D) candidateWithFreshVariables.substitute(solution.getSubstitutor());
            assert substitute != null;
            replaceValueParametersWithSubstitutedOnes(candidateCall, substitute);
            candidateCall.setResultingDescriptor(substitute);

            for (TypeParameterDescriptor typeParameterDescriptor : candidateCall.getCandidateDescriptor().getTypeParameters()) {
                candidateCall.recordTypeArgument(typeParameterDescriptor, solution.getValue(candidateWithFreshVariables.getTypeParameters().get(typeParameterDescriptor.getIndex())));
            }

            // Here we type check the arguments with inferred types expected
            checkValueArgumentTypes(context);

            return SUCCESS;
        }
        else {
            context.tracing.typeInferenceFailed(context.trace, solution.getStatus());
            return OTHER_ERROR.combine(checkAllValueArguments(context));
        }
    }

    private static void recordAutoCastIfNecessary(ReceiverDescriptor receiver, BindingTrace trace) {
        if (receiver instanceof AutoCastReceiver) {
            AutoCastReceiver autoCastReceiver = (AutoCastReceiver) receiver;
            ReceiverDescriptor original = autoCastReceiver.getOriginal();
            if (original instanceof ExpressionReceiver) {
                ExpressionReceiver expressionReceiver = (ExpressionReceiver) original;
                if (autoCastReceiver.canCast()) {
                    trace.record(AUTOCAST, expressionReceiver.getExpression(), autoCastReceiver.getType());
                }
                else {
                    trace.report(AUTOCAST_IMPOSSIBLE.on(expressionReceiver.getExpression(), autoCastReceiver.getType(), expressionReceiver.getExpression().getText()));
                }
            }
            else {
                assert autoCastReceiver.canCast() : "A non-expression receiver must always be autocastabe: " + original;
            }
        }
    }

    private void checkTypesWithNoCallee(BasicResolutionContext context) {
        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                expressionTypingServices.getType(context.scope, argumentExpression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
            }
        }

        for (JetExpression expression : context.call.getFunctionLiteralArguments()) {
            expressionTypingServices.getType(context.scope, expression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
        }

        for (JetTypeProjection typeProjection : context.call.getTypeArguments()) {
            JetTypeReference typeReference = typeProjection.getTypeReference();
            if (typeReference == null) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(typeProjection));
            }
            else {
                typeResolver.resolveType(context.scope, typeReference, context.trace, true);
            }
        }
    }

    private void checkUnmappedArgumentTypes(BasicResolutionContext context, Set<ValueArgument> unmappedArguments) {
        for (ValueArgument valueArgument : unmappedArguments) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                expressionTypingServices.getType(context.scope, argumentExpression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
            }
        }
    }

    private static <D extends CallableDescriptor> void replaceValueParametersWithSubstitutedOnes(
            ResolvedCallImpl<D> candidateCall, @NotNull D substitutedDescriptor) {

        Map<ValueParameterDescriptor, ValueParameterDescriptor> parameterMap = Maps.newHashMap();
        for (ValueParameterDescriptor valueParameterDescriptor : substitutedDescriptor.getValueParameters()) {
            parameterMap.put(valueParameterDescriptor.getOriginal(), valueParameterDescriptor);
        }

        Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments = candidateCall.getValueArguments();
        Map<ValueParameterDescriptor, ResolvedValueArgument> originalValueArguments = Maps.newHashMap(valueArguments);
        valueArguments.clear();
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : originalValueArguments.entrySet()) {
            ValueParameterDescriptor substitutedVersion = parameterMap.get(entry.getKey().getOriginal());
            assert substitutedVersion != null : entry.getKey();
            valueArguments.put(substitutedVersion, entry.getValue());
        }
    }

    private <D extends CallableDescriptor, F extends D> ResolutionStatus checkAllValueArguments(CallResolutionContext<D, F> context) {
        ResolutionStatus result = checkValueArgumentTypes(context);
        ResolvedCall<D> candidateCall = context.candidateCall;

        // Comment about a very special case.
        // Call 'b.foo(1)' where class 'Foo' has an extension member 'fun B.invoke(Int)' should be checked two times for safe call (in 'checkReceiver'), because
        // both 'b' (receiver) and 'foo' (this object) might be nullable. In the first case we mark dot, in the second 'foo'.
        // Class 'CallForImplicitInvoke' helps up to recognise this case, and parameter 'implicitInvokeCheck' helps us to distinguish whether we check receiver or this object.

        result = result.combine(checkReceiver(context, candidateCall, candidateCall.getResultingDescriptor().getReceiverParameter(), candidateCall.getReceiverArgument(),
                                              candidateCall.getExplicitReceiverKind().isReceiver(), false));

        result = result.combine(checkReceiver(context, candidateCall, candidateCall.getResultingDescriptor().getExpectedThisObject(), candidateCall.getThisObject(),
                                              candidateCall.getExplicitReceiverKind().isThisObject(),
                                              // for the invocation 'foo(1)' where foo is a variable of function type we should mark 'foo' if there is unsafe call error
                                              context.call instanceof CallTransformer.CallForImplicitInvoke));
        return result;
    }

    private <D extends CallableDescriptor, F extends D> ResolutionStatus checkReceiver(CallResolutionContext<D, F> context, ResolvedCall<D> candidateCall,
            ReceiverDescriptor receiverParameter, ReceiverDescriptor receiverArgument,
            boolean isExplicitReceiver, boolean implicitInvokeCheck) {

        ResolutionStatus result = SUCCESS;
        if (receiverParameter.exists() && receiverArgument.exists()) {
            boolean safeAccess = isExplicitReceiver && !implicitInvokeCheck && candidateCall.isSafeCall();
            JetType receiverArgumentType = receiverArgument.getType();
            AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(context.dataFlowInfo, context.candidateCall.getTrace().getBindingContext());
            if (!safeAccess && !receiverParameter.getType().isNullable() && !autoCastService.isNotNull(receiverArgument)) {

                context.tracing.unsafeCall(context.candidateCall.getTrace(), receiverArgumentType, implicitInvokeCheck);
                result = UNSAFE_CALL_ERROR;
            }
            else {
                JetType effectiveReceiverArgumentType = safeAccess
                                                        ? TypeUtils.makeNotNullable(receiverArgumentType)
                                                        : receiverArgumentType;
                if (!typeChecker.isSubtypeOf(effectiveReceiverArgumentType, receiverParameter.getType())) {
                    context.tracing.wrongReceiverType(context.candidateCall.getTrace(), receiverParameter, receiverArgument);
                    result = OTHER_ERROR;
                }
            }
            if (safeAccess && (receiverParameter.getType().isNullable() || !receiverArgumentType.isNullable())) {
                context.tracing.unnecessarySafeCall(context.candidateCall.getTrace(), receiverArgumentType);
            }
        }
        return result;
    }

    private <D extends CallableDescriptor, F extends D> ResolutionStatus checkValueArgumentTypes(CallResolutionContext<D, F> context) {
        ResolutionStatus result = SUCCESS;
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : context.candidateCall.getValueArguments().entrySet()) {
            ValueParameterDescriptor parameterDescriptor = entry.getKey();
            ResolvedValueArgument resolvedArgument = entry.getValue();


            for (ValueArgument argument : resolvedArgument.getArguments()) {
                JetExpression expression = argument.getArgumentExpression();
                if (expression == null) continue;

                JetType expectedType = getEffectiveExpectedType(parameterDescriptor, argument);
                JetType type = expressionTypingServices.getType(context.scope, expression, expectedType, context.dataFlowInfo, context.candidateCall.getTrace());
                if (type == null || ErrorUtils.isErrorType(type)) {
                    context.candidateCall.argumentHasNoType();
                }
                else if (!typeChecker.isSubtypeOf(type, expectedType)) {
//                    VariableDescriptor variableDescriptor = AutoCastUtils.getVariableDescriptorFromSimpleName(temporaryTrace.getBindingContext(), argument);
//                    if (variableDescriptor != null) {
//                        JetType autoCastType = null;
//                        for (JetType possibleType : dataFlowInfo.getPossibleTypesForVariable(variableDescriptor)) {
//                            if (semanticServices.getTypeChecker().isSubtypeOf(type, parameterType)) {
//                                autoCastType = possibleType;
//                                break;
//                            }
//                        }
//                        if (autoCastType != null) {
//                            if (AutoCastUtils.isStableVariable(variableDescriptor)) {
//                                temporaryTrace.record(AUTOCAST, argument, autoCastType);
//                            }
//                            else {
//                                temporaryTrace.report(AUTOCAST_IMPOSSIBLE.on(argument, autoCastType, variableDescriptor));
//                                result = false;
//                            }
//                        }
//                    }
//                    else {
                    result = OTHER_ERROR;
                }
            }
        }
        return result;
    }

    @NotNull
    private JetType getEffectiveExpectedType(ValueParameterDescriptor parameterDescriptor, ValueArgument argument) {
        if (argument.getSpreadElement() != null) {
            if (parameterDescriptor.getVarargElementType() == null) {
                // Spread argument passed to a non-vararg parameter, an error is already reported by ValueArgumentsToParametersMapper
                return ErrorUtils.createErrorType("Don't care");
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

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeResultAndReportErrors(
            BindingTrace trace,
            TracingStrategy tracing,
            Set<ResolvedCallWithTrace<D>> successfulCandidates,
            Set<ResolvedCallWithTrace<D>> failedCandidates) {
        // TODO : maybe it's better to filter overrides out first, and only then look for the maximally specific

        if (successfulCandidates.size() > 0) {
            OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(successfulCandidates, true);
            if (results.isAmbiguity()) {
                // This check is needed for the following case:
                //    x.foo(unresolved) -- if there are multiple foo's, we'd report an ambiguity, and it does not make sense here
                if (allClean(results.getResultingCalls())) {
                    tracing.ambiguity(trace, results.getResultingCalls());
                }
                tracing.recordAmbiguity(trace, results.getResultingCalls());
            }
            return results;
        }
        else if (!failedCandidates.isEmpty()) {
            if (failedCandidates.size() != 1) {
                // This is needed when there are several overloads some of which are OK but for nullability of the receiver,
                // and some are not OK at all. In this case we'd like to say "unsafe call" rather than "none applicable"
                // Used to be: weak errors. Generalized for future extensions
                for (EnumSet<ResolutionStatus> severityLevel : SEVERITY_LEVELS) {
                    Set<ResolvedCallWithTrace<D>> thisLevel = Sets.newLinkedHashSet();
                    for (ResolvedCallWithTrace<D> candidate : failedCandidates) {
                        if (severityLevel.contains(candidate.getStatus())) {
                            thisLevel.add(candidate);
                        }
                    }
                    if (!thisLevel.isEmpty()) {
                        OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(thisLevel, false);
                        if (results.isSuccess()) {
                            results.getResultingCall().getTrace().commit();
                            return OverloadResolutionResultsImpl.singleFailedCandidate(results.getResultingCall());
                        }

                        tracing.noneApplicable(trace, results.getResultingCalls());
                        tracing.recordAmbiguity(trace, results.getResultingCalls());
                        return OverloadResolutionResultsImpl.manyFailedCandidates(results.getResultingCalls());
                    }
                }

                assert false : "Should not be reachable, cause every status must belong to some level";

                Set<ResolvedCallWithTrace<D>> noOverrides = OverridingUtil.filterOverrides(failedCandidates, MAP_TO_CANDIDATE);
                if (noOverrides.size() != 1) {
                    tracing.noneApplicable(trace, noOverrides);
                    tracing.recordAmbiguity(trace, noOverrides);
                    return OverloadResolutionResultsImpl.manyFailedCandidates(noOverrides);
                }

                failedCandidates = noOverrides;
            }

            ResolvedCallWithTrace<D> failed = failedCandidates.iterator().next();
            failed.getTrace().commit();
            return OverloadResolutionResultsImpl.singleFailedCandidate(failed);
        }
        else {
            tracing.unresolvedReference(trace);
            return OverloadResolutionResultsImpl.nameNotFound();
        }
    }

    private static <D extends CallableDescriptor> boolean allClean(Collection<ResolvedCallWithTrace<D>> results) {
        for (ResolvedCallWithTrace<D> result : results) {
            if (result.isDirty()) return false;
        }
        return true;
    }

    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> chooseAndReportMaximallySpecific(Set<ResolvedCallWithTrace<D>> candidates, boolean discriminateGenerics) {
        if (candidates.size() != 1) {
            Set<ResolvedCallWithTrace<D>> cleanCandidates = Sets.newLinkedHashSet(candidates);
            for (Iterator<ResolvedCallWithTrace<D>> iterator = cleanCandidates.iterator(); iterator.hasNext(); ) {
                ResolvedCallWithTrace<D> candidate = iterator.next();
                if (candidate.isDirty()) {
                    iterator.remove();
                }
            }

            if (cleanCandidates.isEmpty()) {
                cleanCandidates = candidates;
            }
            ResolvedCallWithTrace<D> maximallySpecific = overloadingConflictResolver.findMaximallySpecific(cleanCandidates, false);
            if (maximallySpecific != null) {
                return OverloadResolutionResultsImpl.success(maximallySpecific);
            }

            if (discriminateGenerics) {
                ResolvedCallWithTrace<D> maximallySpecificGenericsDiscriminated = overloadingConflictResolver.findMaximallySpecific(cleanCandidates, true);
                if (maximallySpecificGenericsDiscriminated != null) {
                    return OverloadResolutionResultsImpl.success(maximallySpecificGenericsDiscriminated);
                }
            }

            Set<ResolvedCallWithTrace<D>> noOverrides = OverridingUtil.filterOverrides(candidates, MAP_TO_RESULT);

            return OverloadResolutionResultsImpl.ambiguity(noOverrides);
        }
        else {
            ResolvedCallWithTrace<D> result = candidates.iterator().next();

            TemporaryBindingTrace temporaryTrace = result.getTrace();
            temporaryTrace.commit();

            return OverloadResolutionResultsImpl.success(result);
        }
    }

    private void checkGenericBoundsInAFunctionCall(List<JetTypeProjection> jetTypeArguments, List<JetType> typeArguments, CallableDescriptor functionDescriptor, BindingTrace trace) {
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
                descriptorResolver.checkBounds(typeReference, typeArgument, typeParameterDescriptor, substitutor, trace);
            }
        }
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveExactSignature(@NotNull JetScope scope, @NotNull ReceiverDescriptor receiver, @NotNull Name name, @NotNull List<JetType> parameterTypes) {
        List<ResolutionCandidate<FunctionDescriptor>> candidates = findCandidatesByExactSignature(scope, receiver, name, parameterTypes);

        BindingTraceContext trace = new BindingTraceContext();
        TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(trace);
        Set<ResolvedCallWithTrace<FunctionDescriptor>> calls = Sets.newLinkedHashSet();
        for (ResolutionCandidate<FunctionDescriptor> candidate : candidates) {
            ResolvedCallImpl<FunctionDescriptor> call = ResolvedCallImpl.create(candidate, temporaryBindingTrace);
            calls.add(call);
        }
        return computeResultAndReportErrors(trace, TracingStrategy.EMPTY, calls, Collections.<ResolvedCallWithTrace<FunctionDescriptor>>emptySet());
    }

    private List<ResolutionCandidate<FunctionDescriptor>> findCandidatesByExactSignature(JetScope scope, ReceiverDescriptor receiver,
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

    private boolean findExtensionFunctions(Collection<ResolutionCandidate<FunctionDescriptor>> candidates, ReceiverDescriptor receiver,
                                           List<JetType> parameterTypes, List<ResolutionCandidate<FunctionDescriptor>> result) {
        boolean found = false;
        for (ResolutionCandidate<FunctionDescriptor> candidate : candidates) {
            FunctionDescriptor functionDescriptor = candidate.getDescriptor();
            ReceiverDescriptor functionReceiver = functionDescriptor.getReceiverParameter();
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
