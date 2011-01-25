package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public class TypeProjection {
    private final ProjectionKind projection;
    private final Type type;

    public TypeProjection(ProjectionKind projection, Type type) {
        this.projection = projection;
        this.type = type;
    }

    public TypeProjection(Type type) {
        this(ProjectionKind.NO_PROJECTION, type);
    }

    public ProjectionKind getProjectionKind() {
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
