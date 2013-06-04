package org.jetbrains.jet.plugin.codeInsight.upDownMover;

import com.intellij.codeInsight.editorActions.moveUpDown.LineMover;
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;

public abstract class AbstractJetUpDownMover extends LineMover {
    protected AbstractJetUpDownMover() {
    }

    protected abstract boolean checkSourceElement(@NotNull PsiElement element);
    protected abstract LineRange getElementSourceLineRange(@NotNull PsiElement element, @NotNull Editor editor, @NotNull LineRange oldRange);

    @Nullable
    protected LineRange getSourceRange(@NotNull PsiElement firstElement, @NotNull PsiElement lastElement, @NotNull Editor editor, LineRange oldRange) {
        if (firstElement == lastElement) {
            LineRange sourceRange = getElementSourceLineRange(firstElement, editor, oldRange);

            if (sourceRange != null) {
                sourceRange.firstElement = sourceRange.lastElement = firstElement;
            }

            return sourceRange;
        }

        PsiElement parent = PsiTreeUtil.findCommonParent(firstElement, lastElement);
        if (parent == null) return null;

        Pair<PsiElement, PsiElement> combinedRange = getElementRange(parent, firstElement, lastElement);

        if (combinedRange == null
            || !checkSourceElement(combinedRange.first)
            || !checkSourceElement(combinedRange.second)) {
            return null;
        }

        LineRange lineRange1 = getElementSourceLineRange(combinedRange.first, editor, oldRange);
        if (lineRange1 == null) return null;

        LineRange lineRange2 = getElementSourceLineRange(combinedRange.second, editor, oldRange);
        if (lineRange2 == null) return null;

        LineRange sourceRange = new LineRange(lineRange1.startLine, lineRange2.endLine);
        sourceRange.firstElement = combinedRange.first;
        sourceRange.lastElement = combinedRange.second;

        return sourceRange;
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
