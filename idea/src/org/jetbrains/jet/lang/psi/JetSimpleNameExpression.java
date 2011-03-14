package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.parsing.JetExpressionParsing;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetSimpleNameExpression extends JetReferenceExpression {
    private static final TokenSet REFERENCE_TOKENS = TokenSet.create(JetTokens.IDENTIFIER, JetTokens.FIELD_IDENTIFIER);

    public JetSimpleNameExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable @IfNotParsed
    public String getReferencedName() {
        PsiElement referencedNameElement = getReferencedNameElement();
        return referencedNameElement == null ? null : referencedNameElement.getNode().getText();
    }

    @Nullable @IfNotParsed
    public PsiElement getReferencedNameElement() {
        PsiElement element = findChildByType(REFERENCE_TOKENS);
        if (element == null) {
            element = findChildByType(JetExpressionParsing.ALL_OPERATIONS);
        }
        return element;
    }

    @Nullable @IfNotParsed
    public IElementType getReferencedNameElementType() {
        PsiElement element = getReferencedNameElement();
        return element == null ? null : element.getNode().getElementType();
    }

    @Override
    public PsiReference findReferenceAt(int offset) {
        return getReference();
    }

    @Override
    public PsiReference getReference() {
        return new JetPsiReference() {

            @Override
            public PsiElement getElement() {
                return getReferencedNameElement();
            }

            @Override
            public TextRange getRangeInElement() {
                return new TextRange(0, getElement().getTextLength());
            }
        };
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitReferenceExpression(this);
    }

}
