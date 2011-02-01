package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetTypeProjection extends JetDeclaration {
    public JetTypeProjection(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public JetProjectionKind getProjectionKind() {
        JetModifierList modifierList = getModifierList();
        if (modifierList != null) {
            if (modifierList.hasModifier(JetTokens.IN_KEYWORD)) {
                return JetProjectionKind.IN;
            }
            if (modifierList.hasModifier(JetTokens.OUT_KEYWORD)) {
                return JetProjectionKind.OUT;
            }
        }
        if (findChildByType(JetTokens.MUL) != null) {
            return JetProjectionKind.STAR;
        }

        return JetProjectionKind.NONE;
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitTypeProjection(this);
    }

    @Nullable
    public JetTypeReference getTypeReference() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }
}
