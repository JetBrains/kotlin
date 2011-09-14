package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public interface Diagnostic {

    @NotNull
    DiagnosticFactory getFactory();

    @NotNull
    String getMessage();

    @NotNull
    Severity getSeverity();
}
