package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

/**
 * @author abreslav
 */
public interface ConstraintSystemSolution {
    @NotNull
    SolutionStatus getStatus();

    @NotNull
    TypeSubstitutor getSubstitutor();

    @Nullable
    JetType getValue(TypeParameterDescriptor typeParameterDescriptor);
}
