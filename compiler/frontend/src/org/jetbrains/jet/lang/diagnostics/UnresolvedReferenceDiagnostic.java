package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

import static org.jetbrains.jet.lang.diagnostics.Severity.ERROR;

/**
* @author abreslav
*/
public class UnresolvedReferenceDiagnostic extends DiagnosticWithPsiElementImpl<JetReferenceExpression> {

    public UnresolvedReferenceDiagnostic(JetReferenceExpression referenceExpression, String message) {
        super(Errors.UNRESOLVED_REFERENCE, ERROR, message, referenceExpression);
    }

    @NotNull
    @Override
    public String getMessage() {
        return super.getMessage() + ": " + getPsiElement().getText();
    }
}
