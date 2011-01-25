package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public enum ProjectionKind {
    OUT_ONLY("out"),
    IN_ONLY("in"),
    NEITHER_OUT_NOR_IN("*"),
    NO_PROJECTION("");

    private final String text;

    ProjectionKind(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
