package org.jetbrains.jet.codegen;

/**
 * @author abreslav
 */
public interface CompilationErrorHandler {

    CompilationErrorHandler THROW_EXCEPTION = new CompilationErrorHandler() {
        @Override
        public void reportException(Throwable exception, String fileUrl) {
            throw new IllegalStateException(exception);
        }
    };

    void reportException(Throwable exception, String fileUrl);
}
