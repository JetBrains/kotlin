package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

/**
 * @author Stepan Koltsov
 *
 * @see TypeVariableByPsiResolver
 */
public interface TypeVariableByNameResolver {
    @NotNull
    TypeParameterDescriptor getTypeVariable(@NotNull String name);
}
