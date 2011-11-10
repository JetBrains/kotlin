package org.jetbrains.jet.lang.types.inference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;

/**
 * @author abreslav
 */
public interface ConstraintSystem {
    void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance);

    void addSubtypingConstraint(@NotNull JetType lower, @NotNull JetType upper);

    @NotNull
    ConstraintSystemSolution solve();
}
