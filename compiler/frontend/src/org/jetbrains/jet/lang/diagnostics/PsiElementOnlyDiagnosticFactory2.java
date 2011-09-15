package org.jetbrains.jet.lang.diagnostics;

import com.intellij.psi.PsiElement;

/**
 * @author svtk
 */
public class PsiElementOnlyDiagnosticFactory2<T extends PsiElement, A, B> extends DiagnosticFactoryWithPsiElement2<T, A, B> implements PsiElementOnlyDiagnosticFactory {
    public static <T extends PsiElement, A, B> PsiElementOnlyDiagnosticFactory2<T, A, B> create(Severity severity, String messageStub) {
        return new PsiElementOnlyDiagnosticFactory2<T, A, B>(severity, messageStub);
    }

    protected PsiElementOnlyDiagnosticFactory2(Severity severity, String message) {
        super(severity, message);
    }
}
