package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.types.ProjectionKind;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class JetTypeProjection extends JetDeclaration {
    public JetTypeProjection(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public ProjectionKind getProjectionKind() {
        JetModifierList modifierList = getModifierList();
        if (modifierList != null) {
            if (modifierList.hasModifier(JetTokens.IN_KEYWORD)) {
                return ProjectionKind.IN_ONLY;
            }
            if (modifierList.hasModifier(JetTokens.OUT_KEYWORD)) {
                return ProjectionKind.OUT_ONLY;
            }
        }
        if (findChildByType(JetTokens.MUL) != null) {
            return ProjectionKind.NEITHER_OUT_NOR_IN;
        }

        return ProjectionKind.NO_PROJECTION;
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
