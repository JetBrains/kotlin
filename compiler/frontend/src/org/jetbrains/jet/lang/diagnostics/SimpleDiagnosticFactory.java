package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class SimpleDiagnosticFactory implements DiagnosticFactory {

    public static SimpleDiagnosticFactory create(Severity severity, String message) {
        return new SimpleDiagnosticFactory(severity, message);
    }

    protected final String message;
    protected final Severity severity;

    public SimpleDiagnosticFactory(Severity severity, String message) {
        this.message = message;
        this.severity = severity;
    }

    @NotNull
    public Diagnostic on(@NotNull TextRange range) {
        return new GenericDiagnostic(this, severity, message, range);
    }

    @NotNull
    public Diagnostic on(@NotNull ASTNode node) {
        return on(node.getTextRange());
    }

    @NotNull
    public Diagnostic on(@NotNull PsiElement element) {
        return new DiagnosticWithPsiElement<PsiElement>(this, severity, message, element);
    }

    @NotNull
    @Override
    public TextRange getMarkerPosition(@NotNull Diagnostic diagnostic) {
        return ((DiagnosticWithTextRange) diagnostic).getTextRange();
    }
}
