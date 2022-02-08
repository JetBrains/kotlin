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

package org.jetbrains.kotlin.resolve.calls.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus;
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.List;
import java.util.Map;

public abstract class DelegatingResolvedCall<D extends CallableDescriptor> implements ResolvedCall<D> {
    private final ResolvedCall<? extends D> resolvedCall;

    public DelegatingResolvedCall(@NotNull ResolvedCall<? extends D> resolvedCall) {
        this.resolvedCall = resolvedCall;
    }

    @NotNull
    @Override
    public ResolutionStatus getStatus() {
        return resolvedCall.getStatus();
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

    @Nullable
    @Override
    public ReceiverValue getExtensionReceiver() {
        return resolvedCall.getExtensionReceiver();
    }

    @Nullable
    @Override
    public ReceiverValue getDispatchReceiver() {
        return resolvedCall.getDispatchReceiver();
    }

    @NotNull
    @Override
    public List<ReceiverValue> getContextReceivers() {
        return resolvedCall.getContextReceivers();
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
    public Map<TypeParameterDescriptor, KotlinType> getTypeArguments() {
        return resolvedCall.getTypeArguments();
    }

    @NotNull
    @Override
    public DataFlowInfoForArguments getDataFlowInfoForArguments() {
        return resolvedCall.getDataFlowInfoForArguments();
    }

    @Nullable
    @Override
    public KotlinType getSmartCastDispatchReceiverType() {
        return resolvedCall.getSmartCastDispatchReceiverType();
    }
}
