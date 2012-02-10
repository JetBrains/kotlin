package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Set;

/**
 * @author abreslav
 */
public interface FunctionDescriptor extends CallableMemberDescriptor {
    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    @NotNull
    @Override
    FunctionDescriptor getOriginal();

    @Override
    FunctionDescriptor substitute(TypeSubstitutor substitutor);

    @Override
    @NotNull
    Set<? extends FunctionDescriptor> getOverriddenDescriptors();

    @NotNull
    @Override
    FunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract, Kind kind, boolean copyOverrides);
}
