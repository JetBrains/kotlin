package org.jetbrains.jet.plugin.quickfix;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author svtk
 */
public class AddAbstractModifierFix extends IntentionActionForPsiElement<JetModifierListOwner> {

    public AddAbstractModifierFix(@NotNull JetModifierListOwner element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return "add.abstract.modifier.fix";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "add.abstract.modifier.family";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return element.isValid() && !element.hasModifier(JetTokens.FINAL_KEYWORD);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        element.replace(addAbstractModifier(element, project));
    }

    @NotNull
    public static JetModifierListOwner addAbstractModifier(@NotNull PsiElement element, @NotNull Project project) {
        JetModifierListOwner newElement = (JetModifierListOwner) (element.copy());

        JetModifierList modifierList = newElement.getModifierList();
        JetModifierList listWithModifier = JetPsiFactory.createModifier(project, JetTokens.ABSTRACT_KEYWORD);
        PsiElement whiteSpace = JetPsiFactory.createWhiteSpace(project);
        if (modifierList == null) {
            PsiElement firstChild = newElement.getFirstChild();
            newElement.addBefore(listWithModifier, firstChild);
            newElement.addBefore(whiteSpace, firstChild);
        }
        else if (modifierList.hasModifier(JetTokens.OPEN_KEYWORD)) {
            PsiElement openModifierPsi = modifierList.getModifierNode(JetTokens.OPEN_KEYWORD).getPsi();
            assert openModifierPsi != null;
            openModifierPsi.replace(listWithModifier.getFirstChild());
        }
        else {
            PsiElement lastChild = modifierList.getLastChild();
            modifierList.addAfter(listWithModifier.getFirstChild(), lastChild);
            modifierList.addAfter(whiteSpace, lastChild);
        }
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
                return new AddAbstractModifierFix((JetModifierListOwner) diagnostic.getPsiElement());
            }
        };
}
