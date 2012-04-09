package org.jetbrains.jet.plugin.refactoring.introduceVariable;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.types.JetType;

/**
 * User: Alefas
 * Date: 14.02.12
 */
public class JetChangePropertyActions {
    private JetChangePropertyActions() {
    }

    public static void declareValueOrVariable(Project project, boolean isVariable, JetProperty property) {
        ASTNode node;
        if (isVariable) {
            node = JetPsiFactory.createVarNode(project);
        }
        else {
            node = JetPsiFactory.createValNode(project);
        }
        property.getValOrVarNode().getPsi().replace(node.getPsi());
    }

    public static void addTypeAnnotation(Project project, JetProperty property, @NotNull JetType exprType) {
        if (property.getPropertyTypeRef() != null) return;
        PsiElement anchor = property.getNameIdentifier();
        if (anchor == null) return;
        anchor = anchor.getNextSibling();
        if (anchor == null || !(anchor instanceof PsiWhiteSpace)) return;
        JetTypeReference typeReference = JetPsiFactory.createType(project, exprType.toString());
        ASTNode colon = JetPsiFactory.createColonNode(project);
        ASTNode anchorNode = anchor.getNode().getTreeNext();
        property.getNode().addChild(colon, anchorNode);
        property.getNode().addChild(JetPsiFactory.createWhiteSpace(project).getNode(), anchorNode);
        property.getNode().addChild(typeReference.getNode(), anchorNode);
        property.getNode().addChild(JetPsiFactory.createWhiteSpace(project).getNode(), anchorNode);
        anchor.delete();
    }

    public static void removeTypeAnnotation(Project project, JetProperty property) {
        JetTypeReference propertyTypeRef = property.getPropertyTypeRef();
        if (propertyTypeRef == null) return;
        PsiElement identifier = property.getNameIdentifier();
        if (identifier == null) return;
        PsiElement sibling = identifier.getNextSibling();
        if (sibling == null) return;
        PsiElement nextSibling = propertyTypeRef.getNextSibling();
        if (nextSibling == null) return;
        sibling.getParent().getNode().removeRange(sibling.getNode(), nextSibling.getNode());
    }
}
