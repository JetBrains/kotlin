package org.jetbrains.jet.lang.descriptors;

import java.util.EnumSet;

/**
 * @author svtk
 */
public enum Visibility {
    PRIVATE(false),
    PROTECTED(true),
    INTERNAL(false),
    PUBLIC(true),
    INTERNAL_PROTECTED(false),
    LOCAL(false);

    public static final EnumSet<Visibility> INTERNAL_VISIBILITIES = EnumSet.of(PRIVATE, INTERNAL, INTERNAL_PROTECTED, LOCAL);
    
    private final boolean isAPI;

    private Visibility(boolean visibleOutside) {
        isAPI = visibleOutside;
    }

    public boolean isAPI() {
        return isAPI;
    }
}
