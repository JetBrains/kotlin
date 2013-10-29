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

package org.jetbrains.jet.lang.resolve.calls.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.CallResolverExtension;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.LabelResolver;

public final class CallCandidateResolutionContext<D extends CallableDescriptor> extends CallResolutionContext<CallCandidateResolutionContext<D>> {
    public final ResolvedCallImpl<D> candidateCall;
    public final TracingStrategy tracing;
    public ReceiverValue receiverForVariableAsFunctionSecondCall = ReceiverValue.NO_RECEIVER;

    private CallCandidateResolutionContext(
            @NotNull ResolvedCallImpl<D> candidateCall,
            @NotNull TracingStrategy tracing,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull Call call,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckValueArgumentsMode checkArguments,
            @NotNull ExpressionPosition expressionPosition,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull LabelResolver labelResolver,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments,
            @NotNull CallResolverExtension callResolverExtension
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, expressionPosition, resolutionResultsCache,
              labelResolver, dataFlowInfoForArguments, callResolverExtension);
        this.candidateCall = candidateCall;
        this.tracing = tracing;
    }

    public static <D extends CallableDescriptor> CallCandidateResolutionContext<D> create(
            @NotNull ResolvedCallImpl<D> candidateCall, @NotNull CallResolutionContext<?> context, @NotNull BindingTrace trace,
            @NotNull TracingStrategy tracing, @NotNull Call call) {
        candidateCall.setInitialDataFlowInfo(context.dataFlowInfo);
        return new CallCandidateResolutionContext<D>(
                candidateCall, tracing, trace, context.scope, call, context.expectedType,
                context.dataFlowInfo, context.contextDependency, context.checkArguments,
                context.expressionPosition, context.resolutionResultsCache, context.labelResolver, context.dataFlowInfoForArguments, context.callResolverExtension);
    }

    public static <D extends CallableDescriptor> CallCandidateResolutionContext<D> create(
            @NotNull ResolvedCallImpl<D> candidateCall, @NotNull CallResolutionContext<?> context, @NotNull BindingTrace trace,
            @NotNull TracingStrategy tracing) {
        return create(candidateCall, context, trace, tracing, context.call);
    }

    public static <D extends CallableDescriptor> CallCandidateResolutionContext<D> createForCallBeingAnalyzed(
            @NotNull ResolvedCallImpl<D> candidateCall, @NotNull BasicCallResolutionContext context, @NotNull TracingStrategy tracing
    ) {
        return new CallCandidateResolutionContext<D>(
                candidateCall, tracing, context.trace, context.scope, context.call, context.expectedType,
                context.dataFlowInfo, context.contextDependency, context.checkArguments, context.expressionPosition,
                context.resolutionResultsCache, context.labelResolver, context.dataFlowInfoForArguments, context.callResolverExtension);
    }

    @Override
    protected CallCandidateResolutionContext<D> create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull ExpressionPosition expressionPosition,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull LabelResolver labelResolver
    ) {
        return new CallCandidateResolutionContext<D>(
                candidateCall, tracing, trace, scope, call, expectedType, dataFlowInfo, contextDependency,
                checkArguments, expressionPosition, resolutionResultsCache, labelResolver, dataFlowInfoForArguments, callResolverExtension);
    }

    @Override
    protected CallCandidateResolutionContext<D> self() {
        return this;
    }
}
