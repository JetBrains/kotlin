package org.jetbrains.jet.lang.resolve.calls;

/**
 * @author abreslav
 */
public enum ResolutionStatus {
    UNKNOWN_STATUS,
    UNSAFE_CALL_ERROR,
    OTHER_ERROR,
    SUCCESS(true);

    private final boolean success;

    private ResolutionStatus(boolean success) {
        this.success = success;
    }

    private ResolutionStatus() {
        this(false);
    }

    public boolean isSuccess() {
        return success;
    }

    public ResolutionStatus combine(ResolutionStatus other) {
        if (this.isSuccess()) return other;
        if (this.isWeakError() && !other.isSuccess()) return other;
        return this;
    }

    public boolean isWeakError() {
        return this == UNSAFE_CALL_ERROR;
    }
}
