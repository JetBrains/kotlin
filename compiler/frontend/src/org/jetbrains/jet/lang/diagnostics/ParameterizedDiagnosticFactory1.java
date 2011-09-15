package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class ParameterizedDiagnosticFactory1<T> extends AbstractDiagnosticFactory {
    public static <T> ParameterizedDiagnosticFactory1<T> create(Severity severity, String messageStub) {
        return new ParameterizedDiagnosticFactory1<T>(severity, messageStub);
    }

    public ParameterizedDiagnosticFactory1(Severity severity, String messageStub) {
        super(severity, messageStub);
    }

    private String makeMessage(@NotNull T argument) {
        return messageFormat.format(new Object[]{makeMessageFor(argument)});
    }

    protected String makeMessageFor(T argument) {
        return argument.toString();
    }

    @NotNull
    public Diagnostic on(@NotNull TextRange range, @NotNull T argument) {
        return new GenericDiagnostic(this, severity, makeMessage(argument), range);
    }

    @NotNull
    public Diagnostic on(@NotNull ASTNode node, @NotNull T argument) {
        return on(node.getTextRange(), argument);
    }

    @NotNull
    public Diagnostic on(@NotNull PsiElement element, @NotNull T argument) {
        return new DiagnosticWithPsiElement<PsiElement>(this, severity, makeMessage(argument), element);
    }
}
