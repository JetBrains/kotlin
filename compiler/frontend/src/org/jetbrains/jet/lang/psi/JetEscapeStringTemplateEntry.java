package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class JetEscapeStringTemplateEntry extends JetStringTemplateEntry {
    public JetEscapeStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitEscapeStringTemplateEntry(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitEscapeStringTemplateEntry(this, data);
    }

    public String getUnescapedValue() {
        return StringUtil.unescapeStringCharacters(getText());
    }
}
