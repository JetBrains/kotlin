package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public class JetIsExpression extends JetExpression {
    public JetIsExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitIsExpression(this);
    }

    @NotNull
    public JetExpression getLeftHandSide() {
        return findChildByClass(JetExpression.class);
    }

    @Nullable @IfNotParsed
    public JetPattern getPattern() {
        return findChildByClass(JetPattern.class);
    }

}
