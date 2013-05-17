package org.jetbrains.jet.descriptors.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;

public interface NestedClassResolver {
    @Nullable
    ClassDescriptor resolveNestedClass(@NotNull ClassDescriptor outerClass, @NotNull Name name);

    @Nullable
    ClassDescriptor resolveClassObject(@NotNull ClassDescriptor outerClass);
}
