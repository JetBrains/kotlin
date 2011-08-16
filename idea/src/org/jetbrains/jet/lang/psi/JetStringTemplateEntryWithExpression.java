package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public abstract class JetStringTemplateEntryWithExpression extends JetStringTemplateEntry {
    public JetStringTemplateEntryWithExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitStringTemplateEntryWithExpression(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitStringTemplateEntryWithExpression(this, data);
    }
}
