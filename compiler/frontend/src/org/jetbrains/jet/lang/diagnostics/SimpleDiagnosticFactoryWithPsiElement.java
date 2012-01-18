package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public abstract class SimpleDiagnosticFactoryWithPsiElement<T extends PsiElement> extends DiagnosticFactoryWithSeverity {

    protected final String message;

    protected SimpleDiagnosticFactoryWithPsiElement(Severity severity, String message) {
        super(severity);
        this.message = message;
    }
    
    private Diagnostic on(@NotNull T elementToBlame, @NotNull TextRange textRange) {
        return new DiagnosticWithPsiElementImpl<T>(this, severity, message, elementToBlame, textRange);
    }

    @NotNull
    public Diagnostic on(@NotNull T elementToBlame, @NotNull ASTNode nodeToMark) {
        return on(elementToBlame, nodeToMark.getTextRange());
    }

    @NotNull
    public Diagnostic on(@NotNull T elementToBlame, @NotNull PsiElement elementToMark) {
        return on(elementToBlame, elementToMark.getTextRange());
    }

    @NotNull
    public Diagnostic on(@NotNull T element) {
        return on(element, getTextRange(element));
    }
    
    @NotNull
    public TextRange getTextRange(@NotNull T element) {
        return element.getTextRange();
    }
}