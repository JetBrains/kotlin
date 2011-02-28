package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public interface ValueParameterDescriptor extends PropertyDescriptor {
    /**
     * Returns the 0-based index of the value parameter in the parameter list of its containing function.
     *
     * @return the parameter index
     */
    int getIndex();
    boolean hasDefaultValue();
    boolean isRef();
    boolean isVararg();
}
