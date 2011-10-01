package org.jetbrains.jet.lang.types;

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
