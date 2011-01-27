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
        if (projection == ProjectionKind.NEITHER_OUT_NOR_IN) {
            return projection.toString();
        }
        if (projection == ProjectionKind.NO_PROJECTION) {
            return type + "";
        }
        return projection + " " + type;
    }
}
