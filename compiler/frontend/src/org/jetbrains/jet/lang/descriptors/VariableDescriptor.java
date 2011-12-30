package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

/**
 * @author abreslav
 */
public interface VariableDescriptor extends CallableDescriptor {
    @NotNull
    JetType getOutType();

    @Override
    @SuppressWarnings({"NullableProblems"})
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    @Override
    VariableDescriptor substitute(TypeSubstitutor substitutor);

    boolean isVar();
}
