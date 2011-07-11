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
    public void accept(JetVisitor visitor) {
        visitor.visitStringTemplateExpression(this);
    }

    @NotNull
    public JetStringTemplateEntry[] getEntries() {
        return findChildrenByClass(JetStringTemplateEntry.class);
    }
}
