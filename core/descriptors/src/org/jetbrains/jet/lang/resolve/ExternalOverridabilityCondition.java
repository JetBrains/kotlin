package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;

public interface ExternalOverridabilityCondition {
    boolean isOverridable(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor);
}
