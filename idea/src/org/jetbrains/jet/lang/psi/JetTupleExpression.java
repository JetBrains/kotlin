package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author max
 */
public class JetTupleExpression extends JetExpression {
    public JetTupleExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitTupleExpression(this);
    }

    public List<JetExpression> getComponents() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetExpression.class);
    }
}
