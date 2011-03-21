package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
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
        public Collection<FunctionDescriptor> getPossiblyApplicableFunctions(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
            return Collections.emptyList();
//            return OverloadResolutionResult.nameNotFound();
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
    Collection<FunctionDescriptor> getPossiblyApplicableFunctions(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes);
//    @NotNull
//    OverloadResolutionResult getPossiblyApplicableFunctions(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes);

    boolean isEmpty();
}
