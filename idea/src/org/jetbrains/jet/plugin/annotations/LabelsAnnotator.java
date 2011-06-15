/*
 * @author max
 */
package org.jetbrains.jet.plugin.annotations;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetHighlighter;
import org.jetbrains.jet.lang.psi.JetLabelQualifiedExpression;
import org.jetbrains.jet.lang.psi.JetPrefixExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetVisitor;
import org.jetbrains.jet.lexer.JetTokens;

public class LabelsAnnotator implements Annotator {
    public void annotate(@NotNull PsiElement element, @NotNull final AnnotationHolder holder) {
//        if (ApplicationManager.getApplication().isUnitTestMode()) return;
        element.accept(new JetVisitor() {
            @Override
            public void visitPrefixExpression(JetPrefixExpression expression) {
                JetSimpleNameExpression operationSign = expression.getOperationSign();
                if (JetTokens.LABELS.contains(operationSign.getReferencedNameElementType())) {
                    holder.createInfoAnnotation(operationSign, null).setTextAttributes(JetHighlighter.JET_LABEL_IDENTIFIER);
                }
            }

            @Override
            public void visitLabelQualifiedExpression(JetLabelQualifiedExpression expression) {
                JetSimpleNameExpression targetLabel = expression.getTargetLabel();
                if (targetLabel != null) {
                    holder.createInfoAnnotation(targetLabel, null).setTextAttributes(JetHighlighter.JET_LABEL_IDENTIFIER);
                }
            }

        });
    }
}
