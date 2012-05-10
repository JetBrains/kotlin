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
import org.jetbrains.jet.plugin.codeInsight.ReferenceToClassesShortening;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Collections;

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
}
