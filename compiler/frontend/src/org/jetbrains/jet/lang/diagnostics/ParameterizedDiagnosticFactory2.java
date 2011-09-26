package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class ParameterizedDiagnosticFactory2<A, B> extends DiagnosticFactoryWithPsiElement2<PsiElement, A,B> {
    public static <A, B> ParameterizedDiagnosticFactory2<A, B> create(Severity severity, String messageStub) {
        return new ParameterizedDiagnosticFactory2<A, B>(severity, messageStub);
    }

    public static <A, B> ParameterizedDiagnosticFactory2<A, B> create(Severity severity, String messageStub, Renderer renderer) {
        return new ParameterizedDiagnosticFactory2<A, B>(severity, messageStub, renderer);
    }

    public ParameterizedDiagnosticFactory2(Severity severity, String message, Renderer renderer) {
        super(severity, message, renderer);
    }

    public ParameterizedDiagnosticFactory2(Severity severity, String messageStub) {
        super(severity, messageStub);
    }

    @NotNull
    public Diagnostic on(@NotNull PsiFile psiFile, @NotNull TextRange range, @NotNull A a, @NotNull B b) {
        return new GenericDiagnostic(this, severity, makeMessage(a, b), psiFile, range);
    }

    @NotNull
    public Diagnostic on(@NotNull ASTNode node, @NotNull A a, @NotNull B b) {
        return on(DiagnosticUtils.getContainingFile(node), node.getTextRange(), a, b);
    }

}
