package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

/**
 * @author abreslav
 */
public interface VariableDescriptor extends DeclarationDescriptor {
    /**
     * @return <code>null</code> for write-only variables (i.e. properties), variable value type otherwise
     */
    @Nullable
    JetType getOutType();

    /**
     * @return <code>null</code> for read-only variables (i.e. val's etc), or the type expected on assignment type otherwise
     */
    @Nullable
    JetType getInType();

    @Override
    @SuppressWarnings({"NullableProblems"})
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    @NotNull
    @Override
    VariableDescriptor substitute(TypeSubstitutor substitutor);
}
