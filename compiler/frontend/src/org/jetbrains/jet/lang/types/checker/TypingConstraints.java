package org.jetbrains.jet.lang.types.checker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;

/**
 * Methods of this class return true to continue type checking and false to fail
 */
public interface TypingConstraints {
    boolean assertEqualTypes(@NotNull JetType a, @NotNull JetType b, TypeCheckingProcedure typeCheckingProcedure);

    boolean assertEqualTypeConstructors(@NotNull TypeConstructor a, @NotNull TypeConstructor b);

    boolean assertSubtype(@NotNull JetType subtype, @NotNull JetType supertype, TypeCheckingProcedure typeCheckingProcedure);

    boolean noCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype);
}
