package org.jetbrains.jet.plugin.codeInsight.upDownMover;

import com.google.common.base.Predicate;
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

public class JetExpressionMover extends AbstractJetUpDownMover {

    private static final Predicate<JetElement> IS_CALL_EXPRESSION = new Predicate<JetElement>() {
        @Override
        public boolean apply(@Nullable JetElement input) {
            return input instanceof JetCallExpression;
        }
    };

    public JetExpressionMover() {
    }

    private final static Class[] MOVABLE_ELEMENT_CLASSES = {
            JetDeclaration.class,
            JetBlockExpression.class,
            // Only assignments
            JetBinaryExpression.class,
            JetCallExpression.class,
            JetWhenEntry.class,
            JetValueArgument.class,
            PsiComment.class
    };

    private static final Function1<PsiElement, Boolean> MOVABLE_ELEMENT_CONSTRAINT = new Function1<PsiElement, Boolean>() {
        @NotNull
        @Override
        public Boolean invoke(PsiElement element) {
            return (!(element instanceof JetBinaryExpression) || JetPsiUtil.isAssignment(element));
        }
    };

    private final static Class[] BLOCKLIKE_ELEMENT_CLASSES =
            {JetBlockExpression.class, JetWhenExpression.class, JetClassBody.class, JetFile.class};

    private final static Class[] FUNCTIONLIKE_ELEMENT_CLASSES =
            {JetFunction.class, JetPropertyAccessor.class, JetClassInitializer.class};

    private static final Predicate<JetElement> CHECK_BLOCK_LIKE_ELEMENT = new Predicate<JetElement>() {
        @Override
        public boolean apply(@Nullable JetElement input) {
            return (input instanceof JetBlockExpression || input instanceof JetClassBody)
                   && !PsiTreeUtil.instanceOf(input.getParent(), FUNCTIONLIKE_ELEMENT_CLASSES);
        }
    };

    private static final Predicate<JetElement> CHECK_BLOCK = new Predicate<JetElement>() {
        @Override
        public boolean apply(@Nullable JetElement input) {
            return input instanceof JetBlockExpression && !PsiTreeUtil.instanceOf(input.getParent(), FUNCTIONLIKE_ELEMENT_CLASSES);
        }
    };

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

