package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public abstract class JetNamedDeclaration extends JetDeclaration implements PsiNameIdentifierOwner {
    public JetNamedDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getText() : null;
    }

    @Override
    public PsiElement getNameIdentifier() {
        return findChildByType(JetTokens.IDENTIFIER);
    }

    @Override
    public PsiReference getReference() {
        return new PsiReference() {
            @Override
            public PsiElement getElement() {
                return getNameIdentifier();
            }

            @Override
            public TextRange getRangeInElement() {
                PsiElement element = getElement();
                if (element == null) return null;
                return element.getTextRange().shiftRight(element.getTextOffset());
            }

            @Override
            public PsiElement resolve() {
                return PsiTreeUtil.getParentOfType(JetNamedDeclaration.this, JetFile.class);
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
        };
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        return getNameIdentifier().replace(JetChangeUtil.createNameIdentifier(getProject(), name));
    }
}
