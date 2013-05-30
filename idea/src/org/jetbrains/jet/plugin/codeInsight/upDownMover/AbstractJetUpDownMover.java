package org.jetbrains.jet.plugin.codeInsight.upDownMover;

import com.intellij.codeInsight.editorActions.moveUpDown.LineMover;
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;

public abstract class AbstractJetUpDownMover extends LineMover {
    protected AbstractJetUpDownMover() {
    }

    protected static PsiElement adjustWhiteSpaceSibling(
            @NotNull Editor editor,
            @NotNull LineRange sourceRange,
            @NotNull MoveInfo info,
            boolean down
    ) {
        PsiElement sibling = down ? sourceRange.lastElement.getNextSibling() : sourceRange.firstElement.getPrevSibling();

        if (sibling instanceof PsiWhiteSpace) {
            Document doc = editor.getDocument();
            TextRange spaceRange = sibling.getTextRange();

            int startLine = doc.getLineNumber(spaceRange.getStartOffset());
            int endLine = doc.getLineNumber(spaceRange.getEndOffset());

            if (endLine - startLine > 1) {
                int nearLine = down ? sourceRange.endLine : sourceRange.startLine - 1;

                info.toMove = sourceRange;
                info.toMove2 = new LineRange(nearLine, nearLine + 1);

                return null;
            }

            sibling = firstNonWhiteElement(sibling, down);
        }

        if (sibling == null) {
            info.toMove2 = null;
            return null;
        }

        return sibling;
    }

    @Nullable
    protected static PsiElement getSiblingOfType(@NotNull PsiElement element, boolean down, @NotNull Class<? extends PsiElement> type) {
        return down ? PsiTreeUtil.getNextSiblingOfType(element, type) : PsiTreeUtil.getPrevSiblingOfType(element, type);
    }

    @Nullable
    protected static PsiElement firstNonWhiteSibling(@NotNull LineRange lineRange, boolean down) {
        return firstNonWhiteElement(down ? lineRange.lastElement.getNextSibling() : lineRange.firstElement.getPrevSibling(), down);
    }

    @Override
    public boolean checkAvailable(@NotNull Editor editor, @NotNull PsiFile file, @NotNull MoveInfo info, boolean down) {
        return (file instanceof JetFile) && super.checkAvailable(editor, file, info, down);
    }
}
