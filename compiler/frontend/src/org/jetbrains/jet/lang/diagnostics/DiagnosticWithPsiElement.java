package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public class DiagnosticWithPsiElement<T extends PsiElement> extends GenericDiagnostic {
    private final T psiElement;

    public DiagnosticWithPsiElement(DiagnosticFactory factory, Severity severity, String message, T psiElement) {
        super(factory, severity, message, psiElement.getTextRange());
        this.psiElement = psiElement;
    }

    @NotNull
    public T getPsiElement() {
        return psiElement;
    }
}
