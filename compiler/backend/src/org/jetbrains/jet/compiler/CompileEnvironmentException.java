package org.jetbrains.jet.compiler;

/**
 * @author yole
 */
public class CompileEnvironmentException extends RuntimeException {
    public CompileEnvironmentException(String message) {
        super(message);
    }

    public CompileEnvironmentException(Throwable cause) {
        super(cause);
    }

    public CompileEnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
