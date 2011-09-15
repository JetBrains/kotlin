package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

/**
* @author abreslav
*/
public class RedeclarationDiagnosticFactory implements DiagnosticFactory {

    public static final RedeclarationDiagnosticFactory INSTANCE = new RedeclarationDiagnosticFactory();

    public RedeclarationDiagnosticFactory() {}

    public RedeclarationDiagnostic on(DeclarationDescriptor a, DeclarationDescriptor b) {
        return new RedeclarationDiagnostic(a, b);
    }

    @NotNull
    @Override
    public TextRange getTextRange(@NotNull Diagnostic diagnostic) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public PsiFile getPsiFile(@NotNull Diagnostic diagnostic) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public String getName() {
        return "REDECLARATION";
    }

    @Override
    public String toString() {
        return getName();
    }
}
