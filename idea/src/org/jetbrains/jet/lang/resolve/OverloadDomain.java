package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.FunctionDescriptor;
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
        public FunctionDescriptor getFunctionDescriptorForNamedArguments(@NotNull List<Type> typeArguments, @NotNull Map<String, Type> valueArgumentTypes, @Nullable Type functionLiteralArgumentType) {
            return null;
        }

        @Nullable
        @Override
        public FunctionDescriptor getFunctionDescriptorForPositionedArguments(@NotNull List<Type> typeArguments, @NotNull List<Type> positionedValueArgumentTypes) {
            return null;
        }
    };

    /**
     * @param typeArguments
     * @param valueArgumentTypes
     * @param functionLiteralArgumentType
     * @return A function descriptor with NO type parameters (they are already substituted), or null
     */
    @Nullable
    FunctionDescriptor getFunctionDescriptorForNamedArguments(
            @NotNull List<Type> typeArguments,
            @NotNull Map<String, Type> valueArgumentTypes,
            @Nullable Type functionLiteralArgumentType);

    /**
     * @param typeArguments
     * @param positionedValueArgumentTypes
     * @return A function descriptor with NO type parameters (they are already substituted), or null
     */
    @Nullable
    FunctionDescriptor getFunctionDescriptorForPositionedArguments(
            @NotNull List<Type> typeArguments,
            @NotNull List<Type> positionedValueArgumentTypes);
}
