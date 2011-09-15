package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public interface DiagnosticHolder {
    DiagnosticHolder DO_NOTHING = new DiagnosticHolder() {
        @Override
        public void report(@NotNull Diagnostic diagnostic) {
        }
    };
    DiagnosticHolder THROW_EXCEPTION = new DiagnosticHolder() {
        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic.getSeverity() == Severity.ERROR) {
                PsiFile psiFile = diagnostic.getFactory().getPsiFile(diagnostic);
                TextRange textRange = diagnostic.getFactory().getTextRange(diagnostic);
                throw new IllegalStateException(diagnostic.getMessage() + DiagnosticUtils.atLocation(psiFile, textRange));
            }
        }
    };

    void report(@NotNull Diagnostic diagnostic);
}
