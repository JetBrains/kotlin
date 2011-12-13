package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author abreslav
 */
public interface BoundsOwner {
    @NotNull
    Set<TypeValue> getUpperBounds();

    @NotNull
    Set<TypeValue> getLowerBounds();
}
