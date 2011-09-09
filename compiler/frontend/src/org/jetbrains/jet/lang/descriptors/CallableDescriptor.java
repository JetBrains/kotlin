package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;
import java.util.Set;

/**
* @author abreslav
*/
public interface CallableDescriptor extends DeclarationDescriptor {
    @Nullable
    JetType getReceiverType();

    @NotNull
    List<TypeParameterDescriptor> getTypeParameters();

    @NotNull
    JetType getReturnType();

    @NotNull
    @Override
    CallableDescriptor getOriginal();

    @Override
    CallableDescriptor substitute(TypeSubstitutor substitutor);

    @NotNull
    List<ValueParameterDescriptor> getValueParameters();

    @NotNull
    Set<? extends CallableDescriptor> getOverriddenDescriptors();
}
