package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

/**
 * @author Stepan Koltsov
 */
public interface TypeVariableResolver {
    @NotNull
    TypeParameterDescriptor getTypeVariable(@NotNull String name);
}
