package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.List;

/**
 * @author abreslav
 * @see JetTypeChecker#isSubtypeOf(JetType, JetType)
 */
public interface JetType extends Annotated {
    @NotNull TypeConstructor getConstructor();
    @NotNull List<TypeProjection> getArguments();
    boolean isNullable();

    @NotNull
    JetScope getMemberScope();

    @Override
    boolean equals(Object other);
}
