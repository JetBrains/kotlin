package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface MutableFunctionDescriptor extends FunctionDescriptor {
    void setUnsubstitutedReturnType(@NotNull JetType type);

    boolean isReturnTypeSet();
}