        PsiElement blockParent = blockLikeElement.getParent();
        if (blockParent instanceof JetWhenEntry) return BraceStatus.NOT_FOUND;
        if (PsiTreeUtil.instanceOf(blockParent, FUNCTIONLIKE_ELEMENT_CLASSES)) return BraceStatus.NOT_FOUND;

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
    private static JetBlockExpression findClosestBlock(@NotNull PsiElement anchor, boolean down, boolean strict) {
        PsiElement current = PsiTreeUtil.getParentOfType(anchor, JetBlockExpression.class, strict);
        while (current != null) {
            PsiElement parent = current.getParent();
            if (parent instanceof JetClassBody ||
                parent instanceof JetClassInitializer ||
                parent instanceof JetNamedFunction ||
                (parent instanceof JetProperty && !((JetProperty) parent).isLocal())) {
                return null;
            }

            if (parent instanceof JetBlockExpression) return (JetBlockExpression) parent;

            PsiElement sibling = down ? current.getNextSibling() : current.getPrevSibling();
            if (sibling != null) {
                //noinspection unchecked
                JetBlockExpression block = (JetBlockExpression) JetPsiUtil.getOutermostDescendantElement(sibling, down, CHECK_BLOCK);
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
    private static JetBlockExpression getDSLLambdaBlock(@NotNull PsiElement element, boolean down) {
        JetCallExpression callExpression =
                (JetCallExpression) JetPsiUtil.getOutermostDescendantElement(element, down, IS_CALL_EXPRESSION);
        if (callExpression == null) return null;

        List<JetExpression> functionLiterals = callExpression.getFunctionLiteralArguments();
        if (functionLiterals.isEmpty()) return null;

        return ((JetFunctionLiteralExpression) functionLiterals.get(0)).getBodyExpression();
    }

    @Nullable
    private static LineRange getExpressionTargetRange(@NotNull Editor editor, @NotNull PsiElement sibling, boolean down) {
        PsiElement start = sibling;
        PsiElement end = sibling;

        // moving out of code block
        if (sibling.getNode().getElementType() == (down ? JetTokens.RBRACE : JetTokens.LBRACE)) {
            PsiElement parent = sibling.getParent();
            if (!(parent instanceof JetBlockExpression || parent instanceof JetFunctionLiteral)) return null;

            JetBlockExpression newBlock;
            if (parent instanceof JetFunctionLiteral) {
                //noinspection ConstantConditions
                newBlock = findClosestBlock(((JetFunctionLiteral) parent).getBodyExpression(), down, false);

                if (!down) {
                    ASTNode arrow = ((JetFunctionLiteral) parent).getArrowNode();
                    if (arrow != null) {
                        end = arrow.getPsi();
                    }
                }
            } else {
                newBlock = findClosestBlock(sibling, down, true);
            }

            if (newBlock == null) return null;

            if (PsiTreeUtil.isAncestor(newBlock, parent, true)) {
                PsiElement outermostParent = JetPsiUtil.getOutermostParent(parent, newBlock, true);

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
        // moving into code block
        else {
            PsiElement blockLikeElement;

            JetBlockExpression dslBlock = getDSLLambdaBlock(sibling, down);
            if (dslBlock != null) {
                // Use JetFunctionLiteral (since it contains braces)
                blockLikeElement = dslBlock.getParent();
            } else {
                // JetBlockExpression and other block-like elements
                blockLikeElement = JetPsiUtil.getOutermostDescendantElement(sibling, down, CHECK_BLOCK_LIKE_ELEMENT);
            }

            if (blockLikeElement != null) {
                if (down) {
                    end = JetPsiUtil.findChildByType(blockLikeElement, JetTokens.LBRACE);
                    if (blockLikeElement instanceof JetFunctionLiteral) {
                        ASTNode arrow = ((JetFunctionLiteral) blockLikeElement).getArrowNode();
                        if (arrow != null) {
                            end = arrow.getPsi();
                        }
                    }
                }
                else {
                    start = JetPsiUtil.findChildByType(blockLikeElement, JetTokens.RBRACE);
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
    private LineRange getValueParamOrArgTargetRange(
            @NotNull Editor editor,
            @NotNull PsiElement elementToCheck,
            @NotNull PsiElement sibling,
            boolean down
    ) {
        PsiElement next = sibling;

        if (next.getNode().getElementType() == JetTokens.COMMA) {
            next = firstNonWhiteSibling(next, down);
        }

        LineRange range = (next instanceof JetParameter || next instanceof JetValueArgument)
                          ? new LineRange(next, next, editor.getDocument())
                          : null;

        if (range != null) {
            parametersOrArgsToMove = new Pair<PsiElement, PsiElement>(elementToCheck, next);
        }

        return range;
    }

    @Nullable
    private LineRange getTargetRange(
            @NotNull Editor editor,
            @Nullable PsiElement elementToCheck,
            @NotNull PsiElement sibling,
            boolean down
    ) {
        if (elementToCheck instanceof JetParameter || elementToCheck instanceof JetValueArgument) {
            return getValueParamOrArgTargetRange(editor, elementToCheck, sibling, down);
        }

        if (elementToCheck instanceof JetExpression || elementToCheck instanceof PsiComment) {
            return getExpressionTargetRange(editor, sibling, down);
        }

        if (elementToCheck instanceof JetWhenEntry) {
            return getWhenEntryTargetRange(editor, sibling, down);
        }

        return null;
    }

    @Override
    protected boolean checkSourceElement(@NotNull PsiElement element) {
        return PsiTreeUtil.instanceOf(element, MOVABLE_ELEMENT_CLASSES);
    }

    @Override
    protected LineRange getElementSourceLineRange(@NotNull PsiElement element, @NotNull Editor editor, @NotNull LineRange oldRange) {
        TextRange textRange = element.getTextRange();
        if (editor.getDocument().getTextLength() < textRange.getEndOffset()) return null;

        int startLine = editor.offsetToLogicalPosition(textRange.getStartOffset()).line;
        int endLine = editor.offsetToLogicalPosition(textRange.getEndOffset()).line + 1;

        return new LineRange(startLine, endLine);
    }

    @Nullable
    private static PsiElement getMovableElement(@NotNull PsiElement element, boolean lookRight) {
        //noinspection unchecked
        PsiElement movableElement = PsiUtilPackage.getParentByTypesAndPredicate(
                element,
                false,
                MOVABLE_ELEMENT_CLASSES,
                MOVABLE_ELEMENT_CONSTRAINT
        );
        if (movableElement == null) return null;

        if (isBracelessBlock(movableElement)) {
            movableElement = firstNonWhiteElement(lookRight ? movableElement.getLastChild() : movableElement.getFirstChild(), !lookRight);
        }

        return movableElement;
    }

    private static boolean isLastOfItsKind(@NotNull PsiElement element, boolean down) {
        return getSiblingOfType(element, down, element.getClass()) == null;
    }

    private static boolean isForbiddenMove(@NotNull PsiElement element, boolean down) {
        if (element instanceof JetParameter || element instanceof JetValueArgument) {
            return isLastOfItsKind(element, down);
        }

        return false;
    }

    private static boolean isBracelessBlock(@NotNull PsiElement element) {
        if (!(element instanceof JetBlockExpression)) return false;

        JetBlockExpression block = (JetBlockExpression) element;

        return block.getLBrace() == null && block.getRBrace() == null;
    }

    protected static PsiElement adjustSibling(
            @NotNull Editor editor,
            @NotNull LineRange sourceRange,
            @NotNull MoveInfo info,
            boolean down
    ) {
        PsiElement element = down ? sourceRange.lastElement : sourceRange.firstElement;
        PsiElement sibling = down ? element.getNextSibling() : element.getPrevSibling();

        PsiElement whiteSpaceTestSubject = sibling;
        if (sibling == null) {
            PsiElement parent = element.getParent();
            if (parent != null && isBracelessBlock(parent)) {
                whiteSpaceTestSubject = down ? parent.getNextSibling() : parent.getPrevSibling();
            }
        }

        if (whiteSpaceTestSubject instanceof PsiWhiteSpace) {
            if (getElementLineCount(whiteSpaceTestSubject, editor) > 1) {
                int nearLine = down ? sourceRange.endLine : sourceRange.startLine - 1;

                info.toMove = sourceRange;
                info.toMove2 = new LineRange(nearLine, nearLine + 1);
                info.indentTarget = false;

                return null;
            }

            if (sibling != null) {
                sibling = firstNonWhiteElement(sibling, down);
            }
        }

        if (sibling == null) {
            JetCallExpression callExpression = PsiTreeUtil.getParentOfType(element, JetCallExpression.class);
            if (callExpression != null) {
                JetBlockExpression dslBlock = getDSLLambdaBlock(callExpression, down);
                if (PsiTreeUtil.isAncestor(dslBlock, element, false)) {
                    //noinspection ConstantConditions
                    PsiElement blockParent = dslBlock.getParent();
                    return down
                           ? JetPsiUtil.findChildByType(blockParent, JetTokens.RBRACE)
                           : JetPsiUtil.findChildByType(blockParent, JetTokens.LBRACE);
                }
            }

            info.toMove2 = null;
            return null;
        }

        return sibling;
    }

    @Override
    public boolean checkAvailable(@NotNull Editor editor, @NotNull PsiFile file, @NotNull MoveInfo info, boolean down) {
        parametersOrArgsToMove = null;

        if (!super.checkAvailable(editor, file, info, down)) return false;

        switch (checkForMovableClosingBrace(editor, file, info, down)) {
            case NOT_MOVABLE: {
                info.toMove2 = null;
                return true;
            }
            case MOVABLE:
                return true;
            default:
                break;
        }

        LineRange oldRange = info.toMove;

        Pair<PsiElement, PsiElement> psiRange = getElementRange(editor, file, oldRange);
        if (psiRange == null) return false;

        //noinspection unchecked
        PsiElement firstElement = getMovableElement(psiRange.getFirst(), false);
        PsiElement lastElement = getMovableElement(psiRange.getSecond(), true);

        if (firstElement == null || lastElement == null) return false;

        if (isForbiddenMove(firstElement, down) || isForbiddenMove(lastElement, down)) {
            info.toMove2 = null;
            return true;
        }

        if ((firstElement instanceof JetParameter || firstElement instanceof JetValueArgument) &&
            PsiTreeUtil.isAncestor(lastElement, firstElement, false)) {
            lastElement = firstElement;
        }

        LineRange sourceRange = getSourceRange(firstElement, lastElement, editor, oldRange);
        if (sourceRange == null) return false;

        PsiElement sibling = getLastNonWhiteSiblingInLine(adjustSibling(editor, sourceRange, info, down), editor, down);

        // Either reached last sibling, or jumped over multi-line whitespace
        if (sibling == null) return true;

        info.toMove = sourceRange;
        info.toMove2 = getTargetRange(editor, sourceRange.firstElement, sibling, down);
        return true;
    }

    @Nullable
    private Pair<PsiElement, PsiElement> parametersOrArgsToMove;

    private static PsiElement getComma(@NotNull PsiElement element) {
        PsiElement sibling = firstNonWhiteSibling(element, true);
        return sibling != null && (sibling.getNode().getElementType() == JetTokens.COMMA) ? sibling : null;
    }

    private static void fixCommaIfNeeded(@NotNull PsiElement element, boolean willBeLast) {
        PsiElement comma = getComma(element);
        if (willBeLast && comma != null) {
            comma.delete();
        }
        else if (!willBeLast && comma == null) {
            PsiElement parent = element.getParent();
            assert parent != null;

            parent.addAfter(JetPsiFactory.createComma(parent.getProject()), element);
        }
    }

    @Override
    public void beforeMove(@NotNull Editor editor, @NotNull MoveInfo info, boolean down) {
        if (parametersOrArgsToMove != null) {
            PsiElement element1 = parametersOrArgsToMove.first;
            PsiElement element2 = parametersOrArgsToMove.second;

            fixCommaIfNeeded(element1, down && isLastOfItsKind(element2, true));
            fixCommaIfNeeded(element2, !down && isLastOfItsKind(element1, true));

            //noinspection ConstantConditions
            PsiDocumentManager.getInstance(editor.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        }
    }
}
