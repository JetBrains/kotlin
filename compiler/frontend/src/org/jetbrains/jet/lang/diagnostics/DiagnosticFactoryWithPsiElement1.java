package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public abstract class DiagnosticFactoryWithPsiElement1<T extends PsiElement, A> extends DiagnosticFactoryWithMessageFormat {
    protected DiagnosticFactoryWithPsiElement1(Severity severity, String message) {
        super(severity, message);
    }

    protected String makeMessage(@NotNull A argument) {
        return messageFormat.format(new Object[]{makeMessageFor(argument)});
    }

    protected String makeMessageFor(A argument) {
        return argument.toString();
    }

    @NotNull
    public Diagnostic on(@NotNull T element, @NotNull A argument) {
        return on(element, element.getNode(), argument);
    }

    @NotNull
    public Diagnostic on(@NotNull T element, @NotNull ASTNode node, @NotNull A argument) {
        return new DiagnosticWithPsiElement<T>(this, severity, makeMessage(argument), element, node.getTextRange());
    }

    @NotNull
    public Diagnostic on(@NotNull T element, @NotNull PsiElement psiElement, @NotNull A argument) {
        return new DiagnosticWithPsiElement<T>(this, severity, makeMessage(argument), element, psiElement.getTextRange());
    }
}