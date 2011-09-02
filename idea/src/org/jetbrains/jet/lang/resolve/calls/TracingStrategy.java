package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;

import java.util.Collection;

/**
* @author abreslav
*/
/*package*/ interface TracingStrategy {
    void bindReference(@NotNull BindingTrace trace, @NotNull CallableDescriptor descriptor);

    void reportOverallResolutionError(@NotNull BindingTrace trace, @NotNull String message);

    void reportWrongTypeArguments(@NotNull BindingTrace trace, @NotNull String message);

    void reportWrongValueArguments(@NotNull BindingTrace trace, @NotNull String message);

    void reportUnresolvedReference(@NotNull BindingTrace trace);

    void reportErrorOnReference(BindingTrace trace, String message);

    <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<D> candidates);
}
