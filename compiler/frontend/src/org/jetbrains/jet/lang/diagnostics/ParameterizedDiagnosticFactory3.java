package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class ParameterizedDiagnosticFactory3<A, B, C> extends DiagnosticFactoryWithPsiElement3<PsiElement, A, B, C> {
    public static <A, B, C> ParameterizedDiagnosticFactory3<A, B, C> create(Severity severity, String messageStub) {
        return new ParameterizedDiagnosticFactory3<A, B, C>(severity, messageStub);
    }

    protected ParameterizedDiagnosticFactory3(Severity severity, String messageStub) {
        super(severity, messageStub);
    }

    @NotNull
    public Diagnostic on(@NotNull PsiFile psiFile, @NotNull TextRange range, @NotNull A a, @NotNull B b, @NotNull C c) {
        return new GenericDiagnostic(this, severity, makeMessage(a, b, c), psiFile, range);
    }

    @NotNull
    public Diagnostic on(@NotNull ASTNode node, @NotNull A a, @NotNull B b, @NotNull C c) {
        return on(DiagnosticUtils.getContainingFile(node), node.getTextRange(), a, b, c);
    }
}
