package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetIsExpression extends JetExpression {
    public JetIsExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitIsExpression(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitIsExpression(this, data);
    }

    @NotNull
    public JetExpression getLeftHandSide() {
        return findChildByClass(JetExpression.class);
    }

    @Nullable @IfNotParsed
    public JetPattern getPattern() {
        return findChildByClass(JetPattern.class);
    }

    @NotNull
    public JetSimpleNameExpression getOperationReference() {
        return (JetSimpleNameExpression) findChildByType(JetNodeTypes.OPERATION_REFERENCE);
    }

    public boolean isNegated() {
        return getOperationReference().getReferencedNameElementType() == JetTokens.NOT_IS;
    }

}
