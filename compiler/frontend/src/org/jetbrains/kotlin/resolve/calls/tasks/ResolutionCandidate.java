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

package org.jetbrains.kotlin.resolve.calls.tasks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.TypeSubstitutor;

public class ResolutionCandidate<D extends CallableDescriptor> {
    private final Call call;
    private final D candidateDescriptor;
    private final TypeSubstitutor knownTypeParametersResultingSubstitutor;
    private ReceiverValue dispatchReceiver; // receiver object of a method
    private ExplicitReceiverKind explicitReceiverKind;

    private ResolutionCandidate(
            @NotNull Call call, @NotNull D descriptor, @Nullable ReceiverValue dispatchReceiver,
            @NotNull ExplicitReceiverKind explicitReceiverKind,
            @Nullable TypeSubstitutor knownTypeParametersResultingSubstitutor
    ) {
        this.call = call;
        this.candidateDescriptor = descriptor;
        this.dispatchReceiver = dispatchReceiver;
        this.explicitReceiverKind = explicitReceiverKind;
        this.knownTypeParametersResultingSubstitutor = knownTypeParametersResultingSubstitutor;
    }

    public static <D extends CallableDescriptor> ResolutionCandidate<D> create(
            @NotNull Call call, @NotNull D descriptor
    ) {
        return new ResolutionCandidate<D>(call, descriptor, null, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, null);
    }

    public static <D extends CallableDescriptor> ResolutionCandidate<D> create(
            @NotNull Call call, @NotNull D descriptor, @Nullable TypeSubstitutor knownTypeParametersResultingSubstitutor
    ) {
        return new ResolutionCandidate<D>(call, descriptor,
                                          null, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                                          knownTypeParametersResultingSubstitutor);
    }

    public static <D extends CallableDescriptor> ResolutionCandidate<D> create(
            @NotNull Call call, @NotNull D descriptor, @Nullable ReceiverValue dispatchReceiver,
            @NotNull ExplicitReceiverKind explicitReceiverKind,
            @Nullable TypeSubstitutor knownTypeParametersResultingSubstitutor
    ) {
        return new ResolutionCandidate<D>(call, descriptor, dispatchReceiver, explicitReceiverKind,
                                          knownTypeParametersResultingSubstitutor);
    }

    public void setDispatchReceiver(@Nullable ReceiverValue dispatchReceiver) {
        this.dispatchReceiver = dispatchReceiver;
    }

    public void setExplicitReceiverKind(@NotNull ExplicitReceiverKind explicitReceiverKind) {
        this.explicitReceiverKind = explicitReceiverKind;
    }

    @NotNull
    public Call getCall() {
        return call;
    }

    @NotNull
    public D getDescriptor() {
        return candidateDescriptor;
    }

    @Nullable
    public ReceiverValue getDispatchReceiver() {
        return dispatchReceiver;
    }

    @NotNull
    public ExplicitReceiverKind getExplicitReceiverKind() {
        return explicitReceiverKind;
    }

    @Nullable
    public TypeSubstitutor getKnownTypeParametersResultingSubstitutor() {
        return knownTypeParametersResultingSubstitutor;
    }

    @Override
    public String toString() {
        return candidateDescriptor.toString();
    }
}
