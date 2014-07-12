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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorWithVisibility;
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetFunctionLiteralArgument;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.List;

public interface TracingStrategy {
    TracingStrategy EMPTY = new TracingStrategy() {

        @Override
        public void bindCall(@NotNull BindingTrace trace, @NotNull Call call) {}

        @Override
        public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall) {}

        @Override
        public <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall) {}

        @Override
        public void unresolvedReference(@NotNull BindingTrace trace) {}

        @Override
        public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates) {}

        @Override
        public <D extends CallableDescriptor> void recordAmbiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates) {}

        @Override
        public void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor expectedReceiver) {}

        @Override
        public void wrongReceiverType(@NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor receiverParameter, @NotNull ReceiverValue receiverArgument) {}

        @Override
        public void noReceiverAllowed(@NotNull BindingTrace trace) {}

        @Override
        public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {}

        @Override
        public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount) {}

        @Override
        public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors) {}

        @Override
        public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors) {}

        @Override
        public <D extends CallableDescriptor> void cannotCompleteResolve(
                @NotNull BindingTrace trace,
                @NotNull Collection<? extends ResolvedCall<D>> descriptors
        ) {}

        @Override
        public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {}

        @Override
        public void unsafeCall(@NotNull BindingTrace trace, @NotNull JetType type, boolean isCallForImplicitInvoke) {}

        @Override
        public void unnecessarySafeCall(@NotNull BindingTrace trace, @NotNull JetType type) {}

        @Override
        public void danglingFunctionLiteralArgumentSuspected(@NotNull BindingTrace trace, @NotNull List<JetFunctionLiteralArgument> functionLiteralArguments) {}

        @Override
        public void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor) {}

        @Override
        public void typeInferenceFailed(@NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData) {}
    };

    void bindCall(@NotNull BindingTrace trace, @NotNull Call call);

    <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall);

    <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall);

    void unresolvedReference(@NotNull BindingTrace trace);

    <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates);

    <D extends CallableDescriptor> void recordAmbiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates);

    void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor expectedReceiver);

    void wrongReceiverType(@NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor receiverParameter, @NotNull ReceiverValue receiverArgument);

    void noReceiverAllowed(@NotNull BindingTrace trace);

    void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter);

    void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount);

    <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors);

    <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors);

    <D extends CallableDescriptor> void cannotCompleteResolve(
            @NotNull BindingTrace trace,
            @NotNull Collection<? extends ResolvedCall<D>> descriptors
    );

    void instantiationOfAbstractClass(@NotNull BindingTrace trace);

    void unsafeCall(@NotNull BindingTrace trace, @NotNull JetType type, boolean isCallForImplicitInvoke);

    void unnecessarySafeCall(@NotNull BindingTrace trace, @NotNull JetType type);

    void danglingFunctionLiteralArgumentSuspected(@NotNull BindingTrace trace, @NotNull List<JetFunctionLiteralArgument> functionLiteralArguments);

    void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor);

    void typeInferenceFailed(@NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData);
}
