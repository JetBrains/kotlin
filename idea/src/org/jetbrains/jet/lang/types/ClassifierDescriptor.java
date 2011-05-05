package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface ClassifierDescriptor extends DeclarationDescriptor {
    @NotNull
    TypeConstructor getTypeConstructor();

    @NotNull
    JetType getDefaultType();
}
