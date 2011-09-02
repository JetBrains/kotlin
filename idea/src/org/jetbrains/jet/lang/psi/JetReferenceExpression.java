package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.DESCRIPTOR_TO_DECLARATION;

/**
 * @author abreslav
 */
public abstract class JetReferenceExpression extends JetExpression {
    public JetReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }

    protected PsiElement doResolve() {
        JetFile file = (JetFile) getContainingFile();
        BindingContext bindingContext = AnalyzingUtils.analyzeFileWithCache(file);
        PsiElement psiElement = BindingContextUtils.resolveToDeclarationPsiElement(bindingContext, this);
        if (psiElement != null) {
            return psiElement;
        }
        Collection<? extends DeclarationDescriptor> declarationDescriptors = bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, this);
        if (declarationDescriptors != null) return null;
        return file;
    }

    protected ResolveResult[] doMultiResolve() {
        JetFile file = (JetFile) getContainingFile();
        BindingContext bindingContext = AnalyzingUtils.analyzeFileWithCache(file);
        Collection<? extends DeclarationDescriptor> declarationDescriptors = bindingContext.get(AMBIGUOUS_REFERENCE_TARGET, this);
        assert declarationDescriptors != null;
        ResolveResult[] results = new ResolveResult[declarationDescriptors.size()];
        int i = 0;
        for (DeclarationDescriptor descriptor : declarationDescriptors) {
            PsiElement element = bindingContext.get(DESCRIPTOR_TO_DECLARATION, descriptor);
            if (element != null) {
                results[i] = new PsiElementResolveResult(element, true);
                i++;
            }
        }
        return results;
    }

    @Override
    public abstract PsiReference getReference();

    protected abstract class JetPsiReference implements PsiPolyVariantReference {

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean incompleteCode) {
            return doMultiResolve();
        }

        @Override
        public PsiElement resolve() {
            return doResolve();
        }

        @NotNull
        @Override
        public String getCanonicalText() {
            return "<TBD>";
        }

        @Override
        public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
            throw new IncorrectOperationException();
        }

        @Override
        public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
            throw new IncorrectOperationException();
        }

        @Override
        public boolean isReferenceTo(PsiElement element) {
            return resolve() == element;
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return EMPTY_ARRAY;
        }

        @Override
        public boolean isSoft() {
            return false;
        }
    }

//    protected abstract class JetPsiMultiReference extends JetPsiReference implements PsiPolyVariantReference {
//        @NotNull
//        @Override
//        public abstract ResolveResult[] multiResolve(boolean incompleteCode);
//    }
}
