package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public interface PropertyDescriptor extends Annotated, Named {
    Type getType();
}
