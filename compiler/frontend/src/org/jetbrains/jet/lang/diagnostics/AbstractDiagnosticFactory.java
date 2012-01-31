package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class AbstractDiagnosticFactory implements DiagnosticFactory {
    
    private String name = null;
    
    /*package*/ void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public List<TextRange> getTextRanges(@NotNull Diagnostic diagnostic) {
        return Collections.singletonList(((DiagnosticWithTextRange) diagnostic).getTextRange());
    }

    @NotNull
    @Override
    public PsiFile getPsiFile(@NotNull Diagnostic diagnostic) {
        return ((DiagnosticWithTextRange) diagnostic).getPsiFile();
    }

    @Override
    public String toString() {
        return getName();
    }
}
