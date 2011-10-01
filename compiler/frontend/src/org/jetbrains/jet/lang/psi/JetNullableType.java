package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

/**
 * @author max
 */
public class JetNullableType extends JetTypeElement {
    public JetNullableType(@NotNull ASTNode node) {
        super(node);
    }
    
    @NotNull
    public ASTNode getQuestionMarkNode() {
        return getNode().findChildByType(JetTokens.QUEST);
    }

    @NotNull
    @Override
    public List<JetTypeReference> getTypeArgumentsAsTypes() {
        return getInnerType().getTypeArgumentsAsTypes();
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitNullableType(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitNullableType(this, data);
    }

    @NotNull
    public JetTypeElement getInnerType() {
        return findChildByClass(JetTypeElement.class);
    }
}
