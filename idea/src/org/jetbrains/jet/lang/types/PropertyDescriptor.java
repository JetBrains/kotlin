package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public interface PropertyDescriptor extends DeclarationDescriptor {
    @Nullable
    JetType getOutType();

    @Nullable
    JetType getInType();

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();
}
