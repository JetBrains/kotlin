package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetArrayAccessExpression extends JetReferenceExpression {
    public JetArrayAccessExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public PsiReference getReference() {
        JetContainerNode indicesNode = getIndicesNode();
        return indicesNode == null ? null : new JetArrayAccessReference();
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitArrayAccessExpression(this);
    }

    @NotNull
    public JetExpression getArrayExpression() {
        JetExpression baseExpression = findChildByClass(JetExpression.class);
        assert baseExpression != null;
        return baseExpression;
    }

    @NotNull
    public List<JetExpression> getIndexExpressions() {
        PsiElement container = getIndicesNode();
        if (container == null) return Collections.emptyList();
        return PsiTreeUtil.getChildrenOfTypeAsList(container, JetExpression.class);
    }

    public JetContainerNode getIndicesNode() {
        return (JetContainerNode) findChildByType(JetNodeTypes.INDICES);
    }

    private class JetArrayAccessReference extends JetPsiReference implements MultiRangeReference {

        @Override
        public PsiElement getElement() {
            return JetArrayAccessExpression.this;
        }

        @Override
        public TextRange getRangeInElement() {
            return getElement().getTextRange().shiftRight(-getElement().getTextOffset());
        }

        @Override
        public List<TextRange> getRanges() {
            List<TextRange> list = new ArrayList<TextRange>();

            JetContainerNode indices = getIndicesNode();
            TextRange textRange = indices.getNode().findChildByType(JetTokens.LBRACKET).getTextRange();
            TextRange lBracketRange = textRange.shiftRight(-getTextOffset());

            list.add(lBracketRange);

            textRange = indices.getNode().findChildByType(JetTokens.RBRACKET).getTextRange();
            TextRange rBracketRange = textRange.shiftRight(-getTextOffset());
            list.add(rBracketRange);

            return list;
        }
    }
}
