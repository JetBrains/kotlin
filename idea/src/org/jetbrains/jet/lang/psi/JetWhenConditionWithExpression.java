package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public class JetWhenConditionWithExpression extends JetWhenCondition {
    public JetWhenConditionWithExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    @IfNotParsed
    public JetExpression getExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitWhenConditionWithExpression(this);
    }
}
