package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetWhenConditionInRange extends JetWhenCondition {
    public JetWhenConditionInRange(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isNegated() {
        return getNode().findChildByType(JetTokens.NOT_IN) != null;
    }

    @Nullable @IfNotParsed
    public JetExpression getRangeExpression() {
        // Copied from JetBinaryExpression
        ASTNode node = getOperationReference().getNode().getTreeNext();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof JetExpression) {
                return (JetExpression) psi;
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitWhenConditionInRange(this);
    }

    public JetSimpleNameExpression getOperationReference() {
        return (JetSimpleNameExpression) findChildByType(JetNodeTypes.OPERATION_REFERENCE);
    }
}
