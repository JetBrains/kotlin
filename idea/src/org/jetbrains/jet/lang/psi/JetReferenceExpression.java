package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;

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
        PsiElement psiElement = bindingContext.resolveToDeclarationPsiElement(this);
        return psiElement == null
                ? file
                : psiElement;
    }

    @Override
    public abstract PsiReference getReference();

    protected abstract class JetPsiReference implements PsiReference {

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
}
