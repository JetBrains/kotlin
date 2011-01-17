package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author max
 */
public class JetTypeParameter extends JetNamedDeclaration {
    public JetTypeParameter(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(JetVisitor visitor) {
        visitor.visitTypeParameter(this);
    }

    @NotNull
    public Variance getVariance() {
        JetModifierList modifierList = getModifierList();
        if (modifierList == null) return Variance.INVARIANT;

        if (modifierList.hasModifier(JetTokens.OUT_KEYWORD)) return Variance.OUT_VARIANCE;
        if (modifierList.hasModifier(JetTokens.IN_KEYWORD)) return Variance.IN_VARIANCE;
        return Variance.INVARIANT;
    }

    @Nullable
    public JetTypeReference getExtendsBound() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }
}
