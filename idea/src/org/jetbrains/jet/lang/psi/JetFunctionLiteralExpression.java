package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetFunctionLiteralExpression extends JetExpression {
    public JetFunctionLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitFunctionLiteralExpression(this);
    }

    //TODO: Due to the lack of multiple implementation inheritance these getters are copied from JetFunction
    @Nullable
    public JetParameterList getParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<JetParameter> getParameters() {
        JetParameterList list = getParameterList();
        return list != null ? list.getParameters() : Collections.<JetParameter>emptyList();
    }

    @NotNull
    public ASTNode getBodyNode() {
        ASTNode answer = getNode().findChildByType(JetNodeTypes.BODY);
        assert answer != null;
        return answer;
    }

    @NotNull
    public List<JetElement> getBody() {
        return PsiTreeUtil.getChildrenOfTypeAsList(getBodyNode().getPsi(), JetElement.class);
    }

    @Nullable
    public JetTypeReference getReceiverTypeRef() {
        PsiElement child = getFirstChild();
        while (child != null) {
            IElementType tt = child.getNode().getElementType();
            if (tt == JetTokens.LPAR || tt == JetTokens.COLON) break;
            if (child instanceof JetTypeReference) {
                return (JetTypeReference) child;
            }
            child = child.getNextSibling();
        }

        return null;
    }

    @Nullable
    public JetTypeReference getReturnTypeRef() {
        boolean colonPassed = false;
        PsiElement child = getFirstChild();
        while (child != null) {
            IElementType tt = child.getNode().getElementType();
            if (tt == JetTokens.COLON) {
                colonPassed = true;
            }
            if (colonPassed && child instanceof JetTypeReference) {
                return (JetTypeReference) child;
            }
            child = child.getNextSibling();
        }

        return null;
    }

    public boolean hasParameterSpecification() {
        return findChildByType(JetTokens.DOUBLE_ARROW) != null;
    }
}
