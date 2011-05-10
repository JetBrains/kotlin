package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.OverloadResolutionResult;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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

        @NotNull
        @Override
        public Set<FunctionDescriptor> getFunctionDescriptors() {
            return Collections.emptySet();
        }
    };


    @NotNull
    @Override
    String getName();

    @NotNull
    OverloadResolutionResult getPossiblyApplicableFunctions(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes);

    boolean isEmpty();

    @NotNull
    Set<FunctionDescriptor> getFunctionDescriptors();
}
