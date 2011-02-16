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
        @Override
        public String getName() {
            return "<empty>";
        }

        @NotNull
        @Override
        public Collection<FunctionDescriptor> getPossiblyApplicableFunctions(@NotNull List<Type> typeArguments, @NotNull List<Type> positionedValueArgumentTypes) {
            return Collections.emptySet();
        }
    };


    @NotNull
    @Override
    String getName();

    @NotNull
    Collection<FunctionDescriptor> getPossiblyApplicableFunctions(@NotNull List<Type> typeArguments, @NotNull List<Type> positionedValueArgumentTypes);
}
