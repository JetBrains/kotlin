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
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitStringTemplateEntryWithExpression(this);
    }
}
