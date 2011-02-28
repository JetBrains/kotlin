package org.jetbrains.jet.lang.annotations;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;

/**
 * @author abreslav
 */
public class JetPsiChecker implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof JetFile) {
            JetFile file = (JetFile) element;
//            JetScope jetScope = FileContentsResolver.INSTANCE.resolveFileContents(JetStandardClasses.STANDARD_CLASSES, file);
        }
    }
}
