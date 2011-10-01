package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public class DiagnosticWithPsiElementImpl<T extends PsiElement> extends GenericDiagnostic implements DiagnosticWithPsiElement<T> {
    private final T psiElement;

    public DiagnosticWithPsiElementImpl(DiagnosticFactory factory, Severity severity, String message, T psiElement) {
        this(factory, severity, message, psiElement, psiElement.getTextRange());
    }

    public DiagnosticWithPsiElementImpl(DiagnosticFactory factory, Severity severity, String message, T psiElement, @NotNull TextRange textRange) {
        super(factory, severity, message, psiElement.getContainingFile(), textRange);
        this.psiElement = psiElement;
    }

    @Override
    @NotNull
    public T getPsiElement() {
        return psiElement;
    }

    @Override
    public <P> DiagnosticWithPsiElement<T> add(DiagnosticParameter<P> parameterType, P parameter) {
        return (new DiagnosticWithParameters<T>(this)).add(parameterType, parameter);
    }
}
