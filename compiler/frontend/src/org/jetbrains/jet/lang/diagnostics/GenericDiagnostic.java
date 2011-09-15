package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
* @author abreslav
*/
public class GenericDiagnostic implements DiagnosticWithTextRange {

    private final TextRange textRange;
    private final String message;
    private final DiagnosticFactory factory;
    private final Severity severity;
    private final PsiFile psiFile;

    public GenericDiagnostic(DiagnosticFactory factory, Severity severity, String message, @NotNull PsiFile psiFile, @NotNull TextRange textRange) {
        this.factory = factory;
        this.textRange = textRange;
        this.severity = severity;
        this.message = message;
        this.psiFile = psiFile;
    }

    @NotNull
    @Override
    public DiagnosticFactory getFactory() {
        return factory;
    }

    @NotNull
    public TextRange getTextRange() {
        return textRange;
    }

    @NotNull
    @Override
    public PsiFile getPsiFile() {
        return psiFile;
    }

    @NotNull
    @Override
    public String getMessage() {
        return message;
    }

    @NotNull
    @Override
    public Severity getSeverity() {
        return severity;
    }
}
