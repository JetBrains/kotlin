package org.jetbrains.jet.lang.descriptors;

/**
 * @author abreslav
 */
public enum Modality {
    FINAL(false),
    OPEN(true),
    ABSTRACT(true);

    private final boolean open;

    private Modality(boolean open) {
        this.open = open;
    }

    public boolean isOpen() {
        return open;
    }
    
    public static Modality convertFromFlags(boolean _abstract, boolean open) {
        if (_abstract) return ABSTRACT;
        if (open) return OPEN;
        return FINAL;
    }
}
