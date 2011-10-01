package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lexer.JetKeywordToken;

/**
 * @author svtk
 */
public class RemoveRedundantModifierFix extends JetIntentionAction<JetModifierList> {
    private JetKeywordToken redundantModifier;
    public RemoveRedundantModifierFix(@NotNull JetModifierList element, @NotNull JetKeywordToken redundantModifier) {
        super(element);
        this.redundantModifier = redundantModifier;
    }

    @NotNull
    @Override
    public String getText() {
        return "Remove redundant '" + redundantModifier + "' modifier";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Remove redundant modifier";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetModifierList newElement = (JetModifierList) element.copy();
        element.replace(RemoveModifierFix.removeModifierFromList(newElement, redundantModifier));
    }

    public static JetIntentionActionFactory<JetModifierList> createFactory() {
        return new JetIntentionActionFactory<JetModifierList>() {
            @Override
            public JetIntentionAction<JetModifierList> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetModifierList;
                DiagnosticWithParameters<PsiElement> diagnosticWithParameters = assertAndCastToDiagnosticWithParameters(diagnostic, DiagnosticParameters.MODIFIER);
                JetKeywordToken modifier = diagnosticWithParameters.getParameter(DiagnosticParameters.MODIFIER);
                return new RemoveRedundantModifierFix((JetModifierList) diagnostic.getPsiElement(), modifier);
            }
        };
    }
}
