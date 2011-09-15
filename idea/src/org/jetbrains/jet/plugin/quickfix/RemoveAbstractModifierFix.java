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
import org.jetbrains.jet.lexer.JetTokens;

/**
* @author svtk
*/
public class RemoveAbstractModifierFix extends QuickFixes.IntentionActionForPsiElement<JetDeclaration> {
    public RemoveAbstractModifierFix(JetDeclaration declaration) {
        super(declaration);
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
        return psiElement.isValid();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetDeclaration declaration = removeAbstractModifier(psiElement);
        psiElement.replace(declaration);
    }

    public static JetDeclaration removeAbstractModifier(PsiElement element) {
        assert element instanceof JetDeclaration;
        JetDeclaration declaration = (JetDeclaration) (element.copy());
        assert declaration.hasModifier(JetTokens.ABSTRACT_KEYWORD);
        ASTNode abstractNode = declaration.getModifierList().getModifierNode(JetTokens.ABSTRACT_KEYWORD);
        declaration.deleteChildInternal(abstractNode);
        return declaration;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    public static QuickFixes.IntentionActionFactory<JetDeclaration> factory =
        new QuickFixes.IntentionActionFactory<JetDeclaration>() {
            @Override
            public QuickFixes.IntentionActionForPsiElement<JetDeclaration> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetDeclaration;
                return new RemoveAbstractModifierFix((JetDeclaration) diagnostic.getPsiElement());
            }
        };
}
