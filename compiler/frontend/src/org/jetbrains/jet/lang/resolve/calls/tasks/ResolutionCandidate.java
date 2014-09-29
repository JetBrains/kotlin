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
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;

public class ResolutionCandidate<D extends CallableDescriptor> {
    private final Call call;
    private final D candidateDescriptor;
    private ReceiverValue dispatchReceiver; // receiver object of a method
    private ReceiverValue extensionReceiver; // receiver of an extension function
    private ExplicitReceiverKind explicitReceiverKind;
    private Boolean isSafeCall;

    private ResolutionCandidate(
            @NotNull Call call, @NotNull D descriptor, @NotNull ReceiverValue dispatchReceiver,
            @NotNull ReceiverValue extensionReceiver, @NotNull ExplicitReceiverKind explicitReceiverKind, @Nullable Boolean isSafeCall
    ) {
        this.call = call;
        this.candidateDescriptor = descriptor;
        this.dispatchReceiver = dispatchReceiver;
        this.extensionReceiver = extensionReceiver;
        this.explicitReceiverKind = explicitReceiverKind;
        this.isSafeCall = isSafeCall;
    }

    /*package*/ static <D extends CallableDescriptor> ResolutionCandidate<D> create(@NotNull Call call, @NotNull D descriptor) {
        return new ResolutionCandidate<D>(call, descriptor, NO_RECEIVER, NO_RECEIVER, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, null);
    }

    /* 'null' for isSafeCall parameter if it should be set later (with 'setSafeCall') */
    public static <D extends CallableDescriptor> ResolutionCandidate<D> create(
            @NotNull Call call, @NotNull D descriptor, @Nullable Boolean isSafeCall
    ) {
        return new ResolutionCandidate<D>(call, descriptor, NO_RECEIVER, NO_RECEIVER, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, isSafeCall);
    }

    public static <D extends CallableDescriptor> ResolutionCandidate<D> create(
            @NotNull Call call, @NotNull D descriptor, @NotNull ReceiverValue dispatchReceiver,
            @NotNull ReceiverValue receiverArgument, @NotNull ExplicitReceiverKind explicitReceiverKind, boolean isSafeCall
    ) {
        return new ResolutionCandidate<D>(call, descriptor, dispatchReceiver, receiverArgument, explicitReceiverKind, isSafeCall);
    }

    public void setDispatchReceiver(@NotNull ReceiverValue dispatchReceiver) {
        this.dispatchReceiver = dispatchReceiver;
    }

    public void setExtensionReceiver(@NotNull ReceiverValue extensionReceiver) {
        this.extensionReceiver = extensionReceiver;
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

    @NotNull
    public ReceiverValue getDispatchReceiver() {
        return dispatchReceiver;
    }

    @NotNull
    public ReceiverValue getExtensionReceiver() {
        return extensionReceiver;
    }

    @NotNull
    public ExplicitReceiverKind getExplicitReceiverKind() {
        return explicitReceiverKind;
    }

    @NotNull
    public static <D extends CallableDescriptor> List<ResolutionCandidate<D>> convertCollection(
            @NotNull Call call, @NotNull Collection<? extends D> descriptors, boolean isSafeCall
    ) {
        List<ResolutionCandidate<D>> result = Lists.newArrayList();
        for (D descriptor : descriptors) {
            result.add(create(call, descriptor, isSafeCall));
        }
        return result;
    }

    public void setSafeCall(boolean safeCall) {
        assert isSafeCall == null;
        isSafeCall = safeCall;
    }

    public boolean isSafeCall() {
        assert isSafeCall != null;
        return isSafeCall;
    }

    @Override
    public String toString() {
        return candidateDescriptor.toString();
    }
}
