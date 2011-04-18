package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface ValueParameterDescriptor extends VariableDescriptor {
    /**
     * Returns the 0-based index of the value parameter in the parameter list of its containing function.
     *
     * @return the parameter index
     */
    int getIndex();
    boolean hasDefaultValue();
    boolean isRef();
    boolean isVararg();

    @Override
    @NotNull
    JetType getOutType();
}
