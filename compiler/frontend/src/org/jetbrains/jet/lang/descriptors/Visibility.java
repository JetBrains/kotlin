package org.jetbrains.jet.lang.descriptors;

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

    private final boolean isAPI;

    private Visibility(boolean visibleOutside) {
        isAPI = visibleOutside;
    }

    public boolean isAPI() {
        return isAPI;
    }
}
