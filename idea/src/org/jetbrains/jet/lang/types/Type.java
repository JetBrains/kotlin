package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author abreslav
 */
public interface Type extends Annotated {
    @NotNull TypeConstructor getConstructor();
    @NotNull List<TypeProjection> getArguments();
    boolean isNullable();

    @NotNull TypeMemberDomain getMemberDomain();

    @NotNull Type getNullableVersion();
}
