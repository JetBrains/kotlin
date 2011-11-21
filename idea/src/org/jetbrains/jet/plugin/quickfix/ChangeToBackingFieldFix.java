package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetThisExpression;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.references.JetThisReference;

/**
 * @author svtk
 */
public class ChangeToBackingFieldFix extends JetIntentionAction<JetSimpleNameExpression> {
    public ChangeToBackingFieldFix(@NotNull JetSimpleNameExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.to.backing.field");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.to.backing.field");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetSimpleNameExpression backingField = (JetSimpleNameExpression) JetPsiFactory.createExpression(project, "$" + element.getText());
        if (element.getParent() instanceof JetDotQualifiedExpression && ((JetDotQualifiedExpression) element.getParent()).getReceiverExpression() instanceof JetThisExpression) {
            element.getParent().replace(backingField);
        }
        else {
            element.replace(backingField);
        }
    }

    public static JetIntentionActionFactory<JetSimpleNameExpression> createFactory() {
        return new JetIntentionActionFactory<JetSimpleNameExpression>() {
            @Override
            public JetIntentionAction<JetSimpleNameExpression> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetSimpleNameExpression;
                return new ChangeToBackingFieldFix((JetSimpleNameExpression) diagnostic.getPsiElement());
            }
        };
    }
}
