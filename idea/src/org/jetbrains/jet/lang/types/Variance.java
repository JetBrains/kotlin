package org.jetbrains.jet.lang.types;

/**
 * @author max
 */
public enum Variance {
    INVARIANT("", true, true),
    IN_VARIANCE("in", true, false),
    OUT_VARIANCE("out", false, true);

    private final String label;
    private final boolean allowsInPosition;
    private final boolean allowsOutPosition;

    Variance(String label, boolean allowsInPosition, boolean allowsOutPosition) {
        this.label = label;
        this.allowsInPosition = allowsInPosition;
        this.allowsOutPosition = allowsOutPosition;
    }

    public boolean allowsInPosition() {
        return allowsInPosition;
    }

    public boolean allowsOutPosition() {
        return allowsOutPosition;
    }

    @Override
    public String toString() {
        return label;
    }
}
