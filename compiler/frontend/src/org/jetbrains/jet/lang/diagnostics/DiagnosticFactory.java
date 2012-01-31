package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
* @author abreslav
*/
public interface DiagnosticFactory {
    @NotNull
    List<TextRange> getTextRanges(@NotNull Diagnostic diagnostic);

    @NotNull
    PsiFile getPsiFile(@NotNull Diagnostic diagnostic);
    
    @NotNull
    String getName();
}
