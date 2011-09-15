package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class ParameterizedDiagnosticFactory2<A, B> extends AbstractDiagnosticFactory {
    public static <A, B> ParameterizedDiagnosticFactory2<A, B> create(Severity severity, String messageStub) {
        return new ParameterizedDiagnosticFactory2<A, B>(severity, messageStub);
    }

    public ParameterizedDiagnosticFactory2(Severity severity, String messageStub) {
        super(severity, messageStub);
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
    public Diagnostic on(@NotNull TextRange range, @NotNull A a, @NotNull B b) {
        return new GenericDiagnostic(this, severity, makeMessage(a, b), range);
    }

    @NotNull
    public Diagnostic on(@NotNull ASTNode node, @NotNull A a, @NotNull B b) {
        return on(node.getTextRange(), a, b);
    }

    @NotNull
    public Diagnostic on(@NotNull PsiElement element, @NotNull A a, @NotNull B b) {
        return new DiagnosticWithPsiElement<PsiElement>(this, severity, makeMessage(a, b), element);
    }
}
