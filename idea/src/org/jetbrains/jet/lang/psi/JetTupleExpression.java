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
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitTupleExpression(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitTupleExpression(this, data);
    }

    public List<JetExpression> getEntries() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetExpression.class);
    }
}
