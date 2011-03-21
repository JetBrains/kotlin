package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.FunctionDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public interface OverloadDomain {
    OverloadDomain EMPTY = new OverloadDomain() {
        @Nullable
        @Override
        public FunctionDescriptor getFunctionDescriptorForNamedArguments(@NotNull List<JetType> typeArguments, @NotNull Map<String, JetType> valueArgumentTypes, @Nullable JetType functionLiteralArgumentType) {
            return null;
        }

        @Nullable
        @Override
        public FunctionDescriptor getFunctionDescriptorForPositionedArguments(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return true;
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
            @NotNull List<JetType> typeArguments,
            @NotNull Map<String, JetType> valueArgumentTypes,
            @Nullable JetType functionLiteralArgumentType);

    /**
     * @param typeArguments
     * @param positionedValueArgumentTypes
     * @return A function descriptor with NO type parameters (they are already substituted), or null
     */
    @Nullable
    FunctionDescriptor getFunctionDescriptorForPositionedArguments(
            @NotNull List<JetType> typeArguments,
            @NotNull List<JetType> positionedValueArgumentTypes);

    /**
     * @return true if the domain is empty
     */
    boolean isEmpty();
}
