package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;

/**
 * @author abreslav
 */
public interface ClassifierDescriptor extends DeclarationDescriptor {
    @NotNull
    TypeConstructor getTypeConstructor();

    @NotNull
    JetType getDefaultType();

    @Nullable
    JetType getClassObjectType();

    boolean isClassObjectAValue();
}
