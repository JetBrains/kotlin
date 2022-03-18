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
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;

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
        public void wrongReceiverType(
                @NotNull BindingTrace trace,
                @NotNull ReceiverParameterDescriptor receiverParameter,
                @NotNull ReceiverValue receiverArgument,
                @NotNull ResolutionContext<?> c
        ) {}

        @Override
        public void noReceiverAllowed(@NotNull BindingTrace trace) {}

        @Override
        public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {}

        @Override
        public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount, @NotNull CallableDescriptor descriptor) {}

        @Override
        public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> resolvedCalls) {}

        @Override
        public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors) {}

        @Override
        public <D extends CallableDescriptor> void cannotCompleteResolve(
                @NotNull BindingTrace trace,
                @NotNull Collection<? extends ResolvedCall<D>> descriptors
        ) {}

        @Override
        public void recursiveType(@NotNull BindingTrace trace, boolean shouldReportErrorsOnRecursiveTypeInsidePlusAssignment) {}

        @Override
        public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {}

        @Override
        public void abstractSuperCall(@NotNull BindingTrace trace) {}

        @Override
        public void nestedClassAccessViaInstanceReference(
                @NotNull BindingTrace trace,
                @NotNull ClassDescriptor classDescriptor,
                @NotNull ExplicitReceiverKind explicitReceiverKind
        ) {}

        @Override
        public void unsafeCall(@NotNull BindingTrace trace, @NotNull KotlinType type, boolean isCallForImplicitInvoke) {}

        @Override
        public void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor) {}

        @Override
        public void typeInferenceFailed(@NotNull ResolutionContext<?> context, @NotNull InferenceErrorData inferenceErrorData) {}
    };

    void bindCall(@NotNull BindingTrace trace, @NotNull Call call);

    <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall);

    <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall);

    void unresolvedReference(@NotNull BindingTrace trace);

    <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates);

    <D extends CallableDescriptor> void recordAmbiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates);

    void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor expectedReceiver);

    void wrongReceiverType(
            @NotNull BindingTrace trace,
            @NotNull ReceiverParameterDescriptor receiverParameter,
            @NotNull ReceiverValue receiverArgument,
            @NotNull ResolutionContext<?> c
    );

    void noReceiverAllowed(@NotNull BindingTrace trace);

    void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter);

    void wrongNumberOfTypeArguments(
            @NotNull BindingTrace trace,
            int expectedTypeArgumentCount,
            @NotNull CallableDescriptor descriptor
    );

    <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> resolvedCalls);

    <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors);

    <D extends CallableDescriptor> void cannotCompleteResolve(
            @NotNull BindingTrace trace,
            @NotNull Collection<? extends ResolvedCall<D>> descriptors
    );

    void recursiveType(@NotNull BindingTrace trace, boolean shouldReportErrorsOnRecursiveTypeInsidePlusAssignment);

    void instantiationOfAbstractClass(@NotNull BindingTrace trace);

    void abstractSuperCall(@NotNull BindingTrace trace);

    default void abstractSuperCallWarning(@NotNull BindingTrace trace) {
        abstractSuperCall(trace);
    }

    void nestedClassAccessViaInstanceReference(
            @NotNull BindingTrace trace,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull ExplicitReceiverKind explicitReceiverKind
    );

    void unsafeCall(@NotNull BindingTrace trace, @NotNull KotlinType type, boolean isCallForImplicitInvoke);

    void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor);

    void typeInferenceFailed(@NotNull ResolutionContext<?> context, @NotNull InferenceErrorData inferenceErrorData);
}
