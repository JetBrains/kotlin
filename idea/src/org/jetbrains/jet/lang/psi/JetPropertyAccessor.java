package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

/**
 * @author max
 */
public class JetPropertyAccessor extends JetDeclaration implements JetDeclarationWithBody {
    public JetPropertyAccessor(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitPropertyAccessor(this);
    }

    public boolean isSetter() {
        return findChildByType(JetTokens.SET_KEYWORD) != null;
    }

    public boolean isGetter() {
        return findChildByType(JetTokens.GET_KEYWORD) != null;
    }

    @Nullable
    public JetParameter getParameter() {
        JetParameterList parameterList = (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
        if (parameterList == null) return null;
        List<JetParameter> parameters = parameterList.getParameters();
        if (parameters.isEmpty()) return null;
        return parameters.get(0);
    }

    @Nullable
    @Override
    public JetExpression getBodyExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    public boolean hasBlockBody() {
        return findChildByType(JetTokens.EQ) == null;
    }

    @NotNull
    @Override
    public JetElement asElement() {
        return this;
    }

    @Nullable
    public JetTypeReference getReturnTypeReference() {
        return findChildByClass(JetTypeReference.class);
    }
}
