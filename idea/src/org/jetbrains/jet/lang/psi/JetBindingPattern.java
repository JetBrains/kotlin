package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetBindingPattern extends JetPattern {
    public JetBindingPattern(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isVar() {
        return findChildByType(JetTokens.VAR_KEYWORD) != null;
    }



}
