package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

/**
* @author abreslav
*/
public class UnresolvedReferenceDiagnosticFactory extends AbstractDiagnosticFactory {
//    public static final UnresolvedReferenceDiagnosticFactory INSTANCE = new UnresolvedReferenceDiagnosticFactory();

    private final String message;
    
    public UnresolvedReferenceDiagnosticFactory(String message) {
        this.message = message;
    }

    public UnresolvedReferenceDiagnostic on(@NotNull JetReferenceExpression reference) {
        return new UnresolvedReferenceDiagnostic(reference, message);
    }
}
