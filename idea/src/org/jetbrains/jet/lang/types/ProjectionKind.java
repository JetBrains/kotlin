package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public enum ProjectionKind {
    OUT_ONLY("out", false, true),
    IN_ONLY("in", true, false),
    NEITHER_OUT_NOR_IN("*", false, false),
    NO_PROJECTION("", true, true);

    private final String text;
    private final boolean allowsInCalls;
    private final boolean allowsOutCalls;

    ProjectionKind(String text, boolean allowsInCalls, boolean allowsOutCalls) {
        this.text = text;
        this.allowsInCalls = allowsInCalls;
        this.allowsOutCalls = allowsOutCalls;

    }

    public boolean allowsInCalls() {
        return allowsInCalls;
    }

    public boolean allowsOutCalls() {
        return allowsOutCalls;
    }

    @Override
    public String toString() {
        return text;
    }
}
