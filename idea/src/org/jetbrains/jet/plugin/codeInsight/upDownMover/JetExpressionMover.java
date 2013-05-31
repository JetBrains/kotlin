package org.jetbrains.jet.plugin.codeInsight.upDownMover;

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
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

public class JetExpressionMover extends AbstractJetUpDownMover {
    public JetExpressionMover() {
    }

    @SuppressWarnings("FieldMayBeFinal")
    private static Class[] MOVABLE_ELEMENT_CLASSES = {JetExpression.class, JetWhenEntry.class, PsiComment.class};

    @SuppressWarnings("FieldMayBeFinal")
    private static Class[] BLOCKLIKE_ELEMENT_CLASSES =
            {JetBlockExpression.class, JetWhenExpression.class, JetClassBody.class, JetFile.class};

    @SuppressWarnings("FieldMayBeFinal")
    private static Class[] FUNCTIONLIKE_ELEMENT_CLASSES =
            {JetFunction.class, JetPropertyAccessor.class, JetClassInitializer.class};

    @Nullable
    private static LineRange getLineRange(@NotNull PsiElement element, @NotNull Editor editor) {
        TextRange textRange = element.getTextRange();
        if (editor.getDocument().getTextLength() < textRange.getEndOffset()) return null;

        int startLine = editor.offsetToLogicalPosition(textRange.getStartOffset()).line;
        int endLine = editor.offsetToLogicalPosition(textRange.getEndOffset()).line + 1;

        return new LineRange(startLine, endLine);
    }

    @Nullable
    private static PsiElement getStandaloneClosingBrace(@NotNull PsiFile file, @NotNull Editor editor) {
        LineRange range = getLineRangeFromSelection(editor);
        if (range.endLine - range.startLine != 1) return null;
        int offset = editor.getCaretModel().getOffset();
        Document document = editor.getDocument();
        int line = document.getLineNumber(offset);
        int lineStartOffset = document.getLineStartOffset(line);
        String lineText = document.getText().substring(lineStartOffset, document.getLineEndOffset(line));
        if (!lineText.trim().equals("}")) return null;

        return file.findElementAt(lineStartOffset + lineText.indexOf('}'));
    }

    private static BraceStatus checkForMovableDownClosingBrace(
            @NotNull PsiElement closingBrace,
            @NotNull PsiElement block,
            @NotNull Editor editor,
            @NotNull MoveInfo info
    ) {
        PsiElement current = block;
        PsiElement nextElement = null;
        PsiElement nextExpression = null;
        do {
            PsiElement sibling = firstNonWhiteElement(current.getNextSibling(), true);
            if (sibling != null && nextElement == null) {
                nextElement = sibling;
            }

            if (sibling instanceof JetExpression) {
                nextExpression = sibling;
                break;
            }

            current = current.getParent();
        }
        while (current != null && !(PsiTreeUtil.instanceOf(current, BLOCKLIKE_ELEMENT_CLASSES)));

        if (nextExpression == null) return BraceStatus.NOT_MOVABLE;

        Document doc = editor.getDocument();

        info.toMove = new LineRange(closingBrace, closingBrace, doc);
        info.toMove2 = new LineRange(nextElement, nextExpression);
        info.indentSource = true;

        return BraceStatus.MOVABLE;
    }

    private static BraceStatus checkForMovableUpClosingBrace(
            @NotNull PsiElement closingBrace,
            PsiElement block,
            @NotNull Editor editor,
            @NotNull MoveInfo info
    ) {
        //noinspection unchecked
        PsiElement prev = JetPsiUtil.getLastChildByType(block, JetExpression.class);
        if (prev == null) return BraceStatus.NOT_MOVABLE;

        Document doc = editor.getDocument();

        info.toMove = new LineRange(closingBrace, closingBrace, doc);
        info.toMove2 = new LineRange(prev, prev, doc);
        info.indentSource = true;

        return BraceStatus.MOVABLE;
    }

    private static enum BraceStatus {
        NOT_FOUND,
        MOVABLE,
        NOT_MOVABLE
    }

