package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

/**
* @author abreslav
*/
public class UnresolvedReferenceDiagnosticFactory extends AbstractDiagnosticFactory {
    public static final UnresolvedReferenceDiagnosticFactory INSTANCE = new UnresolvedReferenceDiagnosticFactory();

    public UnresolvedReferenceDiagnosticFactory() {}

    public UnresolvedReferenceDiagnostic on(@NotNull JetReferenceExpression reference) {
        return new UnresolvedReferenceDiagnostic(reference);
    }
}
