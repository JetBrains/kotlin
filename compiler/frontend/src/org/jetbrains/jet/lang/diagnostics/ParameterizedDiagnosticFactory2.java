package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class ParameterizedDiagnosticFactory2<A, B> extends PsiElementOnlyDiagnosticFactory2<A,B> {
    public static <A, B> ParameterizedDiagnosticFactory2<A, B> create(Severity severity, String messageStub) {
        return new ParameterizedDiagnosticFactory2<A, B>(severity, messageStub);
    }

    public ParameterizedDiagnosticFactory2(Severity severity, String messageStub) {
        super(severity, messageStub);
    }

    @NotNull
    public Diagnostic on(@NotNull TextRange range, @NotNull A a, @NotNull B b) {
        return new GenericDiagnostic(this, severity, makeMessage(a, b), range);
    }

    @NotNull
    public Diagnostic on(@NotNull ASTNode node, @NotNull A a, @NotNull B b) {
        return on(node.getTextRange(), a, b);
    }

}
