package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public interface DiagnosticHolder {
    DiagnosticHolder DO_NOTHING = new DiagnosticHolder() {
        @Override
        public void report(@NotNull Diagnostic diagnostic) {
        }
    };
    DiagnosticHolder THROW_EXCEPTION = new DiagnosticHolder() {
        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic.getSeverity() == Severity.ERROR) {
                throw new IllegalStateException(diagnostic.getMessage());
            }
        }
    };

    void report(@NotNull Diagnostic diagnostic);
}
