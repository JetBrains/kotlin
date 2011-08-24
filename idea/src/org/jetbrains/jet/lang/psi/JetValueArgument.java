package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetValueArgument extends JetElement implements ValueArgument {
    public JetValueArgument(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitArgument(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitArgument(this, data);
    }

    @Override
    @Nullable @IfNotParsed
    public JetExpression getArgumentExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    @Nullable
    public JetValueArgumentName getArgumentName() {
        return (JetValueArgumentName) findChildByType(JetNodeTypes.VALUE_ARGUMENT_NAME);
    }

    @Override
    public boolean isNamed() {
        return getArgumentName() != null;
    }

    @Override
    public boolean isOut() {
        return findChildByType(JetTokens.OUT_KEYWORD) != null;
    }

    @Override
    public boolean isRef() {
        return findChildByType(JetTokens.REF_KEYWORD) != null;
    }

    @NotNull
    @Override
    public PsiElement asElement() {
        return this;
    }
}
