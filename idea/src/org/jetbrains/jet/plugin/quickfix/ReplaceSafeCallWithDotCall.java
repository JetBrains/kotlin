package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author svtk
 */
public class ReplaceSafeCallWithDotCall extends JetIntentionAction<JetElement> {

    public ReplaceSafeCallWithDotCall(@NotNull JetElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("replace.with.dot.call");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("replace.with.dot.call");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (element instanceof JetSafeQualifiedExpression) {
            JetSafeQualifiedExpression safeQualifiedExpression = (JetSafeQualifiedExpression) element;
            JetDotQualifiedExpression newElement = (JetDotQualifiedExpression) JetPsiFactory.createExpression(project, "x.foo");

            CodeEditUtil.replaceChild(newElement.getNode(), newElement.getSelectorExpression().getNode(), safeQualifiedExpression.getSelectorExpression().getNode());
            CodeEditUtil.replaceChild(newElement.getNode(), newElement.getReceiverExpression().getNode(), safeQualifiedExpression.getReceiverExpression().getNode());

            element.replace(newElement);
        }
    }

    public static JetIntentionActionFactory<JetElement> createFactory() {
        return new JetIntentionActionFactory<JetElement>() {
            @Override
            public JetIntentionAction<JetElement> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetElement;
                return new ReplaceSafeCallWithDotCall((JetElement) diagnostic.getPsiElement());
            }
        };
    }
}
