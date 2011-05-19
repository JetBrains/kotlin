package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetTuplePatternEntry extends JetElement {
    public JetTuplePatternEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public ASTNode getNameLabelNode() {
        return getNode().findChildByType(JetTokens.IDENTIFIER);
    }

    @Nullable
    public String getNameLabel() {
        ASTNode nameLabelNode = getNameLabelNode();
        return nameLabelNode == null ? null : nameLabelNode.getText();
    }

    @Nullable @IfNotParsed
    public JetPattern getPattern() {
        return findChildByClass(JetPattern.class);
    }
}
