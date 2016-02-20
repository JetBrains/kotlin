/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.types.KotlinType;

public final class CallCandidateResolutionContext<D extends CallableDescriptor> extends CallResolutionContext<CallCandidateResolutionContext<D>> {
    @NotNull
    public final MutableResolvedCall<D> candidateCall;
    @NotNull
    public final TracingStrategy tracing;
    @NotNull
    public final CandidateResolveMode candidateResolveMode;

    private CallCandidateResolutionContext(
            @NotNull MutableResolvedCall<D> candidateCall,
            @NotNull TracingStrategy tracing,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull Call call,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckArgumentTypesMode checkArguments,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments,
            @NotNull StatementFilter statementFilter,
            @NotNull CandidateResolveMode candidateResolveMode,
            boolean isAnnotationContext,
            boolean isDebuggerContext,
            boolean collectAllCandidates,
            @NotNull CallPosition callPosition
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
              dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext,
              collectAllCandidates, callPosition);
        this.candidateCall = candidateCall;
        this.tracing = tracing;
        this.candidateResolveMode = candidateResolveMode;
    }

    public static <D extends CallableDescriptor> CallCandidateResolutionContext<D> create(
            @NotNull MutableResolvedCall<D> candidateCall, @NotNull CallResolutionContext<?> context, @NotNull BindingTrace trace,
            @NotNull TracingStrategy tracing, @NotNull Call call,
            @NotNull CandidateResolveMode candidateResolveMode
    ) {
        return new CallCandidateResolutionContext<D>(
                candidateCall, tracing, trace, context.scope, call, context.expectedType,
                context.dataFlowInfo, context.contextDependency, context.checkArguments,
                context.resolutionResultsCache, context.dataFlowInfoForArguments,
                context.statementFilter,
                candidateResolveMode, context.isAnnotationContext, context.isDebuggerContext, context.collectAllCandidates, context.callPosition);
    }

    @NotNull
    public static <D extends CallableDescriptor> CallCandidateResolutionContext<D> createForCallBeingAnalyzed(
            @NotNull MutableResolvedCall<D> candidateCall, @NotNull BasicCallResolutionContext context, @NotNull TracingStrategy tracing
    ) {
        return new CallCandidateResolutionContext<D>(
                candidateCall, tracing, context.trace, context.scope, context.call, context.expectedType,
                context.dataFlowInfo, context.contextDependency, context.checkArguments, context.resolutionResultsCache,
                context.dataFlowInfoForArguments, context.statementFilter,
                CandidateResolveMode.FULLY, context.isAnnotationContext, context.isDebuggerContext, context.collectAllCandidates,
                context.callPosition);
    }

    @Override
    protected CallCandidateResolutionContext<D> create(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KotlinType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull StatementFilter statementFilter,
            boolean collectAllCandidates,
            @NotNull CallPosition callPosition
    ) {
        return new CallCandidateResolutionContext<D>(
                candidateCall, tracing, trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                resolutionResultsCache, dataFlowInfoForArguments, statementFilter,
                candidateResolveMode, isAnnotationContext, isDebuggerContext, collectAllCandidates, callPosition);
    }
}
