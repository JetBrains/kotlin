package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author svtk
 */
public class JetWhenConditionWithExpression extends JetWhenCondition {
    public JetWhenConditionWithExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    @IfNotParsed
    public JetExpressionPattern getPattern() {
        return findChildByClass(JetExpressionPattern.class);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitWhenConditionWithExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitWhenConditionExpression(this, data);
    }

}
