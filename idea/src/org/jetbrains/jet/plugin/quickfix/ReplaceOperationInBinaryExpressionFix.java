package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetBinaryExpressionWithTypeRHS;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author svtk
 */
public abstract class ReplaceOperationInBinaryExpressionFix<T extends JetExpression> extends JetIntentionAction<T> {
    private final String expressionWithNecessaryOperation;
    public ReplaceOperationInBinaryExpressionFix(@NotNull T element, String expressionWithNecessaryOperation) {
        super(element);
        this.expressionWithNecessaryOperation = expressionWithNecessaryOperation;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("replace.operation.in.binary.expression");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (element instanceof JetBinaryExpressionWithTypeRHS) {
            JetBinaryExpressionWithTypeRHS expression = (JetBinaryExpressionWithTypeRHS) JetPsiFactory.createExpression(project, expressionWithNecessaryOperation);

            JetBinaryExpressionWithTypeRHS newElement = (JetBinaryExpressionWithTypeRHS) element.copy();
            CodeEditUtil.replaceChild(newElement.getNode(), newElement.getOperationSign().getNode(), expression.getOperationSign().getNode());

            element.replace(newElement);
        }
    }

    public static JetIntentionActionFactory<JetBinaryExpressionWithTypeRHS> createChangeCastToStaticAssertFactory() {
        return new JetIntentionActionFactory<JetBinaryExpressionWithTypeRHS>() {
            @Override
            public JetIntentionAction<JetBinaryExpressionWithTypeRHS> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetBinaryExpressionWithTypeRHS;
                return new ReplaceOperationInBinaryExpressionFix<JetBinaryExpressionWithTypeRHS>((JetBinaryExpressionWithTypeRHS) diagnostic.getPsiElement(), "2 : Int") {
                    @NotNull
                    @Override
                    public String getText() {
                        return JetBundle.message("replace.cast.with.static.assert");
                    }
                };
            }
        };
    }
}
