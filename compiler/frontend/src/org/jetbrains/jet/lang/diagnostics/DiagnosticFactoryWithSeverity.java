package org.jetbrains.jet.lang.diagnostics;

/**
 * @author abreslav
 */
public abstract class DiagnosticFactoryWithSeverity extends AbstractDiagnosticFactory {
    protected final Severity severity;

    public DiagnosticFactoryWithSeverity(Severity severity) {
        this.severity = severity;
    }

}
