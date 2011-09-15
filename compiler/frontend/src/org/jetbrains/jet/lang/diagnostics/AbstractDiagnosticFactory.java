package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class AbstractDiagnosticFactory implements DiagnosticFactory {
    @NotNull
    @Override
    public TextRange getTextRange(@NotNull Diagnostic diagnostic) {
        return ((DiagnosticWithTextRange) diagnostic).getTextRange();
    }

    @NotNull
    @Override
    public PsiFile getPsiFile(@NotNull Diagnostic diagnostic) {
        return ((DiagnosticWithTextRange) diagnostic).getPsiFile();

    }
}
