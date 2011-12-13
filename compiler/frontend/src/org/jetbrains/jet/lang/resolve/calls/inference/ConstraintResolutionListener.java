package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Set;

/**
 * @author abreslav
 */
public interface ConstraintResolutionListener {

    public static final ConstraintResolutionListener DO_NOTHING = new ConstraintResolutionListener() {
        @Override
        public void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, BoundsOwner typeValue) {
        }

        @Override
        public void constraintsForKnownType(JetType type, BoundsOwner typeValue) {
        }

        @Override
        public void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors) {
        }

        @Override
        public void log(Object... messageFragments) {
        }

        @Override
        public void error(Object... messageFragments) {
        }
    };

    void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, BoundsOwner typeValue);
    void constraintsForKnownType(JetType type, BoundsOwner typeValue);
    void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors);

    void log(Object... messageFragments);
    void error(Object... messageFragments);
}
