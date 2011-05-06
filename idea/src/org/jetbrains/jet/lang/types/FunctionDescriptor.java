package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public interface FunctionDescriptor extends DeclarationDescriptor {
    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    @NotNull
    List<TypeParameterDescriptor> getTypeParameters();

    @NotNull
    List<ValueParameterDescriptor> getUnsubstitutedValueParameters();

    @NotNull
    JetType getUnsubstitutedReturnType();

    @NotNull
    FunctionDescriptor getOriginal();

    @Override
    FunctionDescriptor substitute(TypeSubstitutor substitutor);

    @NotNull
    Set<? extends FunctionDescriptor> getOverriddenFunctions();
}
