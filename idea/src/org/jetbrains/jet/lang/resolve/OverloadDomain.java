package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.Type;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public interface OverloadDomain {
    OverloadDomain EMPTY = new OverloadDomain() {
        @Nullable
        @Override
        public Type getReturnTypeForNamedArguments(@NotNull List<Type> typeArguments, @NotNull Map<String, Type> valueArgumentTypes, @Nullable Type functionLiteralArgumentType) {
            return null;
        }

        @Nullable
        @Override
        public Type getReturnTypeForPositionedArguments(@NotNull List<Type> typeArguments, @NotNull List<Type> positionedValueArgumentTypes) {
            return null;
        }
    };

    @Nullable
    Type getReturnTypeForNamedArguments(
            @NotNull List<Type> typeArguments,
            @NotNull Map<String, Type> valueArgumentTypes,
            @Nullable Type functionLiteralArgumentType);

    @Nullable
    Type getReturnTypeForPositionedArguments(
            @NotNull List<Type> typeArguments,
            @NotNull List<Type> positionedValueArgumentTypes);
}
