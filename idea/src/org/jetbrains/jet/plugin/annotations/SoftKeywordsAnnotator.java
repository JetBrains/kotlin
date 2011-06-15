/*
 * @author max
 */
package org.jetbrains.jet.plugin.annotations;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetHighlighter;
import org.jetbrains.jet.lexer.JetTokens;

public class SoftKeywordsAnnotator implements Annotator {
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof LeafPsiElement) {
            if (JetTokens.SOFT_KEYWORDS.contains(((LeafPsiElement) element).getElementType())) {
                holder.createInfoAnnotation(element, null).setTextAttributes(JetHighlighter.JET_SOFT_KEYWORD);
            }
        }
    }
}
