package org.jetbrains.jet.plugin.quickfix;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

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
        if (modifier == JetTokens.ABSTRACT_KEYWORD) {
            return "Make " + getElementName() + " not " + modifier.getValue();
        }
        return "Remove '" + modifier.getValue() + "' modifier";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Remove modifier";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return element.isValid();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        element.replace(removeModifier(element, modifier));
    }

    @NotNull
    /*package*/ static <T extends JetModifierListOwner> T removeModifier(T element, JetToken modifier) {
        T newElement = (T) (element.copy());
        assert newElement.hasModifier(modifier);
        JetModifierList modifierList = newElement.getModifierList();
        ASTNode modifierNode = modifierList.getModifierNode(modifier);
        PsiElement whiteSpace = modifierNode.getPsi().getNextSibling();
        ((JetElement)newElement).deleteChildInternal(modifierNode);
        if (modifierList.getChildren().length == 0) {
            whiteSpace = modifierList.getNextSibling();
            ((JetElement) newElement).deleteChildInternal(modifierList.getNode());
            removeWhiteSpace(newElement, whiteSpace);
        }
        else {
            removeWhiteSpace(newElement, whiteSpace);
        }
        return newElement;
    }

    private static void removeWhiteSpace(PsiElement element, PsiElement subElement) {
        if (subElement instanceof PsiWhiteSpace) {
            ((JetElement) element).deleteChildInternal(subElement.getNode());
        }
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
