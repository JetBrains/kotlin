package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;

import java.util.List;

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

    @NotNull
    @Override
    public List<TextRange> getTextRanges(@NotNull Diagnostic diagnostic) {
        if (diagnostic instanceof UnresolvedReferenceDiagnostic) {
            JetReferenceExpression element = ((UnresolvedReferenceDiagnostic) diagnostic).getPsiElement();
            if (element instanceof JetArrayAccessExpression) {
                return ((JetArrayAccessExpression)element).getBracketRanges();
            }
        }
        return super.getTextRanges(diagnostic);
    }
}
