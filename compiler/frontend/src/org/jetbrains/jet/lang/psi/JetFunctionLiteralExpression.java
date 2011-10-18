package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetFunctionLiteralExpression extends JetExpression implements JetDeclarationWithBody {
    public JetFunctionLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitFunctionLiteralExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitFunctionLiteralExpression(this, data);
    }

    @NotNull
    public JetFunctionLiteral getFunctionLiteral() {
        return (JetFunctionLiteral) findChildByType(JetNodeTypes.FUNCTION_LITERAL);
    }

    @NotNull
    @Override
    public List<JetParameter> getValueParameters() {
        return getFunctionLiteral().getValueParameters();
    }

    @Override
    public JetBlockExpression getBodyExpression() {
        return getFunctionLiteral().getBodyExpression();
    }

    @Override
    public boolean hasBlockBody() {
        return getFunctionLiteral().hasBlockBody();
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return getFunctionLiteral().getReturnTypeRef() != null;
    }

    @NotNull
    @Override
    public JetElement asElement() {
        return this;
    }

}
