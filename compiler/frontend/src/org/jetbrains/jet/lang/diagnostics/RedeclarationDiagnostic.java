package org.jetbrains.jet.lang.diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;

import static org.jetbrains.jet.lang.diagnostics.Severity.ERROR;

/**
 * @author abreslav
 */
public interface RedeclarationDiagnostic extends DiagnosticWithPsiElement<PsiElement> {
    public class SimpleRedeclarationDiagnostic extends DiagnosticWithPsiElementImpl<PsiElement> implements RedeclarationDiagnostic {

        public SimpleRedeclarationDiagnostic(@NotNull PsiElement psiElement) {
            super(RedeclarationDiagnosticFactory.INSTANCE, ERROR, "Redeclaration", psiElement);
        }
    }

    public class RedeclarationDiagnosticWithDeferredResolution implements RedeclarationDiagnostic {
        
        private final DeclarationDescriptor duplicatingDescriptor;
        private final BindingContext contextToResolveToDeclaration;
        private PsiElement element;

        public RedeclarationDiagnosticWithDeferredResolution(@NotNull DeclarationDescriptor duplicatingDescriptor, @NotNull BindingContext contextToResolveToDeclaration) {
            this.duplicatingDescriptor = duplicatingDescriptor;
            this.contextToResolveToDeclaration = contextToResolveToDeclaration;
        }

        private PsiElement resolve() {
            if (element == null) {
                element = contextToResolveToDeclaration.get(BindingContext.DESCRIPTOR_TO_DECLARATION, duplicatingDescriptor);
                assert element != null : "No element for descriptor: " + duplicatingDescriptor;
                }
            return element;
        }
        
        @NotNull
        @Override
        public PsiElement getPsiElement() {
            return resolve();
        }

        @NotNull
        @Override
        public TextRange getTextRange() {
            return resolve().getTextRange();
        }

        @NotNull
        @Override
        public PsiFile getPsiFile() {
            return resolve().getContainingFile();
        }

        @NotNull
        @Override
        public DiagnosticFactory getFactory() {
            return Errors.REDECLARATION;
        }

        @NotNull
        @Override
        public String getMessage() {
            return "Redeclaration";
        }

        @NotNull
        @Override
        public Severity getSeverity() {
            return ERROR;
        }

        @Override
        public <P> DiagnosticWithPsiElement<PsiElement> add(DiagnosticParameter<P> parameterType, P parameter) {
            throw new UnsupportedOperationException();
        }
    }

}
