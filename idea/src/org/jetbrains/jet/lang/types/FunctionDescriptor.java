package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author abreslav
 */
public interface FunctionDescriptor extends DeclarationDescriptor {
    @NotNull
    List<TypeParameterDescriptor> getTypeParameters();

    @NotNull
    List<ValueParameterDescriptor> getUnsubstitutedValueParameters();

    @NotNull
    Type getUnsubstitutedReturnType();

    /**
     * @return The descriptor that corresponds to the original declaration of this function.
     *         A descriptor can be obtained from its original by substituting type arguments (of the declaring class
     *         or of the function itself).
     *         null if the current descriptor is original itself
     */
    @Nullable
    FunctionDescriptor getOriginal();
}
