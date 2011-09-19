package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author svtk
 */
public class AddModifierFix extends ModifierFix {
    private final JetToken[] modifiersThanCanBeReplaced;

    private AddModifierFix(@NotNull JetModifierListOwner element, JetKeywordToken modifier, JetToken[] modifiersThanCanBeReplaced) {
        super(element, modifier);
        this.modifiersThanCanBeReplaced = modifiersThanCanBeReplaced;
    }
    
    @NotNull
    @Override
    public String getText() {
        return "add." + modifier.getValue() + ".modifier.fix";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "add." + modifier.getValue() + ".modifier.family";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && !element.hasModifier(JetTokens.FINAL_KEYWORD);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        element.replace(addModifier(element, modifier, modifiersThanCanBeReplaced, project));
    }

    @NotNull
    private static JetModifierListOwner addModifier(@NotNull PsiElement element, @NotNull JetKeywordToken modifier, JetToken[] modifiersThanCanBeReplaced, @NotNull Project project) {
        JetModifierListOwner newElement = (JetModifierListOwner) (element.copy());

        JetModifierList modifierList = newElement.getModifierList();
        JetModifierList listWithModifier = JetPsiFactory.createModifier(project, modifier);
        PsiElement whiteSpace = JetPsiFactory.createWhiteSpace(project);
        if (modifierList == null) {
            PsiElement firstChild = newElement.getFirstChild();
            newElement.addBefore(listWithModifier, firstChild);
            newElement.addBefore(whiteSpace, firstChild);
        }
        else {
            boolean replaced = false;
            for (JetToken modifierThanCanBeReplaced : modifiersThanCanBeReplaced) {
                if (modifierList.hasModifier(modifierThanCanBeReplaced)) {
                    PsiElement openModifierPsi = modifierList.getModifierNode(modifierThanCanBeReplaced).getPsi();
                    assert openModifierPsi != null;
                    openModifierPsi.replace(listWithModifier.getFirstChild());
                    replaced = true;
                }
            }
            if (!replaced) {
                PsiElement lastChild = modifierList.getLastChild();
                modifierList.addAfter(listWithModifier.getFirstChild(), lastChild);
                modifierList.addAfter(whiteSpace, lastChild);
            }
        }
        return newElement;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static IntentionActionFactory<JetModifierListOwner> createFactory(final JetKeywordToken modifier, final JetToken[] modifiersThatCanBeReplaced) {
        return new IntentionActionFactory<JetModifierListOwner>() {
            @Override
            public IntentionActionForPsiElement<JetModifierListOwner> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetModifierListOwner;
                return new AddModifierFix((JetModifierListOwner) diagnostic.getPsiElement(), modifier, modifiersThatCanBeReplaced);
            }
        };
    }
    
    public static IntentionActionFactory<JetModifierListOwner> createFactory(final JetKeywordToken modifier) {
        return createFactory(modifier, new JetToken[] {});
    }
}
