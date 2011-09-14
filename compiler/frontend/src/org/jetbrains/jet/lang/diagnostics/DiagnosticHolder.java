package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public interface DiagnosticHolder {
    void report(@NotNull Diagnostic diagnostic);
}
