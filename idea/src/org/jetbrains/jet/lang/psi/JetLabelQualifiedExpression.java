package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author abreslav
 */
public class JetLabelQualifiedExpression extends JetExpression {

    public JetLabelQualifiedExpression(@NotNull ASTNode node) {
        super(node);
    }
    
    @Nullable
    public JetSimpleNameExpression getTargetLabel() {
        JetContainerNode qualifier = (JetContainerNode) findChildByType(JetNodeTypes.LABEL_QUALIFIER);
        if (qualifier == null) return null;
        return (JetSimpleNameExpression) qualifier.findChildByType(JetNodeTypes.LABEL_REFERENCE);
    }

    @Nullable @IfNotParsed
    public JetExpression getLabeledExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Nullable
    public String getLabelName() {
        JetSimpleNameExpression labelElement = getTargetLabel();
        assert labelElement == null || labelElement.getText().startsWith("@");
        return labelElement == null ? null : labelElement.getText().substring(1);
    }
}
