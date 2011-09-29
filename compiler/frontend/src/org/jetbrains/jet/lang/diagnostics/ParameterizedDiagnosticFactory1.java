package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class ParameterizedDiagnosticFactory1<A> extends DiagnosticFactoryWithPsiElement1<PsiElement, A> {
    public static <T> ParameterizedDiagnosticFactory1<T> create(Severity severity, String messageStub) {
        return new ParameterizedDiagnosticFactory1<T>(severity, messageStub);
    }

    public static <T> ParameterizedDiagnosticFactory1<T> create(Severity severity, String messageStub, Renderer renderer) {
        return new ParameterizedDiagnosticFactory1<T>(severity, messageStub, renderer);
    }

    public ParameterizedDiagnosticFactory1(Severity severity, String message, Renderer renderer) {
        super(severity, message, renderer);
    }

    public ParameterizedDiagnosticFactory1(Severity severity, String message) {
        super(severity, message);
    }

    @NotNull
    public Diagnostic on(@NotNull PsiFile psiFile, @NotNull TextRange rangeToMark, @NotNull A argument) {
        return new GenericDiagnostic(this, severity, makeMessage(argument), psiFile, rangeToMark);
    }

    @NotNull
    public Diagnostic on(@NotNull ASTNode nodeToMark, @NotNull A argument) {
        return on(DiagnosticUtils.getContainingFile(nodeToMark), nodeToMark.getTextRange(), argument);
    }
}
