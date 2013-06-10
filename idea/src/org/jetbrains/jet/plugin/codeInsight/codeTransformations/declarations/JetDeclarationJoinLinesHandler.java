package org.jetbrains.jet.plugin.codeInsight.codeTransformations.declarations;

import com.google.common.base.Predicate;
import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetPsiUtil;

public class JetDeclarationJoinLinesHandler implements JoinRawLinesHandlerDelegate {
    private static final Predicate<PsiElement> IS_APPLICABLE = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement input) {
            return input != null && DeclarationUtils.checkAndGetPropertyAndInitializer(input) != null;
        }
    };

    @Override
    public int tryJoinRawLines(Document document, PsiFile file, int start, int end) {
        PsiElement element = JetPsiUtil.skipSiblingsBackwardByPredicate(file.findElementAt(start), DeclarationUtils.SKIP_DELIMITERS);

        PsiElement target = JetPsiUtil.getParentByTypeAndPredicate(element, JetElement.class, IS_APPLICABLE, false);
        if (target == null) return -1;

        return DeclarationUtils.joinPropertyDeclarationWithInitializer(target).getTextRange().getStartOffset();
    }

    @Override
    public int tryJoinLines(Document document, PsiFile file, int start, int end) {
        return -1;
    }
}
