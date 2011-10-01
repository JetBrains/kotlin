package org.jetbrains.jet.lang.diagnostics;

/**
 * @author svtk
 */
public class DiagnosticParameterImpl<P> implements DiagnosticParameter<P> {
    private final String value;

    public DiagnosticParameterImpl(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
