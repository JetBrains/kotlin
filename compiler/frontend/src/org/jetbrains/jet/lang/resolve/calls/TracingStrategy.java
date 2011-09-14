package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Set;

/**
* @author abreslav
*/
/*package*/ interface TracingStrategy {
    TracingStrategy EMPTY = new TracingStrategy() {
        @Override
        public void bindReference(@NotNull BindingTrace trace, @NotNull CallableDescriptor descriptor) {}

        @Override
        public void unresolvedReference(@NotNull BindingTrace trace) {}

        @Override
        public <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<D> candidates) {}

        @Override
        public void missingReceiver(@NotNull BindingTrace trace, @NotNull JetType candidateReceiverType) {}

        @Override
        public void noReceiverAllowed(@NotNull BindingTrace trace) {}

        @Override
        public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {}

        @Override
        public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount) {}

        @Override
        public void ambiguity(@NotNull BindingTrace trace, @NotNull Set<? extends CallableDescriptor> descriptors) {}

        @Override
        public void noneApplicable(@NotNull BindingTrace trace, @NotNull Set<? extends CallableDescriptor> descriptors) {}

        @Override
        public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {}

        @Override
        public void typeInferenceFailed(@NotNull BindingTrace trace) {}
    };

    void bindReference(@NotNull BindingTrace trace, @NotNull CallableDescriptor descriptor);

    void unresolvedReference(@NotNull BindingTrace trace);

    <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<D> candidates);

    void missingReceiver(@NotNull BindingTrace trace, @NotNull JetType candidateReceiverType);

    void noReceiverAllowed(@NotNull BindingTrace trace);

    void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter);

    void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount);

    void ambiguity(@NotNull BindingTrace trace, @NotNull Set<? extends CallableDescriptor> descriptors);

    void noneApplicable(@NotNull BindingTrace trace, @NotNull Set<? extends CallableDescriptor> descriptors);

    void instantiationOfAbstractClass(@NotNull BindingTrace trace);

    void typeInferenceFailed(@NotNull BindingTrace trace);
}