    // Returns null if standalone closing brace is not found
    private static BraceStatus checkForMovableClosingBrace(
            @NotNull Editor editor,
            @NotNull PsiFile file,
            @NotNull MoveInfo info,
            boolean down
    ) {
        PsiElement closingBrace = getStandaloneClosingBrace(file, editor);
        if (closingBrace == null) return BraceStatus.NOT_FOUND;

        PsiElement blockLikeElement = closingBrace.getParent();
        if (!(blockLikeElement instanceof JetBlockExpression)) return BraceStatus.NOT_MOVABLE;
        if (blockLikeElement.getParent() instanceof JetWhenEntry) return BraceStatus.NOT_FOUND;

        PsiElement enclosingExpression = PsiTreeUtil.getParentOfType(blockLikeElement, JetExpression.class);

        if (enclosingExpression instanceof JetDoWhileExpression) return BraceStatus.NOT_MOVABLE;

        if (enclosingExpression instanceof JetIfExpression) {
            JetIfExpression ifExpression = (JetIfExpression) enclosingExpression;

            if (blockLikeElement == ifExpression.getThen() && ifExpression.getElse() != null) return BraceStatus.NOT_MOVABLE;
        }

        return down
               ? checkForMovableDownClosingBrace(closingBrace, blockLikeElement, editor, info)
               : checkForMovableUpClosingBrace(closingBrace, blockLikeElement, editor, info);
    }

    @Nullable
    private static JetBlockExpression findClosestBlock(@NotNull PsiElement anchor, boolean down) {
        PsiElement current = PsiTreeUtil.getParentOfType(anchor, JetBlockExpression.class);
        while (current != null) {
            PsiElement parent = current.getParent();
            if (parent instanceof JetClassBody ||
                parent instanceof JetClassInitializer ||
                parent instanceof JetFunction ||
                parent instanceof JetProperty) {
                return null;
            }

            if (parent instanceof JetBlockExpression) return (JetBlockExpression) parent;

            PsiElement sibling = down ? current.getNextSibling() : current.getPrevSibling();
            if (sibling != null) {
                //noinspection unchecked
                JetBlockExpression block = JetPsiUtil.getOutermostJetElement(sibling, down, JetBlockExpression.class);
                if (block != null) return block;

                current = sibling;
            }
            else {
                current = parent;
            }
        }

        return null;
    }

    @Nullable
    private static LineRange getExpressionTargetRange(@NotNull Editor editor, @NotNull PsiElement sibling, boolean down) {
        PsiElement start = sibling;
        PsiElement end = sibling;

        // moving out of code block
        if (sibling.getNode().getElementType() == (down ? JetTokens.RBRACE : JetTokens.LBRACE)) {
            PsiElement parent = sibling.getParent();
            if (!(parent instanceof JetBlockExpression)) return null;

            JetBlockExpression block = (JetBlockExpression) parent;

            JetBlockExpression newBlock = findClosestBlock(sibling, down);
            if (newBlock == null) return null;

            if (PsiTreeUtil.isAncestor(newBlock, block, true)) {
                PsiElement outermostParent = JetPsiUtil.getOutermostParent(block, newBlock, true);

                if (down) {
                    end = outermostParent;
                }
                else {
                    start = outermostParent;
                }
            }
            else {
                if (down) {
                    end = newBlock.getLBrace();
                }
                else {
                    start = newBlock.getRBrace();
                }
            }
        }
        else {
            // moving into code block
            //noinspection unchecked
            JetElement blockLikeElement = JetPsiUtil.getOutermostJetElement(sibling, down, JetBlockExpression.class, JetWhenExpression.class, JetClassBody.class);
            if (blockLikeElement != null &&
                !(PsiTreeUtil.instanceOf(blockLikeElement, FUNCTIONLIKE_ELEMENT_CLASSES))) {
                if (blockLikeElement instanceof JetWhenExpression) {
                    //noinspection unchecked
                    blockLikeElement = JetPsiUtil.getOutermostJetElement(blockLikeElement, down, JetBlockExpression.class);
                }

                if (blockLikeElement != null) {
                    if (down) {
                        end = JetPsiUtil.findChildByType(blockLikeElement, JetTokens.LBRACE);
                    }
                    else {
                        start = JetPsiUtil.findChildByType(blockLikeElement, JetTokens.RBRACE);
                    }
                }
            }
        }

        return start != null && end != null ? new LineRange(start, end, editor.getDocument()) : null;
    }

    @Nullable
    private static LineRange getWhenEntryTargetRange(@NotNull Editor editor, @NotNull PsiElement sibling, boolean down) {
        if (sibling.getNode().getElementType() == (down ? JetTokens.RBRACE : JetTokens.LBRACE) &&
            PsiTreeUtil.getParentOfType(sibling, JetWhenEntry.class) == null) {
            return null;
        }

        return new LineRange(sibling, sibling, editor.getDocument());
    }

