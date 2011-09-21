package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public class DiagnosticWithAdditionalInfoFactory1<T extends PsiElement, A> extends PsiElementOnlyDiagnosticFactory1<T, A> {
    public static <T extends PsiElement, A> DiagnosticWithAdditionalInfoFactory1<T, A> create(Severity severity, String messageStub) {
        return new DiagnosticWithAdditionalInfoFactory1<T, A>(severity, messageStub);
    }

    protected DiagnosticWithAdditionalInfoFactory1(Severity severity, String message) {
        super(severity, message);
    }
    
    @NotNull
    @Override
    protected Diagnostic on(@NotNull T element, @NotNull TextRange textRange, @NotNull A argument) {
        return new DiagnosticWithAdditionalInfo<T, A>(this, severity, makeMessage(argument), element, textRange, argument);
    }
}
