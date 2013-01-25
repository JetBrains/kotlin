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
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.jet.lang.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;

public final class CallCandidateResolutionContext<D extends CallableDescriptor, F extends D> extends CallResolutionContext<CallCandidateResolutionContext<D, F>> {
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
            boolean namespacesAllowed
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, namespacesAllowed);
        this.candidateCall = candidateCall;
        this.tracing = tracing;
        this.candidateCall.setInitialDataFlowInfo(dataFlowInfo);
    }

    public static <D extends CallableDescriptor, F extends D> CallCandidateResolutionContext<D, F> create(
            @NotNull ResolvedCallImpl<D> candidateCall, @NotNull ResolutionTask<D, F> task, @NotNull BindingTrace trace,
            @NotNull TracingStrategy tracing, @NotNull Call call) {
        return new CallCandidateResolutionContext<D, F>(candidateCall, tracing, trace, task.scope, call, task.expectedType, task.dataFlowInfo, task.namespacesAllowed);
    }

    public static <D extends CallableDescriptor, F extends D> CallCandidateResolutionContext<D, F> create(
            @NotNull ResolvedCallImpl<D> candidateCall, @NotNull ResolutionTask<D, F> task, @NotNull BindingTrace trace,
            @NotNull TracingStrategy tracing) {
        return create(candidateCall, task, trace, tracing, task.call);
    }

    public static <D extends CallableDescriptor> CallCandidateResolutionContext<D, D> create(
            @NotNull BasicCallResolutionContext context, @NotNull TracingStrategy tracing,
            @NotNull ResolvedCallImpl<D> candidateCall) {
        return new CallCandidateResolutionContext<D, D>(candidateCall, tracing, context.trace, context.scope, context.call, context.expectedType, context.dataFlowInfo, context.namespacesAllowed);
    }

    @Override
    protected CallCandidateResolutionContext<D, F> replace(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            boolean namespacesAllowed
    ) {
        return new CallCandidateResolutionContext<D, F>(candidateCall, tracing, trace, scope, call, expectedType, dataFlowInfo, namespacesAllowed);
    }

    @Override
    protected CallCandidateResolutionContext<D, F> self() {
        return this;
    }
}
