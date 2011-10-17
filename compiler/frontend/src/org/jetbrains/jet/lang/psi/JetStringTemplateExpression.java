package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class JetStringTemplateExpression extends JetExpression {
    public JetStringTemplateExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitStringTemplateExpression(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitStringTemplateExpression(this, data);
    }

    @NotNull
    public JetStringTemplateEntry[] getEntries() {
        return findChildrenByClass(JetStringTemplateEntry.class);
    }
}