    @Nullable
    private static LineRange getTargetRange(
            @NotNull Editor editor,
            @Nullable PsiElement elementToCheck,
            @NotNull PsiElement sibling,
            boolean down
    ) {
        if (elementToCheck instanceof JetExpression || elementToCheck instanceof PsiComment) {
            return getExpressionTargetRange(editor, sibling, down);
        }

        if (elementToCheck instanceof JetWhenEntry) {
            return getWhenEntryTargetRange(editor, sibling, down);
        }

        return null;
    }

    @Nullable
    private static LineRange getSourceRange(@NotNull PsiElement firstElement, @NotNull PsiElement lastElement, @NotNull Editor editor) {
        if (firstElement == lastElement) {
            //noinspection ConstantConditions
            LineRange sourceRange = getLineRange(firstElement, editor);

            if (sourceRange != null) {
                sourceRange.firstElement = sourceRange.lastElement = firstElement;
            }

            return sourceRange;
        }

        //noinspection ConstantConditions
        PsiElement parent = PsiTreeUtil.findCommonParent(firstElement, lastElement);
        if (parent == null) return null;

        Pair<PsiElement, PsiElement> combinedRange = getElementRange(parent, firstElement, lastElement);

        if (combinedRange == null
            || !(PsiTreeUtil.instanceOf(combinedRange.first, MOVABLE_ELEMENT_CLASSES))
            || !(PsiTreeUtil.instanceOf(combinedRange.second, MOVABLE_ELEMENT_CLASSES))) {
            return null;
        }

        LineRange lineRange1 = getLineRange(combinedRange.first, editor);
        if (lineRange1 == null) return null;

        LineRange lineRange2 = getLineRange(combinedRange.second, editor);
        if (lineRange2 == null) return null;

        LineRange sourceRange = new LineRange(lineRange1.startLine, lineRange2.endLine);
        sourceRange.firstElement = combinedRange.first;
        sourceRange.lastElement = combinedRange.second;

        return sourceRange;
    }

    @Nullable
    private static PsiElement getMovableElement(@NotNull PsiElement element) {
        return PsiTreeUtil.getNonStrictParentOfType(element, MOVABLE_ELEMENT_CLASSES);
    }

    private static enum MoveStatus {
        DEFAULT,
        FORBIDDEN,
        PERMITTED
    }

    private static MoveStatus getMoveStatus(@NotNull PsiElement element, boolean down) {
        if (element instanceof JetParameter) {
            PsiElement sibling = getSiblingOfType(element, down, element.getClass());
            return (sibling != null) ? MoveStatus.DEFAULT : MoveStatus.FORBIDDEN;
        }

        return MoveStatus.PERMITTED;
    }

    @Override
    public boolean checkAvailable(@NotNull Editor editor, @NotNull PsiFile file, @NotNull MoveInfo info, boolean down) {
        if (!super.checkAvailable(editor, file, info, down)) return false;

        switch (checkForMovableClosingBrace(editor, file, info, down)) {
            case NOT_MOVABLE: {
                info.toMove2 = null;
                return true;
            }
            case MOVABLE: return true;
            default: break;
        }

        LineRange oldRange = info.toMove;

        Pair<PsiElement, PsiElement> psiRange = getElementRange(editor, file, oldRange);
        if (psiRange == null) return false;

        //noinspection unchecked
        PsiElement firstElement = getMovableElement(psiRange.getFirst());
        PsiElement lastElement = getMovableElement(psiRange.getSecond());

        if (firstElement == null || lastElement == null) return false;

        MoveStatus firstMoveStatus = getMoveStatus(firstElement, down);
        MoveStatus lastMoveStatus = getMoveStatus(lastElement, down);

        if (firstMoveStatus == MoveStatus.DEFAULT || lastMoveStatus == MoveStatus.DEFAULT) {
            return true;
        }

        if (firstMoveStatus == MoveStatus.FORBIDDEN || lastMoveStatus == MoveStatus.FORBIDDEN) {
            info.toMove2 = null;
            return true;
        }

        LineRange sourceRange = getSourceRange(firstElement, lastElement, editor);
        if (sourceRange == null) return false;

        PsiElement sibling = adjustWhiteSpaceSibling(editor, sourceRange, info, down);

        // Either reached last sibling, or jumped over multi-line whitespace
        if (sibling == null) return true;

        info.toMove = sourceRange;
        info.toMove2 = getTargetRange(editor, sourceRange.firstElement, sibling, down);
        return true;
    }
}
