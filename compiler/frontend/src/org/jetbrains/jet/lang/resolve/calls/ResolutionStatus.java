package org.jetbrains.jet.lang.resolve.calls;

import java.util.EnumSet;

/**
 * @author abreslav
 */
public enum ResolutionStatus {
    UNKNOWN_STATUS,
    UNSAFE_CALL_ERROR,
    OTHER_ERROR,
    SUCCESS(true);

    @SuppressWarnings("unchecked")
    public static final EnumSet<ResolutionStatus>[] SEVERITY_LEVELS = new EnumSet[] {
            EnumSet.of(UNSAFE_CALL_ERROR), // weakest
            EnumSet.of(OTHER_ERROR), // most severe
    };

    private final boolean success;
    private int severityIndex = -1;

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
        if (!other.isSuccess() && this.getSeverityIndex() < other.getSeverityIndex()) return other;
        return this;
    }
    
    private int getSeverityIndex() {
        if (severityIndex == -1) {
            for (int i = 0; i < SEVERITY_LEVELS.length; i++) {
                if (SEVERITY_LEVELS[i].contains(this)) {
                    severityIndex = i;
                    break;
                }
            }
        }
        assert severityIndex >= 0;

        return severityIndex;
    }
}
