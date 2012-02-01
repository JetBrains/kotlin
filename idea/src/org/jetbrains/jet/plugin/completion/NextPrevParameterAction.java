package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author Evgeny Gerashchenko
 * @author max (originally from objc)
 * @since 2/1/12
 */
public abstract class NextPrevParameterAction extends CodeInsightAction {
    private boolean myNext;

    protected NextPrevParameterAction(boolean next) {
        myNext = next;
    }

    @Override
    protected CodeInsightActionHandler getHandler() {
        return new Handler();
    }

    @Override
    protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        return file instanceof JetFile;
    }

    @NotNull
    private PsiElement goToNextPrevElement(@NotNull PsiElement element) {
        if (myNext) {
            PsiElement nextLeaf = PsiTreeUtil.nextLeaf(element);
            if (nextLeaf == null) {
                PsiElement root = PsiTreeUtil.getTopmostParentOfType(element, JetFile.class);
                assert root != null;
                return PsiTreeUtil.firstChild(root);
            }
            return nextLeaf;
        } else {
            PsiElement prevLeaf = PsiTreeUtil.prevLeaf(element);
            if (prevLeaf == null) {
                PsiElement root = PsiTreeUtil.getTopmostParentOfType(element, JetFile.class);
                assert root != null;
                return PsiTreeUtil.lastChild(root);
            }
            return prevLeaf;
        }
    }

    private void selectTemplate(Editor editor, SelectionModel selModel, PsiElement current) {
        PsiElement match = goToNextPrevElement(goToNextPrevElement(current));
        JetToken expected = myNext ? JetTokens.IDE_TEMPLATE_END : JetTokens.IDE_TEMPLATE_START;
        if (expected != match.getNode().getElementType()) return;

        int start = Math.min(current.getTextOffset(), match.getTextOffset());
        int end = Math.max(current.getTextOffset() + current.getTextLength(),
                           match.getTextOffset() + match.getTextLength());

        editor.getCaretModel().moveToOffset(end);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);

        editor.getCaretModel().moveToOffset(start);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);

        selModel.setSelection(start, end);
    }

    private class Handler implements CodeInsightActionHandler {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
            PsiDocumentManager.getInstance(project).commitAllDocuments();

            JetToken terminatingToken = myNext ? JetTokens.IDE_TEMPLATE_START : JetTokens.IDE_TEMPLATE_END;

            SelectionModel selModel = editor.getSelectionModel();
            PsiElement first = file.findElementAt((selModel.getSelectionStart() + selModel.getSelectionEnd()) / 2);
            PsiElement current = first;
            if (first != null) {
                do {
                    if (current.getNode().getElementType() == terminatingToken) {
                        selectTemplate(editor, selModel, current);
                        return;
                    }

                    current = goToNextPrevElement(current);
                } while (current != first);
            }
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }

    public static class Next extends NextPrevParameterAction {
        public Next() {
            super(true);
        }
    }

    public static class Prev extends NextPrevParameterAction {
        public Prev() {
            super(false);
        }
    }
}
