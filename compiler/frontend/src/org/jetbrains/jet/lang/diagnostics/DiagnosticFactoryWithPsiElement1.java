package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author svtk
 */
public abstract class DiagnosticFactoryWithPsiElement1<T extends PsiElement, A> extends DiagnosticFactoryWithMessageFormat {

    protected DiagnosticFactoryWithPsiElement1(Severity severity, String message, Renderer renderer) {
        super(severity, message, renderer);
    }

    protected DiagnosticFactoryWithPsiElement1(Severity severity, String message) {
        super(severity, message);
    }

    protected String makeMessage(@NotNull A argument) {
        return messageFormat.format(new Object[]{makeMessageFor(argument)});
    }

    protected String makeMessageFor(A argument) {
        return renderer.render(argument);
    }

    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToMark, @NotNull A argument) {
        return on(elementToMark, elementToMark.getTextRange(), argument);
    }

    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull ASTNode nodeToMark, @NotNull A argument) {
        return on(elementToBlame, nodeToMark.getTextRange(), argument);
    }

    @NotNull
    public DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull PsiElement elementToMark, @NotNull A argument) {
        return on(elementToBlame, elementToMark.getTextRange(), argument);
    }
    
    @NotNull
    protected DiagnosticWithPsiElement<T> on(@NotNull T elementToBlame, @NotNull TextRange textRangeToMark, @NotNull A argument) {
        return new DiagnosticWithPsiElementImpl<T>(this, severity, makeMessage(argument), elementToBlame, textRangeToMark);
    }
}