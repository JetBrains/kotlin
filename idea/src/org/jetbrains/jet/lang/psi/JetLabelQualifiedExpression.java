package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetLabelQualifiedExpression extends JetExpression {

    public JetLabelQualifiedExpression(@NotNull ASTNode node) {
        super(node);
    }
    
    @Nullable
    public JetToken getTargetLabel() {
        ASTNode targetLabelNode = getTargetLabelNode();
        if (targetLabelNode == null) return null;
        return (JetToken) targetLabelNode.getElementType();
    }
    
    @Nullable
    public ASTNode getTargetLabelNode() {
        return getNode().findChildByType(JetTokens.LABELS);
    }
}
