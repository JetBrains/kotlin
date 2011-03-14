package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface PropertyDescriptor extends DeclarationDescriptor {
    Type getType();

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();
}
