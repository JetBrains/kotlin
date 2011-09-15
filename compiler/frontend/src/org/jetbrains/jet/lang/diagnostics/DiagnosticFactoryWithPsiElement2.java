package org.jetbrains.jet.lang.diagnostics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class DiagnosticFactoryWithPsiElement2<A, B> extends DiagnosticFactoryWithMessageFormat {
    
    public static <A, B> DiagnosticFactoryWithPsiElement2<A, B> create(Severity severity, String message) {
        return new DiagnosticFactoryWithPsiElement2<A, B>(severity, message);
    }
    
    public DiagnosticFactoryWithPsiElement2(Severity severity, String message) {
        super(severity, message);
    }

    protected String makeMessage(@NotNull A a, @NotNull B b) {
        return messageFormat.format(new Object[] {makeMessageForA(a), makeMessageForB(b)});
    }

    protected String makeMessageForA(@NotNull A a) {
        return a.toString();
    }

    protected String makeMessageForB(@NotNull B b) {
        return b.toString();
    }

    @NotNull
    public Diagnostic on(@NotNull PsiElement element, @NotNull A a, @NotNull B b) {
        return new DiagnosticWithPsiElement<PsiElement>(this, severity, makeMessage(a, b), element);
    }
}
