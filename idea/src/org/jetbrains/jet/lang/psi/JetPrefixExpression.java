package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author max
 */
public class JetPrefixExpression extends JetUnaryExpression {
    public JetPrefixExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitPrefixExpression(this);
    }

    @Nullable @IfNotParsed
    public JetExpression getBaseExpression() {
        PsiElement expression = getOperationSign().getNextSibling();
        while (expression != null && false == expression instanceof JetExpression) {
            expression = expression.getNextSibling();
        }
        return (JetExpression) expression;
    }
}
