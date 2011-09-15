package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

/**
* @author abreslav
*/
public class AbstractDiagnosticFactory implements DiagnosticFactory {
    protected final MessageFormat messageFormat;
    protected final Severity severity;

    public AbstractDiagnosticFactory(Severity severity, String message) {
        this.severity = severity;
        this.messageFormat = new MessageFormat(message);
    }

    @NotNull
    @Override
    public TextRange getMarkerPosition(@NotNull Diagnostic diagnostic) {
        return ((DiagnosticWithTextRange) diagnostic).getTextRange();
    }
}
