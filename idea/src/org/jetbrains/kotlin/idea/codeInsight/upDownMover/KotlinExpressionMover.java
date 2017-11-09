/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.codeInsight.upDownMover;

import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtilKt;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;

import java.util.List;
import java.util.function.Predicate;

public class KotlinExpressionMover extends AbstractKotlinUpDownMover {

    private static final Predicate<KtElement> IS_CALL_EXPRESSION = input -> input instanceof KtCallExpression;

    public KotlinExpressionMover() {
    }

    private final static Class[] MOVABLE_ELEMENT_CLASSES = {
            KtExpression.class,
            KtWhenEntry.class,
            KtValueArgument.class,
            PsiComment.class
    };

    private static final Function1<PsiElement, Boolean> MOVABLE_ELEMENT_CONSTRAINT = new Function1<PsiElement, Boolean>() {
        @NotNull
        @Override
        public Boolean invoke(PsiElement element) {
            return (!(element instanceof KtExpression)
                    || element instanceof KtDeclaration
                    || element instanceof KtBlockExpression
                    || element.getParent() instanceof KtBlockExpression);
        }
    };

    private final static Class[] BLOCKLIKE_ELEMENT_CLASSES =
            {KtBlockExpression.class, KtWhenExpression.class, KtClassBody.class, KtFile.class};

    private final static Class[] FUNCTIONLIKE_ELEMENT_CLASSES =
            {KtFunction.class, KtPropertyAccessor.class, KtAnonymousInitializer.class};

    private static final Predicate<KtElement> CHECK_BLOCK_LIKE_ELEMENT =
            input -> (input instanceof KtBlockExpression || input instanceof KtClassBody)
                     && !PsiTreeUtil.instanceOf(input.getParent(), FUNCTIONLIKE_ELEMENT_CLASSES);

    private static final Predicate<KtElement> CHECK_BLOCK =
            input -> input instanceof KtBlockExpression && !PsiTreeUtil.instanceOf(input.getParent(), FUNCTIONLIKE_ELEMENT_CLASSES);

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

            if (sibling instanceof KtExpression) {
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
        PsiElement prev = KtPsiUtil.getLastChildByType(block, KtExpression.class);
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
        if (!(blockLikeElement instanceof KtBlockExpression)) return BraceStatus.NOT_MOVABLE;

        PsiElement blockParent = blockLikeElement.getParent();
        if (blockParent instanceof KtWhenEntry) return BraceStatus.NOT_FOUND;
        if (PsiTreeUtil.instanceOf(blockParent, FUNCTIONLIKE_ELEMENT_CLASSES)) return BraceStatus.NOT_FOUND;

        PsiElement enclosingExpression = PsiTreeUtil.getParentOfType(blockLikeElement, KtExpression.class);

        if (enclosingExpression instanceof KtDoWhileExpression) return BraceStatus.NOT_MOVABLE;

        if (enclosingExpression instanceof KtIfExpression) {
            KtIfExpression ifExpression = (KtIfExpression) enclosingExpression;

            if (blockLikeElement == ifExpression.getThen() && ifExpression.getElse() != null) return BraceStatus.NOT_MOVABLE;
        }

        return down
               ? checkForMovableDownClosingBrace(closingBrace, blockLikeElement, editor, info)
               : checkForMovableUpClosingBrace(closingBrace, blockLikeElement, editor, info);
    }

