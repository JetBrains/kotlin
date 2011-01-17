package org.jetbrains.jet.lang.types;

/**
 * @author max
 */
public enum Variance {
    INVARIANT(""),
    IN_VARIANCE("in"),
    OUT_VARIANCE("out");

    private final String label;

    Variance(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
