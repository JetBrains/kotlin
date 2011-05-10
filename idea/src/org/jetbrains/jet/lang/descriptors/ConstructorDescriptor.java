package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;

/**
 * @author abreslav
 */
public interface ConstructorDescriptor extends FunctionDescriptor {
    /**
     * @throws UnsupportedOperationException -- no type parameters supported for constructors
     */
    @NotNull
    @Override
    List<TypeParameterDescriptor> getTypeParameters();

    /**
     * @throws UnsupportedOperationException -- return type is not stored for constructors
     */
    @NotNull
    @Override
    JetType getUnsubstitutedReturnType();

    @NotNull
    @Override
    ClassDescriptor getContainingDeclaration();

    /**
     * @return "&lt;init&gt;" -- name is not stored for constructors
     */
    @NotNull
    @Override
    String getName();

    boolean isPrimary();
}
