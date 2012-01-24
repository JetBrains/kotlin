package org.jetbrains.jet.compiler;

/**
 * @author yole
 */
public class ModuleExecutionException extends RuntimeException {
    public ModuleExecutionException(String message) {
        super(message);
    }

    public ModuleExecutionException(Throwable cause) {
        super(cause);
    }

    public ModuleExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
