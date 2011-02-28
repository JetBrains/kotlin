package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.psi.JetDeclaration;

/**
 * @author abreslav
 */
public interface PropertyDescriptor extends DeclarationDescriptor {
    Type getType();
}
