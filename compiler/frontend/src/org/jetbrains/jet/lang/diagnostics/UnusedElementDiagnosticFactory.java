package org.jetbrains.jet.lang.diagnostics;

import com.intellij.psi.PsiElement;
/**
 * @author svtk
 */
public class UnusedElementDiagnosticFactory<T extends PsiElement, A> extends PsiElementOnlyDiagnosticFactory1<T, A> {
    public static <T extends PsiElement, A> UnusedElementDiagnosticFactory<T, A> create(Severity severity, String messageStub) {
        return new UnusedElementDiagnosticFactory<T, A>(severity, messageStub);
    }

    public static <T extends PsiElement, A> UnusedElementDiagnosticFactory<T, A> create(Severity severity, String messageStub, Renderer renderer) {
        return new UnusedElementDiagnosticFactory<T, A>(severity, messageStub, renderer);
    }

    public UnusedElementDiagnosticFactory(Severity severity, String message, Renderer renderer) {
        super(severity, message, renderer);
    }

    protected UnusedElementDiagnosticFactory(Severity severity, String message) {
        super(severity, message);
    }
}
