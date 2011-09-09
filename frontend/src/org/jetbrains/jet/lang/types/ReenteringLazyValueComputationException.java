package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public class ReenteringLazyValueComputationException extends RuntimeException {
    public ReenteringLazyValueComputationException() {
    }

    public ReenteringLazyValueComputationException(String message) {
        super(message);
    }

    public ReenteringLazyValueComputationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReenteringLazyValueComputationException(Throwable cause) {
        super(cause);
    }
}
