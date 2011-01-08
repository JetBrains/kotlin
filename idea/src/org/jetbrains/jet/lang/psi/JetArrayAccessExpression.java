package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetArrayAccessExpression extends JetExpression {
    public JetArrayAccessExpression(@NotNull ASTNode node) {
        super(node);
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
        PsiElement container = findChildByType(JetNodeTypes.INDICES);
        if (container == null) return Collections.emptyList();
        return PsiTreeUtil.getChildrenOfTypeAsList(container, JetExpression.class);
    }
}
