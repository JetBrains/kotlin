package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetBinaryExpressionWithTypeRHS;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
 * @author svtk
 */
public abstract class RemoveRightPartOfBinaryExpressionFix<T extends JetExpression> extends JetIntentionAction<T> {
    public RemoveRightPartOfBinaryExpressionFix(@NotNull T element) {
        super(element);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Remove right part of a binary expression";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (element instanceof JetBinaryExpression) {
            JetBinaryExpression newElement = (JetBinaryExpression) element.copy();
            element.replace(newElement.getLeft());
        }
        else if (element instanceof JetBinaryExpressionWithTypeRHS) {
            JetBinaryExpressionWithTypeRHS newElement = (JetBinaryExpressionWithTypeRHS) element.copy();
            element.replace(newElement.getLeft());
        }
    }

    public static JetIntentionActionFactory<JetBinaryExpressionWithTypeRHS> createRemoveCastFactory() {
        return new JetIntentionActionFactory<JetBinaryExpressionWithTypeRHS>() {
            @Override
            public JetIntentionAction<JetBinaryExpressionWithTypeRHS> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetBinaryExpressionWithTypeRHS;
                return new RemoveRightPartOfBinaryExpressionFix<JetBinaryExpressionWithTypeRHS>((JetBinaryExpressionWithTypeRHS) diagnostic.getPsiElement()) {
                    @NotNull
                    @Override
                    public String getText() {
                        return "Remove cast";
                    }
                };
            }
        };
    }

    public static JetIntentionActionFactory<JetBinaryExpression> createRemoveElvisOperatorFactory() {
        return new JetIntentionActionFactory<JetBinaryExpression>() {
            @Override
            public JetIntentionAction<JetBinaryExpression> createAction(DiagnosticWithPsiElement diagnostic) {
                assert diagnostic.getPsiElement() instanceof JetBinaryExpression;
                return new RemoveRightPartOfBinaryExpressionFix<JetBinaryExpression>((JetBinaryExpression) diagnostic.getPsiElement()) {
                    @NotNull
                    @Override
                    public String getText() {
                        return "Remove elvis operator";
                    }
                };
            }
        };
    }
}

