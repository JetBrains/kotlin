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

package org.jetbrains.kotlin.resolve.calls.tasks;

import com.google.common.collect.Lists;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.JetReferenceExpression;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.context.*;
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.JetType;

import java.util.Collection;

/**
 * Stores candidates for call resolution.
 */
public class ResolutionTask<D extends CallableDescriptor, F extends D> extends CallResolutionContext<ResolutionTask<D, F>> {
    private final Function0<Collection<ResolutionCandidate<D>>> lazyCandidates;
    private final Collection<MutableResolvedCall<F>> resolvedCalls;
    public final TracingStrategy tracing;

    private ResolutionTask(
            @NotNull Function0<Collection<ResolutionCandidate<D>>> lazyCandidates,
            @NotNull TracingStrategy tracing,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull Call call,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckValueArgumentsMode checkArguments,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments,
            @NotNull CallChecker callChecker,
            @NotNull Collection<MutableResolvedCall<F>> resolvedCalls,
            boolean isAnnotationContext,
            boolean collectAllCandidates
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
              dataFlowInfoForArguments, callChecker, isAnnotationContext, collectAllCandidates);
        this.lazyCandidates = lazyCandidates;
        this.resolvedCalls = resolvedCalls;
        this.tracing = tracing;
    }

    public ResolutionTask(
            @NotNull BasicCallResolutionContext context,
            @NotNull TracingStrategy tracing,
            @NotNull Function0<Collection<ResolutionCandidate<D>>> lazyCandidates
    ) {
        this(lazyCandidates, tracing,
             context.trace, context.scope, context.call,
             context.expectedType, context.dataFlowInfo, context.contextDependency, context.checkArguments,
             context.resolutionResultsCache, context.dataFlowInfoForArguments,
             context.callChecker, Lists.<MutableResolvedCall<F>>newArrayList(), context.isAnnotationContext, context.collectAllCandidates);
    }

    public ResolutionTask(
            @NotNull final Collection<ResolutionCandidate<D>> candidates,
            @NotNull JetReferenceExpression reference,
            @NotNull BasicCallResolutionContext context
    ) {
        this(context, TracingStrategyImpl.create(reference, context.call), new Function0<Collection<ResolutionCandidate<D>>>() {
            @Override
            public Collection<ResolutionCandidate<D>> invoke() {
                return candidates;
            }
        });
    }

    @NotNull
    public Collection<ResolutionCandidate<D>> getCandidates() {
        return lazyCandidates.invoke();
    }

    public void addResolvedCall(@NotNull MutableResolvedCall<F> resolvedCall) {
        resolvedCalls.add(resolvedCall);
    }

    @NotNull
    public Collection<MutableResolvedCall<F>> getResolvedCalls() {
        return resolvedCalls;
    }

    @Override
    protected ResolutionTask<D, F> create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            boolean collectAllCandidates
    ) {
        return new ResolutionTask<D, F>(
                lazyCandidates, tracing, trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                resolutionResultsCache, dataFlowInfoForArguments, callChecker, resolvedCalls, isAnnotationContext,
                collectAllCandidates);
    }

    public ResolutionTask<D, F> replaceContext(@NotNull BasicCallResolutionContext newContext) {
        return new ResolutionTask<D, F>(newContext, tracing, lazyCandidates);
    }

    public ResolutionTask<D, F> replaceCall(@NotNull Call newCall) {
        return new ResolutionTask<D, F>(
                lazyCandidates, tracing, trace, scope, newCall, expectedType, dataFlowInfo, contextDependency, checkArguments,
                resolutionResultsCache, dataFlowInfoForArguments, callChecker, resolvedCalls,
                isAnnotationContext, collectAllCandidates);
    }

    public interface DescriptorCheckStrategy {
        <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing);
    }

    @Override
    public String toString() {
        return lazyCandidates.toString();
    }
}
