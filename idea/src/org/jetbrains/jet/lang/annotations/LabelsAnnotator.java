/*
 * @author max
 */
package org.jetbrains.jet.lang.annotations;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetHighlighter;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

public class LabelsAnnotator implements Annotator {
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
        element.accept(new JetVisitor() {
            @Override
            public void visitPrefixExpression(JetPrefixExpression expression) {
                ASTNode operationTokenNode = expression.getOperationTokenNode();
                if (JetTokens.LABELS.contains(operationTokenNode.getElementType())) {
                    holder.createInfoAnnotation(operationTokenNode, null).setTextAttributes(JetHighlighter.JET_LABEL_IDENTIFIER);
                }
            }

            @Override
            public void visitLabelQualifiedExpression(JetLabelQualifiedExpression expression) {
                ASTNode targetLabelNode = expression.getTargetLabelNode();
                if (targetLabelNode != null) {
                    holder.createInfoAnnotation(targetLabelNode, null).setTextAttributes(JetHighlighter.JET_LABEL_IDENTIFIER);
                }
            }

        });
    }
}
