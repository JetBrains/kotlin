package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetConstructor extends JetDeclaration implements JetDeclarationWithBody {
    public JetConstructor(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitConstructor(this);
    }

    @Nullable @IfNotParsed
    public JetParameterList getParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<JetParameter> getParameters() {
        JetParameterList list = getParameterList();
        return list != null ? list.getParameters() : Collections.<JetParameter>emptyList();
    }

    @Nullable
    public JetInitializerList getInitializerList() {
        return (JetInitializerList) findChildByType(JetNodeTypes.INITIALIZER_LIST);
    }

    @NotNull
    public List<JetDelegationSpecifier> getInitializers() {
        JetInitializerList list = getInitializerList();
        return list != null ? list.getInitializers() : Collections.<JetDelegationSpecifier>emptyList();
    }

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

    public ASTNode getNameNode() {
        return getNode().findChildByType(JetTokens.THIS_KEYWORD);
    }
}
