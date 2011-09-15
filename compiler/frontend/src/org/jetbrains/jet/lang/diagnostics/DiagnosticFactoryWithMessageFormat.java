package org.jetbrains.jet.lang.diagnostics;

import java.text.MessageFormat;

/**
* @author abreslav
*/
public class DiagnosticFactoryWithMessageFormat extends DiagnosticFactoryWithSeverity {
    protected final MessageFormat messageFormat;

    public DiagnosticFactoryWithMessageFormat(Severity severity, String message) {
        super(severity);
        this.messageFormat = new MessageFormat(message);
    }

}
