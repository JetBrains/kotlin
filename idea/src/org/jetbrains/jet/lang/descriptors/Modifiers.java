package org.jetbrains.jet.lang.descriptors;

/**
 * @author svtk
 */
public class Modifiers {
    private final boolean isAbstract;

    public Modifiers(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public boolean isAbstract() {
        return isAbstract;
    }
}
