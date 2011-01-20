package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public class TypeProjection {
    private final Variance projection;
    private final Type type;

    public TypeProjection(Variance projection, Type type) {
        this.projection = projection;
        this.type = type;
    }

    public TypeProjection(Type type) {
        this(Variance.INVARIANT, type);
    }

    public Variance getProjection() {
        return projection;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return projection + " " + type;
    }
}
