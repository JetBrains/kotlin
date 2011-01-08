package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeType;

/**
 * @author max
 */
public class JetExpression extends JetElement {
    public JetExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitExpression(this);
    }


    JetExpression findExpressionUnder(JetNodeType type) {
        PsiElement containerNode = findChildByType(type);
        if (containerNode == null) return null;

        return PsiTreeUtil.findChildOfType(containerNode, JetExpression.class);
    }
}
