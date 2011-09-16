package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;

/**
* @author abreslav
*/
public class RedeclarationDiagnosticFactory implements DiagnosticFactory {

    public static final RedeclarationDiagnosticFactory INSTANCE = new RedeclarationDiagnosticFactory();

    public RedeclarationDiagnosticFactory() {}

    public RedeclarationDiagnostic on(@NotNull PsiElement duplicatingElement) {
        return new RedeclarationDiagnostic.SimpleRedeclarationDiagnostic(duplicatingElement);
    }

    public Diagnostic on(DeclarationDescriptor duplicatingDescriptor, BindingContext contextToResolveToDeclaration) {
        return new RedeclarationDiagnostic.RedeclarationDiagnosticWithDeferredResolution(duplicatingDescriptor, contextToResolveToDeclaration);
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
