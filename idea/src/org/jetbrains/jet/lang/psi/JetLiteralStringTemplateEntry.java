package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class JetLiteralStringTemplateEntry extends JetStringTemplateEntry {
    public JetLiteralStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitLiteralStringTemplateEntry(this);
    }

    @Override
    public <R, D> R visit(@NotNull JetExtendedVisitor<R, D> visitor, D data) {
        return visitor.visitLiteralStringTemplateEntry(this, data);
    }
}
