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
    
    private final String name;
    final Severity severity;
    private final String messagePrefix;

    public static final RedeclarationDiagnosticFactory REDECLARATION = new RedeclarationDiagnosticFactory(
            "REDECLARATION", Severity.ERROR, "Redeclaration: ");
    public static final RedeclarationDiagnosticFactory NAME_SHADOWING = new RedeclarationDiagnosticFactory(
            "NAME_SHADOWING", Severity.WARNING, "Name shadowed: ");

    public RedeclarationDiagnosticFactory(String name, Severity severity, String messagePrefix) {
        this.name = name;
        this.severity = severity;
        this.messagePrefix = messagePrefix;
    }

    public RedeclarationDiagnostic on(@NotNull PsiElement duplicatingElement, @NotNull String name) {
        return new RedeclarationDiagnostic.SimpleRedeclarationDiagnostic(duplicatingElement, name, this);
    }

    public Diagnostic on(DeclarationDescriptor duplicatingDescriptor, BindingContext contextToResolveToDeclaration) {
        return new RedeclarationDiagnostic.RedeclarationDiagnosticWithDeferredResolution(duplicatingDescriptor, contextToResolveToDeclaration, this);
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
        return name;
    }
    
    public String makeMessage(String identifier) {
        return messagePrefix + identifier;
    }

    @Override
    public String toString() {
        return getName();
    }
}
