package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

/**
 * @author abreslav
 */
public class JetBindingPattern extends JetPattern {
    public JetBindingPattern(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public JetProperty getVariableDeclaration() {
        return (JetProperty) findChildByType(JetNodeTypes.PROPERTY);
    }

    @Nullable
    public JetWhenCondition getCondition() {
        return findChildByClass(JetWhenCondition.class);
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitBindingPattern(this);
    }
}
