package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author abreslav
 */
public class JetValueArgumentName extends JetElement {
    public JetValueArgumentName(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable @IfNotParsed
    public JetSimpleNameExpression getReferenceExpression() {
        return (JetSimpleNameExpression) findChildByType(JetNodeTypes.REFERENCE_EXPRESSION);
    }


}
