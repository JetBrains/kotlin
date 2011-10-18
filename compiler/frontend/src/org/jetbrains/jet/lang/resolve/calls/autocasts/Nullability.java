package org.jetbrains.jet.lang.resolve.calls.autocasts;

import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public enum Nullability {
    NULL(true, false),
    NOT_NULL(false, true),
    UNKNOWN(true, true),
    IMPOSSIBLE(false, false);

    @NotNull
    public static Nullability fromFlags(boolean canBeNull, boolean canBeNonNull) {
        if (!canBeNull && !canBeNonNull) return IMPOSSIBLE;
        if (!canBeNull && canBeNonNull) return NOT_NULL;
        if (canBeNull  && !canBeNonNull) return NULL;
        return UNKNOWN;
    }

    private final boolean canBeNull;
    private final boolean canBeNonNull;

    Nullability(boolean canBeNull, boolean canBeNonNull) {
        this.canBeNull = canBeNull;
        this.canBeNonNull = canBeNonNull;
    }

    public boolean canBeNull() {
        return canBeNull;
    }

    public boolean canBeNonNull() {
        return canBeNonNull;
    }

    @NotNull
    public Nullability refine(@NotNull Nullability other) {
        switch (this) {
            case UNKNOWN:
                return other;
            case IMPOSSIBLE:
                return other;
            case NULL:
                switch (other) {
                    case NOT_NULL: return NOT_NULL;
                    default: return NULL;
                }
            case NOT_NULL:
                switch (other) {
                    case NULL: return NOT_NULL;
                    default: return NOT_NULL;
                }
        }
        throw new IllegalStateException();
    }

    @NotNull
    public Nullability invert() {
        switch (this) {
            case NULL:
                return NOT_NULL;
            case NOT_NULL:
                return UNKNOWN;
            case UNKNOWN:
                return UNKNOWN;
            case IMPOSSIBLE:
                return UNKNOWN;
        }
        throw new IllegalStateException();
    }

    @NotNull
    public Nullability and(@NotNull Nullability other) {
        return fromFlags(this.canBeNull && other.canBeNull, this.canBeNonNull && other.canBeNonNull);
    }

    @NotNull
    public Nullability or(@NotNull Nullability other) {
        return fromFlags(this.canBeNull || other.canBeNull, this.canBeNonNull || other.canBeNonNull);
    }
}
