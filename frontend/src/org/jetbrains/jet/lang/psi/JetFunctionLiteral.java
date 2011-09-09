package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetFunctionLiteral extends JetFunction {
    public JetFunctionLiteral(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public boolean hasBlockBody() {
        return false;
    }

    @Override
    public String getName() {
        return "<anonymous>";
    }

    @Override
    public PsiElement getNameIdentifier() {
        return null;
    }

    public boolean hasParameterSpecification() {
        return findChildByType(JetTokens.DOUBLE_ARROW) != null;
    }

    @Override
    public JetBlockExpression getBodyExpression() {
        return (JetBlockExpression) super.getBodyExpression();
    }
}
