package org.jetbrains.jet.lang.diagnostics;

import java.text.MessageFormat;

/**
* @author abreslav
*/
public abstract class DiagnosticFactoryWithMessageFormat extends DiagnosticFactoryWithSeverity {
    protected final MessageFormat messageFormat;
    protected final Renderer renderer;

    public DiagnosticFactoryWithMessageFormat(Severity severity, String message) {
        this(severity, message, Renderer.TO_STRING);
    }

    public DiagnosticFactoryWithMessageFormat(Severity severity, String message, Renderer renderer) {
        super(severity);
        this.messageFormat = new MessageFormat(message);
        this.renderer = renderer;
    }

}
