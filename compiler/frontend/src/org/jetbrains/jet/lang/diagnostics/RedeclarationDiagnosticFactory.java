package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContext;

/**
* @author abreslav
*/
public class RedeclarationDiagnosticFactory extends AbstractDiagnosticFactory {

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
        PsiElement redeclaration = ((RedeclarationDiagnostic) diagnostic).getPsiElement();
        if (redeclaration instanceof JetNamedDeclaration) {
            PsiElement nameIdentifier = ((JetNamedDeclaration) redeclaration).getNameIdentifier();
            if (nameIdentifier != null) {
                return nameIdentifier.getTextRange();
            }
        }
        return redeclaration.getTextRange();
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
