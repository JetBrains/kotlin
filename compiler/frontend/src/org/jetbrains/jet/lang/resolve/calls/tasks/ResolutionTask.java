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

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.CallResolverExtension;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.context.*;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.LabelResolver;

import java.util.Collection;
import java.util.Set;

/**
 * Stores candidates for call resolution.
 */
public class ResolutionTask<D extends CallableDescriptor, F extends D> extends CallResolutionContext<ResolutionTask<D, F>> {
    private final Collection<ResolutionCandidate<D>> candidates;
    private final Set<ResolvedCallWithTrace<F>> resolvedCalls = Sets.newLinkedHashSet();
    private DescriptorCheckStrategy checkingStrategy;
    public final JetReferenceExpression reference;
    public final TracingStrategy tracing;

    public ResolutionTask(
            @NotNull Collection<ResolutionCandidate<D>> candidates, @NotNull JetReferenceExpression reference,
            @NotNull TracingStrategy tracing, BindingTrace trace, JetScope scope, Call call, JetType expectedType,
            DataFlowInfo dataFlowInfo, ContextDependency contextDependency, CheckValueArgumentsMode checkArguments,
            ExpressionPosition expressionPosition, ResolutionResultsCache resolutionResultsCache,
            @NotNull LabelResolver labelResolver, @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments,
            @NotNull CallResolverExtension callResolverExtension
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, expressionPosition, resolutionResultsCache,
              labelResolver, dataFlowInfoForArguments, callResolverExtension);
        this.candidates = candidates;
        this.reference = reference;
        this.tracing = tracing;
    }

    public ResolutionTask(
            @NotNull Collection<ResolutionCandidate<D>> candidates,
            @NotNull JetReferenceExpression reference,
            @NotNull BasicCallResolutionContext context,
            @Nullable TracingStrategy tracing
    ) {
        this(candidates, reference, tracing != null ? tracing : TracingStrategyImpl.create(reference, context.call),
             context.trace, context.scope, context.call,
             context.expectedType, context.dataFlowInfo, context.contextDependency, context.checkArguments,
             context.expressionPosition, context.resolutionResultsCache, context.labelResolver, context.dataFlowInfoForArguments, context.callResolverExtension);
    }

    public ResolutionTask(
            @NotNull Collection<ResolutionCandidate<D>> candidates,
            @NotNull JetReferenceExpression reference,
            @NotNull BasicCallResolutionContext context
    ) {
        this(candidates, reference, context, null);
    }

    @NotNull
    public Collection<ResolutionCandidate<D>> getCandidates() {
        return candidates;
    }

    @NotNull
    public Set<ResolvedCallWithTrace<F>> getResolvedCalls() {
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
            @NotNull ExpressionPosition expressionPosition,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull LabelResolver labelResolver
    ) {
        ResolutionTask<D, F> newTask = new ResolutionTask<D, F>(
                candidates, reference, tracing, trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                expressionPosition, resolutionResultsCache, labelResolver, dataFlowInfoForArguments, callResolverExtension);
        newTask.setCheckingStrategy(checkingStrategy);
        return newTask;
    }

    @Override
    protected ResolutionTask<D, F> self() {
        return this;
    }

    public ResolutionTask<D, F> replaceCall(@NotNull Call newCall) {
        return new ResolutionTask<D, F>(
                candidates, reference, tracing, trace, scope, newCall, expectedType, dataFlowInfo, contextDependency, checkArguments,
                expressionPosition, resolutionResultsCache, labelResolver, dataFlowInfoForArguments, callResolverExtension);
    }

    public interface DescriptorCheckStrategy {
        <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing);
    }

    @Override
    public String toString() {
        return candidates.toString();
    }
}
