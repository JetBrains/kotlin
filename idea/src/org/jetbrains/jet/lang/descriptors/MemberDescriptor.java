package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface MemberDescriptor {
    @NotNull
    MemberModifiers getModifiers();
}
