package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.inference.SolutionStatus;

import java.util.Collection;

/**
* @author abreslav
*/
/*package*/ interface TracingStrategy {
    TracingStrategy EMPTY = new TracingStrategy() {
        @Override
        public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCallImpl<D> resolvedCall) {}

        @Override
        public void unresolvedReference(@NotNull BindingTrace trace) {}

        @Override
        public <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCallImpl<D>> candidates) {}

        @Override
        public void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor expectedReceiver) {}

        @Override
        public void wrongReceiverType(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor receiverParameter, @NotNull ReceiverDescriptor receiverArgument) {}

        @Override
        public void noReceiverAllowed(@NotNull BindingTrace trace) {}

        @Override
        public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {}

        @Override
        public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount) {}

        @Override
        public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallImpl<D>> descriptors) {}

        @Override
        public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallImpl<D>> descriptors) {}

        @Override
        public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {}

        @Override
        public void typeInferenceFailed(@NotNull BindingTrace trace, SolutionStatus status) {}

        @Override
        public void unsafeCall(@NotNull BindingTrace trace, @NotNull JetType type) {}

        @Override
        public void unnecessarySafeCall(@NotNull BindingTrace trace, @NotNull JetType type) {}
    };

    <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCallImpl<D> resolvedCall);

    void unresolvedReference(@NotNull BindingTrace trace);

    <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCallImpl<D>> candidates);

    void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor expectedReceiver);

    void wrongReceiverType(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor receiverParameter, @NotNull ReceiverDescriptor receiverArgument);

    void noReceiverAllowed(@NotNull BindingTrace trace);

    void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter);

    void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount);

    <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallImpl<D>> descriptors);

    <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallImpl<D>> descriptors);

    void instantiationOfAbstractClass(@NotNull BindingTrace trace);

    void typeInferenceFailed(@NotNull BindingTrace trace, SolutionStatus status);

    void unsafeCall(@NotNull BindingTrace trace, @NotNull JetType type);

    void unnecessarySafeCall(@NotNull BindingTrace trace, @NotNull JetType type);
}
