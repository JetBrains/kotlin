package org.jetbrains.jet.plugin.quickfix;

import com.intellij.extapi.psi.ASTDelegatePsiElement;
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
        if (modifier == JetTokens.ABSTRACT_KEYWORD || modifier == JetTokens.OPEN_KEYWORD) {
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
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetModifierListOwner newElement = (JetModifierListOwner) element.copy();
        element.replace(removeModifier(newElement, modifier));
    }

    @NotNull
    /*package*/ static <T extends JetModifierListOwner> T removeModifier(T element, JetToken modifier) {
        JetModifierList modifierList = element.getModifierList();
        assert modifierList != null;
        removeModifierFromList(modifierList, modifier);
        if (modifierList.getFirstChild() == null) {
            PsiElement whiteSpace = modifierList.getNextSibling();
            assert element instanceof JetElement;
            ((JetElement) element).deleteChildInternal(modifierList.getNode());
            removeWhiteSpace((JetElement) element, whiteSpace);
        }
        return element;
    }

    /*package*/ static JetModifierList removeModifierFromList(@NotNull JetModifierList modifierList, JetToken modifier) {
        assert modifierList.hasModifier(modifier);
        ASTNode modifierNode = modifierList.getModifierNode(modifier);
        PsiElement whiteSpace = modifierNode.getPsi().getNextSibling();
        boolean wsRemoved = removeWhiteSpace(modifierList, whiteSpace);
        modifierList.deleteChildInternal(modifierNode);
        if (!wsRemoved) {
            removeWhiteSpace(modifierList, modifierList.getLastChild());
        }
        return modifierList;
    }

    private static boolean removeWhiteSpace(ASTDelegatePsiElement element, PsiElement subElement) {
        if (subElement instanceof PsiWhiteSpace) {
            element.deleteChildInternal(subElement.getNode());
            return true;
        }
        return false;
    }

    public static JetIntentionActionFactory<JetModifierListOwner> createFactory(final JetKeywordToken modifier) {
        return new JetIntentionActionFactory<JetModifierListOwner>() {
            @Override
            public JetIntentionAction<JetModifierListOwner> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetModifierListOwner;
                return new RemoveModifierFix((JetModifierListOwner) diagnostic.getPsiElement(), modifier);
            }
        };
    }
}
