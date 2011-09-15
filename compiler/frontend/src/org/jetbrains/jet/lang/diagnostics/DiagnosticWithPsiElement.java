package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public class DiagnosticWithPsiElement<T extends PsiElement> extends GenericDiagnostic {
    private final T psiElement;

    public DiagnosticWithPsiElement(DiagnosticFactory factory, Severity severity, String message, T psiElement) {
        this(factory, severity, message, psiElement, psiElement.getTextRange());
    }

    public DiagnosticWithPsiElement(DiagnosticFactory factory, Severity severity, String message, T psiElement, @NotNull TextRange textRange) {
        super(factory, severity, message, psiElement.getContainingFile(), textRange);
        this.psiElement = psiElement;
    }

    @NotNull
    public T getPsiElement() {
        return psiElement;
    }
}
