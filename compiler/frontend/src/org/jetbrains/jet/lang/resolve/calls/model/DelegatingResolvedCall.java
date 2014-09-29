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

package org.jetbrains.jet.lang.resolve.calls.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;
import java.util.Map;

public abstract class DelegatingResolvedCall<D extends CallableDescriptor> implements ResolvedCall<D> {
    private final ResolvedCall<? extends D> resolvedCall;

    public DelegatingResolvedCall(@NotNull ResolvedCall<? extends D> resolvedCall) {
        this.resolvedCall = resolvedCall;
    }

    @NotNull
    @Override
    public Call getCall() {
        return resolvedCall.getCall();
    }

    @NotNull
    @Override
    public D getCandidateDescriptor() {
        return resolvedCall.getCandidateDescriptor();
    }

    @NotNull
    @Override
    public D getResultingDescriptor() {
        return resolvedCall.getResultingDescriptor();
    }

    @NotNull
    @Override
    public ReceiverValue getExtensionReceiver() {
        return resolvedCall.getExtensionReceiver();
    }

    @NotNull
    @Override
    public ReceiverValue getDispatchReceiver() {
        return resolvedCall.getDispatchReceiver();
    }

    @NotNull
    @Override
    public ExplicitReceiverKind getExplicitReceiverKind() {
        return resolvedCall.getExplicitReceiverKind();
    }

    @NotNull
    @Override
    public Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments() {
        return resolvedCall.getValueArguments();
    }

    @NotNull
    @Override
    public ArgumentMapping getArgumentMapping(@NotNull ValueArgument valueArgument) {
        return resolvedCall.getArgumentMapping(valueArgument);
    }

    @Nullable
    @Override
    public List<ResolvedValueArgument> getValueArgumentsByIndex() {
        return resolvedCall.getValueArgumentsByIndex();
    }

    @NotNull
    @Override
    public Map<TypeParameterDescriptor, JetType> getTypeArguments() {
        return resolvedCall.getTypeArguments();
    }

    @NotNull
    @Override
    public DataFlowInfoForArguments getDataFlowInfoForArguments() {
        return resolvedCall.getDataFlowInfoForArguments();
    }

    @Override
    public boolean isSafeCall() {
        return resolvedCall.isSafeCall();
    }
}
