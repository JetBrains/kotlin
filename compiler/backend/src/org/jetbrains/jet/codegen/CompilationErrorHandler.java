package org.jetbrains.jet.codegen;

/**
 * @author abreslav
 */
public interface CompilationErrorHandler {

    CompilationErrorHandler THROW_EXCEPTION = new CompilationErrorHandler() {
        @Override
        public void reportError(String message, String fileUrl) {
            throw new IllegalStateException(message);
        }
    };

    void reportError(String message, String fileUrl);
}
