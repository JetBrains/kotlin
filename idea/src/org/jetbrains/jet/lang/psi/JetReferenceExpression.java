package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetReferenceExpression extends JetExpression {
    private static final TokenSet REFERENCE_TOKENS = TokenSet.create(JetTokens.IDENTIFIER, JetTokens.FIELD_IDENTIFIER);

    public JetReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable @IfNotParsed
    public String getReferencedName() {
        ASTNode node = getNode().findChildByType(REFERENCE_TOKENS);
        return node == null ? null : node.getText();
    }

    @Nullable @IfNotParsed
    public PsiElement getReferencedNameElement() {
        return findChildByType(REFERENCE_TOKENS);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitReferenceExpression(this);
    }

    @Override
    public PsiReference findReferenceAt(int offset) {
        return getReference();
    }

    @Override
    public PsiReference getReference() {
        if (getReferencedName() == null) return null;
        return new PsiReference() {
            @Override
            public PsiElement getElement() {
                return findChildByType(REFERENCE_TOKENS);
            }

            @Override
            public TextRange getRangeInElement() {
                return new TextRange(0, getElement().getTextLength());
            }

            @Override
            public PsiElement resolve() {
                PsiElement element = getElement();
                JetFile file = PsiTreeUtil.getParentOfType(element, JetFile.class);
                BindingContext bindingContext = AnalyzingUtils.analyzeFile(file, ErrorHandler.DO_NOTHING);
                PsiElement psiElement = bindingContext.resolveToDeclarationPsiElement(JetReferenceExpression.this);
                return psiElement == null
                        ? file
                        : psiElement;
            }

            @NotNull
            @Override
            public String getCanonicalText() {
                return getReferencedName();
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

}
