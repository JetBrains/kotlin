package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetContinueExpression extends JetLabelQualifiedExpression {
    public JetContinueExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitContinueExpression(this);
    }

    @Nullable
    public String getLabelName() {
        PsiElement id = findChildByType(JetTokens.IDENTIFIER);
        return id != null ? id.getText() : null;
    }
}
