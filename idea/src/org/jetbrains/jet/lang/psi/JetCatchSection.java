package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

import java.util.List;

/**
 * @author max
 */
public class JetCatchSection extends JetElement {
    public JetCatchSection(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitCatchSection(this);
    }

    @Nullable(IF_NOT_PARSED)
    public JetParameterList getParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
    }

    @Nullable(IF_NOT_PARSED)
    public JetParameter getCatchParameter() {
        JetParameterList list = getParameterList();
        if (list == null) return null;
        List<JetParameter> parameters = list.getParameters();
        return parameters.size() == 1 ? parameters.get(0) : null;
    }


    @Nullable(IF_NOT_PARSED)
    public JetExpression getCatchBody() {
        return findChildByClass(JetExpression.class);
    }
}
