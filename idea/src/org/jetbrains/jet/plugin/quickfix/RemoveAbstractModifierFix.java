package org.jetbrains.jet.plugin.quickfix;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lexer.JetTokens;

/**
* @author svtk
*/
public class RemoveAbstractModifierFix extends IntentionActionForPsiElement<JetModifierListOwner> {
    public RemoveAbstractModifierFix(@NotNull JetModifierListOwner element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return "remove.abstract.modifier.fix";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "remove.abstract.modifier.family";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return element.isValid();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        element.replace(removeAbstractModifier(element));
    }

    @NotNull
    public static JetModifierListOwner removeAbstractModifier(PsiElement element) {
        JetModifierListOwner newElement = (JetModifierListOwner) (element.copy());
        assert newElement.hasModifier(JetTokens.ABSTRACT_KEYWORD);
        ASTNode abstractNode = newElement.getModifierList().getModifierNode(JetTokens.ABSTRACT_KEYWORD);
        ((JetElement)newElement).deleteChildInternal(abstractNode);
        return newElement;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static IntentionActionFactory<JetModifierListOwner> factory =
        new IntentionActionFactory<JetModifierListOwner>() {
            @Override
            public IntentionActionForPsiElement<JetModifierListOwner> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetModifierListOwner;
                return new RemoveAbstractModifierFix((JetModifierListOwner) diagnostic.getPsiElement());
            }
        };
}
