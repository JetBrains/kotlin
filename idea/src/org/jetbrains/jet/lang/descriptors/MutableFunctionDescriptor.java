package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public interface MutableFunctionDescriptor extends FunctionDescriptor {
    void setUnsubstitutedReturnType(@NotNull JetType type);

    boolean isReturnTypeSet();
}
