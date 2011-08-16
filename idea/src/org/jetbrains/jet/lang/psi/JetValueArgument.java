package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetValueArgument extends JetElement implements ValueArgumentPsi {
    public JetValueArgument(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitArgument(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitArgument(this, data);
    }

    @Nullable @IfNotParsed
    public JetExpression getArgumentExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    public PsiElement asElement() {
        return this;
    }

    @Nullable
    public String getArgumentName() {
        ASTNode firstChildNode = getNode().getFirstChildNode();
        if (firstChildNode == null) {
            return null;
        }
        return firstChildNode.getElementType() == JetTokens.IDENTIFIER ? firstChildNode.getText() : null;
    }

    public boolean isNamed() {
        return getArgumentName() != null;
    }

    public boolean isOut() {
        return findChildByType(JetTokens.OUT_KEYWORD) != null;
    }

    public boolean isRef() {
        return findChildByType(JetTokens.REF_KEYWORD) != null;
    }
}
