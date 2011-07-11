package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author abreslav
 */
public class JetSimpleNameStringTemplateEntry extends JetStringTemplateEntryWithExpression
{
    public JetSimpleNameStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitSimpleNameStringTemplateEntry(this);
    }
}
