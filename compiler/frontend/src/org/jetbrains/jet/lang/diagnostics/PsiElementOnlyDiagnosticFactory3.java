package org.jetbrains.jet.lang.diagnostics;

import com.intellij.psi.PsiElement;
/**
 * @author svtk
 */
public class PsiElementOnlyDiagnosticFactory3<T extends PsiElement, A, B, C> extends DiagnosticFactoryWithPsiElement3<T, A, B, C> implements PsiElementOnlyDiagnosticFactory<T> {
    public static <T extends PsiElement, A, B, C> PsiElementOnlyDiagnosticFactory3<T, A, B, C> create(Severity severity, String messageStub) {
        return new PsiElementOnlyDiagnosticFactory3<T, A, B, C>(severity, messageStub);
    }

    public static <T extends PsiElement, A, B, C> PsiElementOnlyDiagnosticFactory3<T, A, B, C> create(Severity severity, String messageStub, Renderer renderer) {
        return new PsiElementOnlyDiagnosticFactory3<T, A, B, C>(severity, messageStub, renderer);
    }

    protected PsiElementOnlyDiagnosticFactory3(Severity severity, String messageStub) {
        super(severity, messageStub);
    }

    public PsiElementOnlyDiagnosticFactory3(Severity severity, String messageStub, Renderer renderer) {
        super(severity, messageStub, renderer);
    }
}
