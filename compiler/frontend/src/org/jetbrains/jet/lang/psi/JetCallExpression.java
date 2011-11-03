package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetCallExpression extends JetExpression implements JetCallElement {
    public JetCallExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitCallExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitCallExpression(this, data);
    }

    @Override
    @Nullable
    public JetExpression getCalleeExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    @Nullable
    public JetValueArgumentList getValueArgumentList() {
        return (JetValueArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @Nullable
    public JetTypeArgumentList getTypeArgumentList() {
        return (JetTypeArgumentList) findChildByType(JetNodeTypes.TYPE_ARGUMENT_LIST);
    }

    @Override
    @NotNull
    public List<JetExpression> getFunctionLiteralArguments() {
        JetExpression calleeExpression = getCalleeExpression();
        ASTNode node;
        if (calleeExpression instanceof JetFunctionLiteralExpression) {
            node = calleeExpression.getNode().getTreeNext();
        }
        else {
            node = getNode().getFirstChildNode();
        }
        List<JetExpression> result = new SmartList<JetExpression>();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof JetFunctionLiteralExpression) {
                result.add((JetFunctionLiteralExpression) psi);
            }
            else if (psi instanceof JetPrefixExpression) {
                JetPrefixExpression prefixExpression = (JetPrefixExpression) psi;
                if (JetTokens.LABELS.contains(prefixExpression.getOperationSign().getReferencedNameElementType())) {
                    JetExpression labeledExpression = prefixExpression.getBaseExpression();
                    if (labeledExpression instanceof JetFunctionLiteralExpression) {
                        result.add(labeledExpression);
                    }
                }
            }
            node = node.getTreeNext();
        }
        return result;
    }

    @Override
    @NotNull
    public List<? extends ValueArgument> getValueArguments() {
        JetValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<JetValueArgument>emptyList();
    }

    @NotNull
    public List<JetTypeProjection> getTypeArguments() {
        JetTypeArgumentList list = getTypeArgumentList();
        return list != null ? list.getArguments() : Collections.<JetTypeProjection>emptyList();
    }
}
