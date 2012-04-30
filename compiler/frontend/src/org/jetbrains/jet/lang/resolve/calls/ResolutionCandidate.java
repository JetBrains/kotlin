/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author svtk
 */
public class ResolutionCandidate<D extends CallableDescriptor> {
    private final D candidateDescriptor;
    private ReceiverDescriptor thisObject; // receiver object of a method
    private ReceiverDescriptor receiverArgument; // receiver of an extension function

    private ResolutionCandidate(@NotNull D descriptor, @NotNull ReceiverDescriptor thisObject, @NotNull ReceiverDescriptor receiverArgument) {
        this.candidateDescriptor = descriptor;
        this.thisObject = thisObject;
        this.receiverArgument = receiverArgument;
    }

    public static <D extends CallableDescriptor> ResolutionCandidate<D> create(@NotNull D descriptor) {
        return new ResolutionCandidate<D>(descriptor, NO_RECEIVER, NO_RECEIVER);
    }

    public static <D extends CallableDescriptor> ResolutionCandidate<D> create(@NotNull D descriptor, @NotNull ReceiverDescriptor thisObject, @NotNull ReceiverDescriptor receiverArgument) {
        return new ResolutionCandidate<D>(descriptor, thisObject, receiverArgument);
    }

    public void setThisObject(@NotNull ReceiverDescriptor thisObject) {
        this.thisObject = thisObject;
    }

    public void setReceiverArgument(@NotNull ReceiverDescriptor receiverArgument) {
        this.receiverArgument = receiverArgument;
    }

    @NotNull
    public D getDescriptor() {
        return candidateDescriptor;
    }

    @NotNull
    public ReceiverDescriptor getThisObject() {
        return thisObject;
    }

    @NotNull
    public ReceiverDescriptor getReceiverArgument() {
        return receiverArgument;
    }
    
    @NotNull
    public static <D extends CallableDescriptor> List<ResolutionCandidate<D>> convertCollection(@NotNull Collection<? extends D> descriptors) {
        List<ResolutionCandidate<D>> result = Lists.newArrayList();
        for (D descriptor : descriptors) {
            result.add(create(descriptor));
        }
        return result;
    }
    
}
