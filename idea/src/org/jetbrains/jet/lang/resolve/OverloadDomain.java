package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public interface OverloadDomain {

    OverloadDomain EMPTY = new OverloadDomain() {
        @NotNull
        @Override
        public OverloadResolutionResult getFunctionDescriptorForNamedArguments(@NotNull List<JetType> typeArguments, @NotNull Map<String, JetType> valueArgumentTypes, @Nullable JetType functionLiteralArgumentType) {
            return OverloadResolutionResult.nameNotFound();
        }

        @NotNull
        @Override
        public OverloadResolutionResult getFunctionDescriptorForPositionedArguments(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
            return OverloadResolutionResult.nameNotFound();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    };

    /**
     * @return A function descriptor with NO type parameters (they are already substituted) wrapped together with a result code
     */
    @NotNull
    OverloadResolutionResult getFunctionDescriptorForNamedArguments(
            @NotNull List<JetType> typeArguments,
            @NotNull Map<String, JetType> valueArgumentTypes,
            @Nullable JetType functionLiteralArgumentType);

    /**
     * @return A function descriptor with NO type parameters (they are already substituted) wrapped together with a result code
     */
    @NotNull
    OverloadResolutionResult getFunctionDescriptorForPositionedArguments(
            @NotNull List<JetType> typeArguments,
            @NotNull List<JetType> positionedValueArgumentTypes);

    /**
     * @return true if the domain is empty
     */
    boolean isEmpty();
}
