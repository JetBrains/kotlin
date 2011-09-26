package org.jetbrains.jet.lang.diagnostics;

import com.intellij.psi.PsiElement;
/**
 * @author svtk
 */
public class PsiElementOnlyDiagnosticFactory1<T extends PsiElement, A> extends DiagnosticFactoryWithPsiElement1<T, A> implements PsiElementOnlyDiagnosticFactory<T> {
    public static <T extends PsiElement, A> PsiElementOnlyDiagnosticFactory1<T, A> create(Severity severity, String messageStub) {
        return new PsiElementOnlyDiagnosticFactory1<T, A>(severity, messageStub);
    }

    public static <T extends PsiElement, A> PsiElementOnlyDiagnosticFactory1<T, A> create(Severity severity, String messageStub, Renderer renderer) {
        return new PsiElementOnlyDiagnosticFactory1<T, A>(severity, messageStub, renderer);
    }

    public PsiElementOnlyDiagnosticFactory1(Severity severity, String message, Renderer renderer) {
        super(severity, message, renderer);
    }

    protected PsiElementOnlyDiagnosticFactory1(Severity severity, String message) {
        super(severity, message);
    }
}
