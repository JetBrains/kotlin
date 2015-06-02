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
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker;
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker;
import org.jetbrains.kotlin.resolve.calls.context.*;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator;
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
            @NotNull SymbolUsageValidator symbolUsageValidator,
            @NotNull AdditionalTypeChecker additionalTypeChecker,
            @NotNull StatementFilter statementFilter,
            @NotNull Collection<MutableResolvedCall<F>> resolvedCalls,
            boolean isAnnotationContext,
            boolean collectAllCandidates,
            boolean insideSafeCallChain
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
              dataFlowInfoForArguments, callChecker, symbolUsageValidator, additionalTypeChecker, statementFilter, isAnnotationContext, collectAllCandidates, insideSafeCallChain);
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
             context.callChecker, context.symbolUsageValidator, context.additionalTypeChecker,
             context.statementFilter, Lists.<MutableResolvedCall<F>>newArrayList(),
             context.isAnnotationContext, context.collectAllCandidates, context.insideCallChain);
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
            @NotNull StatementFilter statementFilter,
            boolean collectAllCandidates,
            boolean insideSafeCallChain
    ) {
        return new ResolutionTask<D, F>(
                lazyCandidates, tracing, trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                resolutionResultsCache, dataFlowInfoForArguments,
                callChecker, symbolUsageValidator, additionalTypeChecker,
                statementFilter, resolvedCalls,
                isAnnotationContext, collectAllCandidates, insideSafeCallChain);
    }

    @Override
    public String toString() {
        return lazyCandidates.toString();
    }
}
