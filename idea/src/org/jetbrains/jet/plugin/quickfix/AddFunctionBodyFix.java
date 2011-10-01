package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author svtk
 */
public class AddFunctionBodyFix extends JetIntentionAction<JetFunctionOrPropertyAccessor> {
    public AddFunctionBodyFix(@NotNull JetFunctionOrPropertyAccessor element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return "Add function body";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Add function body";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) &&
                element.getBodyExpression() == null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetFunctionOrPropertyAccessor newElement = (JetFunctionOrPropertyAccessor) element.copy();
        JetExpression bodyExpression = newElement.getBodyExpression();
        if (!(newElement.getLastChild() instanceof PsiWhiteSpace)) {
            newElement.add(JetPsiFactory.createWhiteSpace(project));
        }
        if (bodyExpression == null) {
            newElement.add(JetPsiFactory.createEmptyBody(project));
        }
        element.replace(newElement);
    }
    
    public static JetIntentionActionFactory<JetFunctionOrPropertyAccessor> createFactory() {
        return new JetIntentionActionFactory<JetFunctionOrPropertyAccessor>() {
            @Override
            public JetIntentionAction<JetFunctionOrPropertyAccessor> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetFunctionOrPropertyAccessor;
                return new AddFunctionBodyFix((JetFunctionOrPropertyAccessor) diagnostic.getPsiElement());
            }
        };
    }
}
