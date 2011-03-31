package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetLabelQualifiedExpression extends JetExpression {

    public JetLabelQualifiedExpression(@NotNull ASTNode node) {
        super(node);
    }
    
    @Nullable
    public JetSimpleNameExpression getTargetLabel() {
        return (JetSimpleNameExpression) findChildByType(JetTokens.LABELS);
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
