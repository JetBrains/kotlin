/*
 * @author max
 */
package org.jetbrains.jet.plugin.annotations;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetHighlighter;
import org.jetbrains.jet.lexer.JetTokens;

public class SoftKeywordsAnnotator implements Annotator {
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof LeafPsiElement) {
            if (JetTokens.SOFT_KEYWORDS.contains(((LeafPsiElement) element).getElementType())) {
                holder.createInfoAnnotation(element, null).setTextAttributes(JetHighlighter.JET_SOFT_KEYWORD);
            }
        }
        else if (element instanceof JetAnnotationEntry) {
            JetAnnotationEntry entry = (JetAnnotationEntry) element;
            JetTypeReference typeReference = entry.getTypeReference();
            if (typeReference != null) {
                JetTypeElement typeElement = typeReference.getTypeElement();
                markAnnotationIdentifiers(typeElement, holder);
            }
        }
    }

    private void markAnnotationIdentifiers(JetTypeElement typeElement, @NotNull AnnotationHolder holder) {
        if (typeElement instanceof JetUserType) {
            JetUserType userType = (JetUserType) typeElement;
            if (userType.getQualifier() == null) {
                JetSimpleNameExpression referenceExpression = userType.getReferenceExpression();
                if (referenceExpression != null) {
                    holder.createInfoAnnotation(referenceExpression.getNode(), "Annotation").setTextAttributes(JetHighlighter.JET_SOFT_KEYWORD);
                }
            }
        }
    }
}
