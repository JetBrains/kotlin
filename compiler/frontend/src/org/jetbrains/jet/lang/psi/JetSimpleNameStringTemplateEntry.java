package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class JetSimpleNameStringTemplateEntry extends JetStringTemplateEntryWithExpression
{
    public JetSimpleNameStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitSimpleNameStringTemplateEntry(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitSimpleNameStringTemplateEntry(this, data);
    }
}
