package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;

/**
 * @author Stepan Koltsov
 */
public enum JvmMethodParameterKind {
    VALUE,
    THIS,
    /** @see CodegenUtil#hasThis0(ClassDescriptor) */
    THIS0,
    RECEIVER,
    TYPE_INFO,
}
