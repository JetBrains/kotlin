package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public class DiagnosticWithAdditionalInfo<T extends PsiElement, I> extends DiagnosticWithPsiElementImpl<T> {
    private final I info;

    public DiagnosticWithAdditionalInfo(DiagnosticFactory factory, Severity severity, String message, T psiElement, I info) {
        super(factory, severity, message, psiElement);
        this.info = info;
    }

    public DiagnosticWithAdditionalInfo(DiagnosticFactory factory, Severity severity, String message, T psiElement, @NotNull TextRange textRange, I info) {
        super(factory, severity, message, psiElement, textRange);
        this.info = info;
    }

    public I getInfo() {
        return info;
    }
}
