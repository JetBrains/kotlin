package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

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
    JetType getUnsubstitutedReturnType();

    @NotNull
    FunctionDescriptor getOriginal();

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    @Override
    FunctionDescriptor substitute(TypeSubstitutor substitutor);
}
