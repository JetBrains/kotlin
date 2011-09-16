package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class ParameterizedDiagnosticFactory3<A, B, C> extends DiagnosticFactoryWithMessageFormat {
    public static <A, B, C> ParameterizedDiagnosticFactory3<A, B, C> create(Severity severity, String messageStub) {
        return new ParameterizedDiagnosticFactory3<A, B, C>(severity, messageStub);
    }

    public ParameterizedDiagnosticFactory3(Severity severity, String messageStub) {
        super(severity, messageStub);
    }

    protected String makeMessage(@NotNull A a, @NotNull B b, @NotNull C c) {
        return messageFormat.format(new Object[]{makeMessageForA(a), makeMessageForB(b), makeMessageForC(c)});
    }

    protected String makeMessageForA(@NotNull A a) {
        return a.toString();
    }

    protected String makeMessageForB(@NotNull B b) {
        return b.toString();
    }

    protected String makeMessageForC(@NotNull C c) {
        return c.toString();
    }

    @NotNull
    public Diagnostic on(@NotNull PsiFile psiFile, @NotNull TextRange range, @NotNull A a, @NotNull B b, @NotNull C c) {
        return new GenericDiagnostic(this, severity, makeMessage(a, b, c), psiFile, range);
    }

    @NotNull
    public Diagnostic on(@NotNull ASTNode node, @NotNull A a, @NotNull B b, @NotNull C c) {
        return on(DiagnosticUtils.getContainingFile(node), node.getTextRange(), a, b, c);
    }

    @NotNull
    public Diagnostic on(@NotNull PsiElement element, @NotNull A a, @NotNull B b, @NotNull C c) {
        return new DiagnosticWithPsiElementImpl<PsiElement>(this, severity, makeMessage(a, b, c), element);
    }
}
