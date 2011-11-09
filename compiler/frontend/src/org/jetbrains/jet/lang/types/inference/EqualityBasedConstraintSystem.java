package org.jetbrains.jet.lang.types.inference;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class EqualityBasedConstraintSystem implements ConstraintSystem {

    private abstract class Constraint {
        private final TypeParameterDescriptor typeParameterDescriptor;

        protected Constraint(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
            this.typeParameterDescriptor = typeParameterDescriptor;
        }

        @NotNull
        public TypeParameterDescriptor getTypeParameterDescriptor() {
            return typeParameterDescriptor;
        }
    }

    private class Unknown {
        // T -> variance of its position
        private final Map<TypeParameterDescriptor, Variance> typeParameters = Maps.newHashMap();
        private final List<Constraint> constraints = Lists.newArrayList();

    }

    private final Map<TypeParameterDescriptor, Unknown> unknowns = Maps.newHashMap();

    @Override
    public void registerTypeVariable(@NotNull TypeParameterDescriptor typeParameterDescriptor, @NotNull Variance positionVariance) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void addSubtypingConstraint(@NotNull JetType lower, @NotNull JetType upper) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public ConstraintSystemSolution solve() {
        throw new UnsupportedOperationException(); // TODO
    }
}
