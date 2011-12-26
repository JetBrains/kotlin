package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
        return findChildByType(JetTokens.ARROW) != null;
    }

    @Override
    public JetBlockExpression getBodyExpression() {
        return (JetBlockExpression) super.getBodyExpression();
    }

    @NotNull
    public ASTNode getOpenBraceNode() {
        return getNode().findChildByType(JetTokens.LBRACE);
    }

    @Nullable
    @IfNotParsed
    public ASTNode getClosingBraceNode() {
        return getNode().findChildByType(JetTokens.RBRACE);
    }

    @Nullable
    public ASTNode getArrowNode() {
        return getNode().findChildByType(JetTokens.ARROW);
    }
}
