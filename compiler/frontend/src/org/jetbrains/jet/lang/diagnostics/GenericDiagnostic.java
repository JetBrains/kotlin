package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class GenericDiagnostic implements DiagnosticWithTextRange {

    private final TextRange textRange;
    private final String message;
    private final DiagnosticFactory factory;
    private final Severity severity;

    public GenericDiagnostic(DiagnosticFactory factory, Severity severity, String message, TextRange textRange) {
        this.factory = factory;
        this.textRange = textRange;
        this.severity = severity;
        this.message = message;
    }

    @NotNull
    @Override
    public DiagnosticFactory getFactory() {
        return factory;
    }

    @NotNull
    public TextRange getTextRange() {
        return textRange;
    }

    @NotNull
    @Override
    public String getMessage() {
        return message;
    }

    @NotNull
    @Override
    public Severity getSeverity() {
        return severity;
    }
}
