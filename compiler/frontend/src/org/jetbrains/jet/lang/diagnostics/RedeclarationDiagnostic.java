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
public interface RedeclarationDiagnostic extends DiagnosticWithPsiElement<PsiElement> {
    public class SimpleRedeclarationDiagnostic extends DiagnosticWithPsiElementImpl<PsiElement> implements RedeclarationDiagnostic {

        public SimpleRedeclarationDiagnostic(@NotNull PsiElement psiElement, @NotNull String name, RedeclarationDiagnosticFactory factory) {
            super(factory, factory.severity, factory.makeMessage(name), psiElement);
        }
    }

    public class RedeclarationDiagnosticWithDeferredResolution implements RedeclarationDiagnostic {
        
        private final DeclarationDescriptor duplicatingDescriptor;
        private final BindingContext contextToResolveToDeclaration;
        private final RedeclarationDiagnosticFactory factory;
        private PsiElement element;

        public RedeclarationDiagnosticWithDeferredResolution(@NotNull DeclarationDescriptor duplicatingDescriptor, @NotNull BindingContext contextToResolveToDeclaration, RedeclarationDiagnosticFactory factory) {
            this.duplicatingDescriptor = duplicatingDescriptor;
            this.contextToResolveToDeclaration = contextToResolveToDeclaration;
            this.factory = factory;
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
            return factory;
        }

        @NotNull
        @Override
        public String getMessage() {
            return factory.makeMessage(duplicatingDescriptor.getName());
        }

        @NotNull
        @Override
        public Severity getSeverity() {
            return factory.severity;
        }

        @Override
        public <P> DiagnosticWithPsiElement<PsiElement> add(DiagnosticParameter<P> parameterType, P parameter) {
            throw new UnsupportedOperationException();
        }
    }

}
