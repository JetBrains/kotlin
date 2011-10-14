package org.jetbrains.jet.util.lazy;

/**
 * @author abreslav
 */
public class ReenteringLazyValueComputationException extends RuntimeException {
    public ReenteringLazyValueComputationException() {
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
