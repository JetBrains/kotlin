package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collection;
import java.util.Set;

/**
* @author abreslav
*/
/*package*/ interface TracingStrategy {
    TracingStrategy EMPTY = new TracingStrategy() {
        @Override
        public void bindReference(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor receiver, @NotNull CallableDescriptor descriptor) {}

        @Override
        public void unresolvedReference(@NotNull BindingTrace trace) {}

        @Override
        public <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCall<D>> candidates) {}

        @Override
        public void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor expectedReceiver) {}

        @Override
        public void noReceiverAllowed(@NotNull BindingTrace trace) {}

        @Override
        public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {}

        @Override
        public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount) {}

        @Override
        public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Set<ResolvedCall<D>> descriptors) {}

        @Override
        public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Set<ResolvedCall<D>> descriptors) {}

        @Override
        public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {}

        @Override
        public void typeInferenceFailed(@NotNull BindingTrace trace) {}
    };

    void bindReference(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor receiver, @NotNull CallableDescriptor descriptor);

    void unresolvedReference(@NotNull BindingTrace trace);

    <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCall<D>> candidates);

    void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor expectedReceiver);

    void noReceiverAllowed(@NotNull BindingTrace trace);

    void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter);

    void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount);

    <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Set<ResolvedCall<D>> descriptors);

    <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Set<ResolvedCall<D>> descriptors);

    void instantiationOfAbstractClass(@NotNull BindingTrace trace);

    void typeInferenceFailed(@NotNull BindingTrace trace);
}
