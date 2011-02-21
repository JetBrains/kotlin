package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author abreslav
 */
public interface FunctionDescriptor extends Annotated, Named {
    @NotNull
    List<TypeParameterDescriptor> getTypeParameters();

    @NotNull
    List<ValueParameterDescriptor> getUnsubstitutedValueParameters();

    @NotNull
    Type getUnsubstitutedReturnType();
}
