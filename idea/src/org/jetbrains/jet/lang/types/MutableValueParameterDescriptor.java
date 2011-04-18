package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface MutableValueParameterDescriptor extends ValueParameterDescriptor {
    void setType(@NotNull JetType type);
}
