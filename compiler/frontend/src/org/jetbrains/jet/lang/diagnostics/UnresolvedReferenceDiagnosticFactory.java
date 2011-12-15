package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;

/**
* @author abreslav
*/
public class UnresolvedReferenceDiagnosticFactory extends AbstractDiagnosticFactory
        implements PsiElementOnlyDiagnosticFactory<JetSimpleNameExpression> {

    private final String message;

    public UnresolvedReferenceDiagnosticFactory(String message) {
        this.message = message;
    }

    public UnresolvedReferenceDiagnostic on(@NotNull JetReferenceExpression reference) {
        return new UnresolvedReferenceDiagnostic(reference, message);
    }
}
