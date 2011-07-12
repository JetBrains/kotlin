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
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitEscapeStringTemplateEntry(this);
    }

    public String getUnescapedValue() {
        return StringUtil.unescapeStringCharacters(getText());
    }
}
