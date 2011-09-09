package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetNamedArgumentImpl extends JetValueArgument {
    public JetNamedArgumentImpl(@NotNull ASTNode node) {
        super(node);
    }

    public String getParameterName() {
        return getParameterNameNode().getText();
    }

    private ASTNode getParameterNameNode() {
        return getNode().findChildByType(JetTokens.IDENTIFIER);
    }
}
