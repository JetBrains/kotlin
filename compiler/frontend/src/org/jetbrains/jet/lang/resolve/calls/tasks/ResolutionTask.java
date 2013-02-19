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
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.CallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.ExpressionPosition;
import org.jetbrains.jet.lang.resolve.calls.context.ResolveMode;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

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
            @NotNull Collection<ResolutionCandidate<D>> candidates, @NotNull JetReferenceExpression reference, @NotNull TracingStrategy tracing,
            BindingTrace trace, JetScope scope, Call call, JetType expectedType, DataFlowInfo dataFlowInfo, ResolveMode resolveMode, ExpressionPosition expressionPosition) {
        super(trace, scope, call, expectedType, dataFlowInfo, resolveMode, expressionPosition);
        this.candidates = candidates;
        this.reference = reference;
        this.tracing = tracing;
    }

    public ResolutionTask(@NotNull Collection<ResolutionCandidate<D>> candidates, @NotNull JetReferenceExpression reference, @NotNull BasicCallResolutionContext context) {
        this(candidates, reference, TracingStrategyImpl.create(reference, context.call), context.trace, context.scope, context.call, context.expectedType, context.dataFlowInfo, context.resolveMode, context.expressionPosition);
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
            ExpressionPosition expressionPosition
    ) {
        ResolutionTask<D, F> newTask = new ResolutionTask<D, F>(candidates, reference, tracing, trace, scope, call, expectedType, dataFlowInfo, resolveMode, expressionPosition);
        newTask.setCheckingStrategy(checkingStrategy);
        return newTask;
    }

    @Override
    protected ResolutionTask<D, F> self() {
        return this;
    }

    public interface DescriptorCheckStrategy {
        <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing);
    }
}
