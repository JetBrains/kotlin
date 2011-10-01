package org.jetbrains.jet.lang.descriptors;

/**
 * @author abreslav
 */
public enum Modality {
    FINAL(false),
    OPEN(true),
    ABSTRACT(true);

    private final boolean overridable;

    private Modality(boolean overridable) {
        this.overridable = overridable;
    }

    public boolean isOverridable() {
        return overridable;
    }
    
    public static Modality convertFromFlags(boolean _abstract, boolean open) {
        if (_abstract) return ABSTRACT;
        if (open) return OPEN;
        return FINAL;
    }
}
