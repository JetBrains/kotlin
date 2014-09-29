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

package org.jetbrains.jet.lang.resolve.calls.tasks;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.CallResolverExtension;
import org.jetbrains.jet.lang.resolve.calls.context.*;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

/**
 * Stores candidates for call resolution.
 */
public class ResolutionTask<D extends CallableDescriptor, F extends D> extends CallResolutionContext<ResolutionTask<D, F>> {
    private final Collection<ResolutionCandidate<D>> candidates;
    private final Collection<MutableResolvedCall<F>> resolvedCalls;
    private DescriptorCheckStrategy checkingStrategy;
    public final TracingStrategy tracing;

    private ResolutionTask(
            @NotNull Collection<ResolutionCandidate<D>> candidates,
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
            @NotNull CallResolverExtension callResolverExtension,
            @NotNull Collection<MutableResolvedCall<F>> resolvedCalls,
            boolean isAnnotationContext,
            boolean collectAllCandidates
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
              dataFlowInfoForArguments, callResolverExtension, isAnnotationContext, collectAllCandidates);
        this.candidates = candidates;
        this.resolvedCalls = resolvedCalls;
        this.tracing = tracing;
    }

    public ResolutionTask(
            @NotNull Collection<ResolutionCandidate<D>> candidates,
            @NotNull BasicCallResolutionContext context,
            @NotNull TracingStrategy tracing
    ) {
        this(candidates, tracing,
             context.trace, context.scope, context.call,
             context.expectedType, context.dataFlowInfo, context.contextDependency, context.checkArguments,
             context.resolutionResultsCache, context.dataFlowInfoForArguments,
             context.callResolverExtension, Lists.<MutableResolvedCall<F>>newArrayList(), context.isAnnotationContext, context.collectAllCandidates);
    }

    public ResolutionTask(
            @NotNull Collection<ResolutionCandidate<D>> candidates,
            @NotNull JetReferenceExpression reference,
            @NotNull BasicCallResolutionContext context
    ) {
        this(candidates, context, TracingStrategyImpl.create(reference, context.call));
    }

    @NotNull
    public Collection<ResolutionCandidate<D>> getCandidates() {
        return candidates;
    }

    public void addResolvedCall(@NotNull MutableResolvedCall<F> resolvedCall) {
        resolvedCalls.add(resolvedCall);
    }

    @NotNull
    public Collection<MutableResolvedCall<F>> getResolvedCalls() {
        return resolvedCalls;
    }

    public void setCheckingStrategy(DescriptorCheckStrategy strategy) {
        checkingStrategy = strategy;
    }

    public boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing) {
        if (checkingStrategy != null && !checkingStrategy.performAdvancedChecks(descriptor, trace, tracing)) {
            return false;
        }
        return true;
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
        ResolutionTask<D, F> newTask = new ResolutionTask<D, F>(
                candidates, tracing, trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                resolutionResultsCache, dataFlowInfoForArguments, callResolverExtension, resolvedCalls, isAnnotationContext,
                collectAllCandidates);
        newTask.setCheckingStrategy(checkingStrategy);
        return newTask;
    }

    public ResolutionTask<D, F> replaceCall(@NotNull Call newCall) {
        return new ResolutionTask<D, F>(
                candidates, tracing, trace, scope, newCall, expectedType, dataFlowInfo, contextDependency, checkArguments,
                resolutionResultsCache, dataFlowInfoForArguments, callResolverExtension, resolvedCalls,
                isAnnotationContext, collectAllCandidates);
    }

    public interface DescriptorCheckStrategy {
        <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing);
    }

    @Override
    public String toString() {
        return candidates.toString();
    }
}
