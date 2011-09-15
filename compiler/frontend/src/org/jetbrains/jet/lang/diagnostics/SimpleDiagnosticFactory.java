package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class SimpleDiagnosticFactory extends DiagnosticFactoryWithSeverity {

    public static SimpleDiagnosticFactory create(Severity severity, String message) {
        return new SimpleDiagnosticFactory(severity, message);
    }

    protected final String message;

    public SimpleDiagnosticFactory(Severity severity, String message) {
        super(severity);
        this.message = message;
    }

    @NotNull
    public Diagnostic on(@NotNull PsiFile psiFile, @NotNull TextRange range) {
        return new GenericDiagnostic(this, severity, message, psiFile, range);
    }

    @NotNull
    public Diagnostic on(@NotNull ASTNode node) {
        return on(DiagnosticUtils.getContainingFile(node), node.getTextRange());
    }

    @NotNull
    public Diagnostic on(@NotNull PsiElement element) {
        return new DiagnosticWithPsiElement<PsiElement>(this, severity, message, element);
    }
}
