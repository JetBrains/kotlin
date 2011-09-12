package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
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

        @Override
        public boolean isEmpty() {
            return true;
        }

        @NotNull
        @Override
        public Set<FunctionDescriptor> getFunctionDescriptors() {
            return Collections.emptySet();
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    };


    @NotNull
    @Override
    String getName();

    boolean isEmpty();

    @NotNull
    Set<FunctionDescriptor> getFunctionDescriptors();
}
