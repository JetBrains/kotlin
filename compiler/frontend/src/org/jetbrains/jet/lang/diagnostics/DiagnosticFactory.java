package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public interface DiagnosticFactory {
    @NotNull
    TextRange getTextRange(@NotNull Diagnostic diagnostic);

    @NotNull
    PsiFile getPsiFile(@NotNull Diagnostic diagnostic);
    
    @NotNull
    String getName();
}
