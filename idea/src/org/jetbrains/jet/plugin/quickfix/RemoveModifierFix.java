package org.jetbrains.jet.plugin.quickfix;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;

/**
* @author svtk
*/
public class RemoveModifierFix extends ModifierFix {

    public RemoveModifierFix(@NotNull JetModifierListOwner element, JetKeywordToken modifier) {
        super(element, modifier);
    }

    @NotNull
    @Override
    public String getText() {
        return "remove." + modifier.getValue() + ".modifier.fix";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "remove." + modifier.getValue() + "abstract.modifier.family";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        element.replace(removeModifier(element, modifier));
    }

    @NotNull
    private static JetModifierListOwner removeModifier(PsiElement element, JetToken modifier) {
        JetModifierListOwner newElement = (JetModifierListOwner) (element.copy());
        assert newElement.hasModifier(modifier);
        ASTNode modifierNode = newElement.getModifierList().getModifierNode(modifier);
        ((JetElement)newElement).deleteChildInternal(modifierNode);
        return newElement;
    }


    public static IntentionActionFactory<JetModifierListOwner> createFactory(final JetKeywordToken modifier) {
        return new IntentionActionFactory<JetModifierListOwner>() {
            @Override
            public IntentionActionForPsiElement<JetModifierListOwner> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetModifierListOwner;
                return new RemoveModifierFix((JetModifierListOwner) diagnostic.getPsiElement(), modifier);
            }
        };
    }
}
