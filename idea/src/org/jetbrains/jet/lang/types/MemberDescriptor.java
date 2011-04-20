package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface MemberDescriptor {
    @NotNull
    MemberModifiers getModifiers();
}
