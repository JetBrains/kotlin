package org.jetbrains.jet.lang.types.inference;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

/**
 * @author abreslav
 */
public interface ConstraintSystemSolution {
    boolean isSuccessful();

    TypeSubstitutor getSubstitutor();

    @Nullable
    JetType getValue(TypeParameterDescriptor typeParameterDescriptor);
}
