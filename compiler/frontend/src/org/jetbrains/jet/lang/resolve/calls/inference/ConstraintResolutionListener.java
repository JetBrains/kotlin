package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Set;

/**
 * @author abreslav
 */
public interface ConstraintResolutionListener {

    void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, ConstraintSystemImpl.TypeValue typeValue);
    void constraintsForKnownType(JetType type, ConstraintSystemImpl.TypeValue typeValue);
    void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors);

    void log(Object message);
    void error(Object message);
}
