package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.OverloadResolutionResult;

import java.util.List;

/**
 * @author abreslav
 */
public interface FunctionGroup extends Named {
    FunctionGroup EMPTY = new FunctionGroup() {
        @NotNull
        @Override
        public String getName() {
            return "<empty>";
        }

        @NotNull
        @Override
        public OverloadResolutionResult getPossiblyApplicableFunctions(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
            return OverloadResolutionResult.nameNotFound();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    };


    @NotNull
    @Override
    String getName();

    @NotNull
    OverloadResolutionResult getPossiblyApplicableFunctions(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes);

    boolean isEmpty();
}