    @Nullable
    private static KtBlockExpression findClosestBlock(@NotNull PsiElement anchor, boolean down, boolean strict) {
        PsiElement current = PsiTreeUtil.getParentOfType(anchor, KtBlockExpression.class, strict);
        while (current != null) {
            PsiElement parent = current.getParent();
            if (parent instanceof KtClassBody ||
                (parent instanceof KtAnonymousInitializer && !(parent instanceof KtScriptInitializer)) ||
                parent instanceof KtNamedFunction ||
                (parent instanceof KtProperty && !((KtProperty) parent).isLocal())) {
                return null;
            }

            if (parent instanceof KtBlockExpression) return (KtBlockExpression) parent;

            PsiElement sibling = down ? current.getNextSibling() : current.getPrevSibling();
            if (sibling != null) {
                //noinspection unchecked
                KtBlockExpression block = (KtBlockExpression) KtPsiUtil.getOutermostDescendantElement(sibling, down, CHECK_BLOCK);
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
    private static KtBlockExpression getDSLLambdaBlock(@NotNull PsiElement element, boolean down) {
        KtCallExpression callExpression =
                (KtCallExpression) KtPsiUtil.getOutermostDescendantElement(element, down, IS_CALL_EXPRESSION);
        if (callExpression == null) return null;

        List<KtLambdaArgument> functionLiterals = callExpression.getLambdaArguments();
        if (functionLiterals.isEmpty()) return null;

        return functionLiterals.get(0).getLambdaExpression().getBodyExpression();
    }

    @Nullable
    private static LineRange getExpressionTargetRange(@NotNull Editor editor, @NotNull PsiElement sibling, boolean down) {
        if (sibling instanceof KtIfExpression && !down) {
            KtExpression elseBranch = ((KtIfExpression) sibling).getElse();
            if (elseBranch instanceof KtBlockExpression) {
                sibling = elseBranch;
            }
        }

        PsiElement start = sibling;
        PsiElement end = sibling;

        // moving out of code block
        if (sibling.getNode().getElementType() == (down ? KtTokens.RBRACE : KtTokens.LBRACE)) {
            PsiElement parent = sibling.getParent();
            if (!(parent instanceof KtBlockExpression || parent instanceof KtFunctionLiteral)) return null;

            KtBlockExpression newBlock;
            if (parent instanceof KtFunctionLiteral) {
                //noinspection ConstantConditions
                newBlock = findClosestBlock(((KtFunctionLiteral) parent).getBodyExpression(), down, false);

                if (!down) {
                    PsiElement arrow = ((KtFunctionLiteral) parent).getArrow();
                    if (arrow != null) {
                        end = arrow;
                    }
                }
            } else {
                newBlock = findClosestBlock(sibling, down, true);
            }

            if (newBlock == null) return null;

            if (PsiTreeUtil.isAncestor(newBlock, parent, true)) {
                PsiElement outermostParent = KtPsiUtil.getOutermostParent(parent, newBlock, true);

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

            KtBlockExpression dslBlock = getDSLLambdaBlock(sibling, down);
            if (dslBlock != null) {
                // Use JetFunctionLiteral (since it contains braces)
                blockLikeElement = dslBlock.getParent();
            } else {
                // JetBlockExpression and other block-like elements
                blockLikeElement = KtPsiUtil.getOutermostDescendantElement(sibling, down, CHECK_BLOCK_LIKE_ELEMENT);
            }

            if (blockLikeElement != null) {
                if (down) {
                    end = KtPsiUtil.findChildByType(blockLikeElement, KtTokens.LBRACE);
                    if (blockLikeElement instanceof KtFunctionLiteral) {
                        PsiElement arrow = ((KtFunctionLiteral) blockLikeElement).getArrow();
                        if (arrow != null) {
                            end = arrow;
                        }
                    }
                }
                else {
                    start = KtPsiUtil.findChildByType(blockLikeElement, KtTokens.RBRACE);
                }
            }
        }

        return start != null && end != null ? new LineRange(start, end, editor.getDocument()) : null;
    }

    @Nullable
    private static LineRange getWhenEntryTargetRange(@NotNull Editor editor, @NotNull PsiElement sibling, boolean down) {
        if (sibling.getNode().getElementType() == (down ? KtTokens.RBRACE : KtTokens.LBRACE) &&
            PsiTreeUtil.getParentOfType(sibling, KtWhenEntry.class) == null) {
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

        if (next.getNode().getElementType() == KtTokens.COMMA) {
            next = firstNonWhiteSibling(next, down);
        }

        LineRange range = (next instanceof KtParameter || next instanceof KtValueArgument)
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
        if (elementToCheck instanceof KtParameter || elementToCheck instanceof KtValueArgument) {
            return getValueParamOrArgTargetRange(editor, elementToCheck, sibling, down);
        }

        if (elementToCheck instanceof KtExpression || elementToCheck instanceof PsiComment) {
            return getExpressionTargetRange(editor, sibling, down);
        }

        if (elementToCheck instanceof KtWhenEntry) {
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
        PsiElement movableElement = PsiUtilsKt.getParentOfTypesAndPredicate(
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
        if (element instanceof KtParameter || element instanceof KtValueArgument) {
            return isLastOfItsKind(element, down);
        }

        return false;
    }

    private static boolean isBracelessBlock(@NotNull PsiElement element) {
        if (!(element instanceof KtBlockExpression)) return false;

        KtBlockExpression block = (KtBlockExpression) element;

        return block.getLBrace() == null && block.getRBrace() == null;
    }

    protected static PsiElement adjustSibling(
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
            if (KotlinRefactoringUtilKt.isMultiLine(whiteSpaceTestSubject)) {
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
            KtCallExpression callExpression = PsiTreeUtil.getParentOfType(element, KtCallExpression.class);
            if (callExpression != null) {
                KtBlockExpression dslBlock = getDSLLambdaBlock(callExpression, down);
                if (PsiTreeUtil.isAncestor(dslBlock, element, false)) {
                    //noinspection ConstantConditions
                    PsiElement blockParent = dslBlock.getParent();
                    return down
                           ? KtPsiUtil.findChildByType(blockParent, KtTokens.RBRACE)
                           : KtPsiUtil.findChildByType(blockParent, KtTokens.LBRACE);
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

        if ((firstElement instanceof KtParameter || firstElement instanceof KtValueArgument) &&
            PsiTreeUtil.isAncestor(lastElement, firstElement, false)) {
            lastElement = firstElement;
        }

        LineRange sourceRange = getSourceRange(firstElement, lastElement, editor, oldRange);
        if (sourceRange == null) return false;

        PsiElement sibling = getLastNonWhiteSiblingInLine(adjustSibling(sourceRange, info, down), editor, down);

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
        return sibling != null && (sibling.getNode().getElementType() == KtTokens.COMMA) ? sibling : null;
    }

    private static void fixCommaIfNeeded(@NotNull PsiElement element, boolean willBeLast) {
        PsiElement comma = getComma(element);
        if (willBeLast && comma != null) {
            comma.delete();
        }
        else if (!willBeLast && comma == null) {
            PsiElement parent = element.getParent();
            assert parent != null;

            parent.addAfter(KtPsiFactoryKt.KtPsiFactory(parent.getProject()).createComma(), element);
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
