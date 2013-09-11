package org.jetbrains.jet.plugin.codeInsight.upDownMover;

import com.intellij.codeInsight.editorActions.moveUpDown.LineMover;
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetBlockExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetFunctionLiteral;

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

        int topExtension = 0;
        int bottomExtension = 0;

        if (parent instanceof JetFunctionLiteral) {
            JetBlockExpression block = ((JetFunctionLiteral) parent).getBodyExpression();
            if (block != null) {
                PsiElement comment = null;

                boolean extendDown = false;
                if (checkCommentAtBlockBound(firstElement, lastElement, block)) {
                    comment = lastElement;
                    extendDown = true;
                    lastElement = block.getLastChild();
                } else if (checkCommentAtBlockBound(lastElement, firstElement, block)) {
                    comment = firstElement;
                    firstElement = block.getFirstChild();
                }

                if (comment != null) {
                    int extension = getElementLineCount(comment, editor);
                    if (extendDown) {
                        bottomExtension = extension;
                    }
                    else {
                        topExtension = extension;
                    }
                }


                parent = PsiTreeUtil.findCommonParent(firstElement, lastElement);
            }
        }

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

        LineRange parentLineRange = getElementSourceLineRange(parent, editor, oldRange);

        LineRange sourceRange = new LineRange(lineRange1.startLine - topExtension, lineRange2.endLine + bottomExtension);
        if (parentLineRange != null && sourceRange.contains(parentLineRange)) {
            sourceRange.firstElement = sourceRange.lastElement = parent;
        } else {
            sourceRange.firstElement = combinedRange.first;
            sourceRange.lastElement = combinedRange.second;
        }

        return sourceRange;
    }

    protected static int getElementLineCount(PsiElement element, Editor editor) {
        Document doc = editor.getDocument();
        TextRange spaceRange = element.getTextRange();

        int startLine = doc.getLineNumber(spaceRange.getStartOffset());
        int endLine = doc.getLineNumber(spaceRange.getEndOffset());

        return endLine - startLine;
    }

    protected static int getElementLine(PsiElement element, Editor editor, boolean first) {
        if (element == null) return -1;

        Document doc = editor.getDocument();
        TextRange spaceRange = element.getTextRange();

        return first ? doc.getLineNumber(spaceRange.getStartOffset()) : doc.getLineNumber(spaceRange.getEndOffset());
    }

    protected static PsiElement getLastNonWhiteSiblingInLine(@Nullable PsiElement element, @NotNull Editor editor, boolean down) {
        if (element == null) return null;

        int line = getElementLine(element, editor, down);

        PsiElement lastElement = element;
        while (true) {
            if (lastElement == null) return null;
            PsiElement sibling = firstNonWhiteSibling(lastElement, down);
            if (getElementLine(sibling, editor, down) == line) {
                lastElement = sibling;
            }
            else break;
        }

        return lastElement;
    }

    private static boolean checkCommentAtBlockBound(PsiElement blockElement, PsiElement comment, JetBlockExpression block) {
        return PsiTreeUtil.isAncestor(block, blockElement, true) && comment instanceof PsiComment;
    }

    @Nullable
    protected static PsiElement getSiblingOfType(@NotNull PsiElement element, boolean down, @NotNull Class<? extends PsiElement> type) {
        return down ? PsiTreeUtil.getNextSiblingOfType(element, type) : PsiTreeUtil.getPrevSiblingOfType(element, type);
    }

    @Nullable
    protected static PsiElement firstNonWhiteSibling(@NotNull LineRange lineRange, boolean down) {
        return firstNonWhiteElement(down ? lineRange.lastElement.getNextSibling() : lineRange.firstElement.getPrevSibling(), down);
    }

    @Nullable
    protected static PsiElement firstNonWhiteSibling(@NotNull PsiElement element, boolean down) {
        return firstNonWhiteElement(down ? element.getNextSibling() : element.getPrevSibling(), down);
    }

    @Override
    public boolean checkAvailable(@NotNull Editor editor, @NotNull PsiFile file, @NotNull MoveInfo info, boolean down) {
        return (file instanceof JetFile) && super.checkAvailable(editor, file, info, down);
    }
}
