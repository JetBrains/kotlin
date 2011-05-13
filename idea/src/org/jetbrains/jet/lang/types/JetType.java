package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.Annotated;
import org.jetbrains.jet.lang.resolve.JetScope;

import java.util.List;

/**
 * @author abreslav
 */
public interface JetType extends Annotated {
    @NotNull TypeConstructor getConstructor();
    @NotNull List<TypeProjection> getArguments();
    boolean isNullable();

    @NotNull
    JetScope getMemberScope();

    @Override
    public boolean equals(Object other);
}
