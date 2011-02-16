package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public interface ValueParameterDescriptor extends PropertyDescriptor {
    boolean hasDefaultValue();
    boolean isRef();
    boolean isVararg();
}
