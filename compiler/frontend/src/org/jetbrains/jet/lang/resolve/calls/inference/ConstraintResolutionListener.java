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
        public void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, ConstraintSystemImpl.TypeValue typeValue) {
        }

        @Override
        public void constraintsForKnownType(JetType type, ConstraintSystemImpl.TypeValue typeValue) {
        }

        @Override
        public void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors) {
        }

        @Override
        public void log(Object message) {
        }

        @Override
        public void error(Object message) {
        }
    };

    void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, ConstraintSystemImpl.TypeValue typeValue);
    void constraintsForKnownType(JetType type, ConstraintSystemImpl.TypeValue typeValue);
    void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors);

    void log(Object message);
    void error(Object message);
}
