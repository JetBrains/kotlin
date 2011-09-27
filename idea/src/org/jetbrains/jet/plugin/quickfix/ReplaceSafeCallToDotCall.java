package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author svtk
 */
public class ReplaceSafeCallToDotCall extends JetIntentionAction<JetElement> {

    public ReplaceSafeCallToDotCall(@NotNull JetElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return "Replace to dot call";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Replace safe call to dot call";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (element instanceof JetSafeQualifiedExpression) {
            JetSafeQualifiedExpression safeQualifiedExpression = (JetSafeQualifiedExpression) element;
            JetDotQualifiedExpression newElement = (JetDotQualifiedExpression) JetPsiFactory.createExpression(project, "x.foo");

            //TODO check for null
            CodeEditUtil.replaceChild(newElement.getNode(), newElement.getSelectorExpression().getNode(), safeQualifiedExpression.getSelectorExpression().getNode());
            CodeEditUtil.replaceChild(newElement.getNode(), newElement.getReceiverExpression().getNode(), safeQualifiedExpression.getReceiverExpression().getNode());

            element.replace(newElement);
        }
        else if (element instanceof JetWhenConditionCall) {
            JetWhenConditionCall newElement = (JetWhenConditionCall) element;
            JetDotQualifiedExpression callExpression = (JetDotQualifiedExpression) JetPsiFactory.createExpression(project, "x.foo");
            CodeEditUtil.replaceChild(newElement.getNode(), newElement.getOperationTokenNode(), callExpression.getOperationTokenNode());

            element.replace(newElement);
        }
    }

    public static JetIntentionActionFactory<JetElement> createFactory() {
        return new JetIntentionActionFactory<JetElement>() {
            @Override
            public JetIntentionAction<JetElement> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetElement;
                return new ReplaceSafeCallToDotCall((JetElement) diagnostic.getPsiElement());
            }
        };
    }
}
