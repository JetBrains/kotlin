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

    @Nullable
    FunctionDescriptor getOriginal();
}
