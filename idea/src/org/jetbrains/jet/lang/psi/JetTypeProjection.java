package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
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
        ASTNode projectionNode = getProjectionNode();
        if (projectionNode == null) return JetProjectionKind.NONE;

        if (projectionNode.getElementType() == JetTokens.IN_KEYWORD) return JetProjectionKind.IN;
        if (projectionNode.getElementType() == JetTokens.OUT_KEYWORD) return JetProjectionKind.OUT;
        if (projectionNode.getElementType() == JetTokens.MUL) return JetProjectionKind.STAR;

        throw new IllegalStateException(projectionNode.getText());
    }

    @Override
    public void accept(@NotNull JetVisitor visitor) {
        visitor.visitTypeProjection(this);
    }

    @Nullable
    public JetTypeReference getTypeReference() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }

    @Nullable
    public ASTNode getProjectionNode() {
        JetModifierList modifierList = getModifierList();
        if (modifierList != null) {
            ASTNode node = modifierList.getModifierNode(JetTokens.IN_KEYWORD);
            if (node != null) {
                return node;
            }
            node = modifierList.getModifierNode(JetTokens.OUT_KEYWORD);
            if (node != null) {
                return node;
            }
        }
        PsiElement star = findChildByType(JetTokens.MUL);
        if (star != null) {
            return star.getNode();
        }

        return null;
    }
}
