package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

/**
* @author abreslav
*/
public class UnresolvedReferenceDiagnosticFactory implements DiagnosticFactory {
    public static final UnresolvedReferenceDiagnosticFactory INSTANCE = new UnresolvedReferenceDiagnosticFactory();

    public UnresolvedReferenceDiagnosticFactory() {}

    public UnresolvedReferenceDiagnostic on(@NotNull JetReferenceExpression reference) {
        return new UnresolvedReferenceDiagnostic(reference);
    }

    @NotNull
    @Override
    public TextRange getMarkerPosition(@NotNull Diagnostic diagnostic) {
        return ((UnresolvedReferenceDiagnostic) diagnostic).getTextRange();
    }

}
