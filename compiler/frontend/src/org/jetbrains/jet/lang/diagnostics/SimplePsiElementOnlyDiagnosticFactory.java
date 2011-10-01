package org.jetbrains.jet.lang.diagnostics;

import com.intellij.psi.PsiElement;

/**
 * @author svtk
 */
public class SimplePsiElementOnlyDiagnosticFactory<T extends PsiElement> extends SimpleDiagnosticFactoryWithPsiElement<T> implements PsiElementOnlyDiagnosticFactory<T> {

    public static <T extends PsiElement> SimplePsiElementOnlyDiagnosticFactory<T> create(Severity severity, String message) {
        return new SimplePsiElementOnlyDiagnosticFactory<T>(severity, message);
    }

    protected SimplePsiElementOnlyDiagnosticFactory(Severity severity, String message) {
        super(severity, message);
    }
}
