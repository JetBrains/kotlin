package org.jetbrains.jet.plugin.intentions.declarations;

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import jet.Function1;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;

public class JetDeclarationJoinLinesHandler implements JoinRawLinesHandlerDelegate {
    private static final Function1<PsiElement, Boolean> IS_APPLICABLE = new Function1<PsiElement, Boolean>() {
        @Override
        public Boolean invoke(@Nullable PsiElement input) {
            return input != null && DeclarationUtils.checkAndGetPropertyAndInitializer(input) != null;
        }
    };

    @Override
    public int tryJoinRawLines(Document document, PsiFile file, int start, int end) {
        PsiElement element = JetPsiUtil.skipSiblingsBackwardByPredicate(file.findElementAt(start), DeclarationUtils.SKIP_DELIMITERS);

        //noinspection unchecked
        PsiElement target = PsiUtilPackage.getParentByTypesAndPredicate(element, false, ArrayUtil.EMPTY_CLASS_ARRAY, IS_APPLICABLE);
        if (target == null) return -1;

        return DeclarationUtils.joinPropertyDeclarationWithInitializer(target).getTextRange().getStartOffset();
    }

    @Override
    public int tryJoinLines(Document document, PsiFile file, int start, int end) {
        return -1;
    }
}
