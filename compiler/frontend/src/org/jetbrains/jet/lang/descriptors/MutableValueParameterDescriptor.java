package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public interface MutableValueParameterDescriptor extends ValueParameterDescriptor {
    void setType(@NotNull JetType type);
}
