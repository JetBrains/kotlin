/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.codeInsight.upDownMover

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange
import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.util.getLineCount
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaHelper
import org.jetbrains.kotlin.idea.util.isComma
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.function.Predicate

class KotlinExpressionMover : AbstractKotlinUpDownMover() {
    private enum class BraceStatus {
        NOT_FOUND, MOVABLE, NOT_MOVABLE
    }

    private fun getValueParamOrArgTargetRange(
        editor: Editor,
        elementToCheck: PsiElement,
        sibling: PsiElement,
        down: Boolean
    ): LineRange? {
        val next = if (sibling.node.elementType === KtTokens.COMMA || sibling is PsiComment) {
            firstNonWhiteSibling(sibling, down)
        } else {
            sibling
        }

        if (next != null) {
            val afterNext = firstNonWhiteSibling(next, true)
            if (afterNext?.node?.elementType == KtTokens.RPAR &&
                getElementLine(afterNext, editor, true) == getElementLine(next, editor, false)
            ) return null
        }

        return next?.takeIf { it is KtParameter || it is KtValueArgument }?.let { LineRange(it, it, editor.document) }?.also {
            parametersOrArgsToMove = elementToCheck to next
        }
    }

    private fun getTargetRange(
        editor: Editor,
        elementToCheck: PsiElement?,
        sibling: PsiElement,
        down: Boolean
    ): LineRange? = when (elementToCheck) {
        is KtParameter, is KtValueArgument -> getValueParamOrArgTargetRange(editor, elementToCheck, sibling, down)
        is KtExpression, is PsiComment -> getExpressionTargetRange(editor, elementToCheck, sibling, down)
        is KtWhenEntry -> getWhenEntryTargetRange(editor, sibling, down)
        else -> null
    }

    override fun checkSourceElement(element: PsiElement): Boolean =
        PsiTreeUtil.instanceOf(element, *MOVABLE_ELEMENT_CLASSES) || element.node.elementType === KtTokens.SEMICOLON

    override fun getElementSourceLineRange(
        element: PsiElement,
        editor: Editor,
        oldRange: LineRange
    ): LineRange? {
        val textRange = element.textRange
        if (editor.document.textLength < textRange.endOffset) return null
        val startLine = editor.offsetToLogicalPosition(textRange.startOffset).line
        val endLine = editor.offsetToLogicalPosition(textRange.endOffset).line + 1
        return LineRange(startLine, endLine)
    }

    override fun checkAvailable(
        editor: Editor,
        file: PsiFile,
        info: MoveInfo,
        down: Boolean
    ): Boolean {
        parametersOrArgsToMove = null
        if (!super.checkAvailable(editor, file, info, down)) return false

        when (checkForMovableClosingBrace(editor, file, info, down)) {
            BraceStatus.NOT_MOVABLE -> {
                info.toMove2 = null
                return true
            }
            BraceStatus.MOVABLE -> return true
            else -> {
            }
        }

        val oldRange = info.toMove
        val psiRange = StatementUpDownMover.getElementRange(editor, file, oldRange) ?: return false
        val firstElement = getMovableElement(psiRange.getFirst(), false) ?: return false
        var lastElement = getMovableElement(psiRange.getSecond(), true) ?: return false
        if (isForbiddenMove(editor, firstElement, down) || isForbiddenMove(editor, lastElement, down)) {
            info.toMove2 = null
            return true
        }

        if ((firstElement is KtParameter || firstElement is KtValueArgument) &&
            PsiTreeUtil.isAncestor(lastElement, firstElement, false)
        ) {
            lastElement = firstElement
        }
        val sourceRange = getSourceRange(firstElement, lastElement, editor, oldRange) ?: return false
        val sibling = getLastNonWhiteSiblingInLine(adjustSibling(editor, sourceRange, info, down), editor, down) ?: return true

        // Either reached last sibling, or jumped over multi-line whitespace
        info.toMove = sourceRange
        info.toMove2 = getTargetRange(editor, sourceRange.firstElement, sibling, down)
        return true
    }

    private var parametersOrArgsToMove: Pair<PsiElement, PsiElement>? = null

    override fun beforeMove(
        editor: Editor,
        info: MoveInfo,
        down: Boolean
    ) {
        if (parametersOrArgsToMove != null) {
            val (first, second) = parametersOrArgsToMove ?: return
            parametersOrArgsToMove = null

            val lastElementOnFirstLine = getLastSiblingOfSameTypeInLine(first, editor)
            val lastElementOnSecondLine = getLastSiblingOfSameTypeInLine(second, editor)
            val withTrailingComma = lastElementOnFirstLine.parent
                ?.safeAs<KtElement>()
                ?.let {
                    TrailingCommaHelper.trailingCommaExistsOrCanExist(it, CodeStyle.getSettings(it.project))
                } == true

            fixCommaIfNeeded(lastElementOnFirstLine, down && isLastOfItsKind(lastElementOnSecondLine, true), withTrailingComma)
            fixCommaIfNeeded(lastElementOnSecondLine, !down && isLastOfItsKind(lastElementOnFirstLine, true), withTrailingComma)
            editor.project?.let { PsiDocumentManager.getInstance(it).doPostponedOperationsAndUnblockDocument(editor.document) }
        }
    }

    companion object {
        private val IS_CALL_EXPRESSION = Predicate { input: KtElement? -> input is KtCallExpression }
        private val MOVABLE_ELEMENT_CLASSES: Array<Class<out PsiElement>> = arrayOf(
            KtExpression::class.java,
            KtWhenEntry::class.java,
            KtValueArgument::class.java,
            PsiComment::class.java
        )

        private val MOVABLE_ELEMENT_CONSTRAINT = { element: PsiElement ->
            (element !is KtExpression || element is KtDeclaration || element is KtBlockExpression || element.getParent() is KtBlockExpression)
        }

        private val BLOCKLIKE_ELEMENT_CLASSES: Array<Class<out PsiElement>> = arrayOf(
            KtBlockExpression::class.java,
            KtWhenExpression::class.java,
            KtClassBody::class.java,
            KtFile::class.java
        )

        private val FUNCTIONLIKE_ELEMENT_CLASSES: Array<Class<out PsiElement>> = arrayOf(
            KtFunction::class.java,
            KtPropertyAccessor::class.java,
            KtAnonymousInitializer::class.java
        )

        private val CHECK_BLOCK_LIKE_ELEMENT = Predicate { input: KtElement ->
            (input is KtBlockExpression || input is KtClassBody) && !PsiTreeUtil.instanceOf(input.parent, *FUNCTIONLIKE_ELEMENT_CLASSES)
        }

        private val CHECK_BLOCK = Predicate { input: KtElement ->
            input is KtBlockExpression && !PsiTreeUtil.instanceOf(input.getParent(), *FUNCTIONLIKE_ELEMENT_CLASSES)
        }

        private fun getStandaloneClosingBrace(
            file: PsiFile,
            editor: Editor
        ): PsiElement? {
            val range = StatementUpDownMover.getLineRangeFromSelection(editor)
            if (range.endLine - range.startLine != 1) return null
            val offset = editor.caretModel.offset
            val document = editor.document
            val line = document.getLineNumber(offset)
            val lineStartOffset = document.getLineStartOffset(line)
            val lineText = document.text.substring(lineStartOffset, document.getLineEndOffset(line))
            return if (lineText.trim { it <= ' ' } != "}") null else file.findElementAt(lineStartOffset + lineText.indexOf('}'))
        }

        private fun checkForMovableDownClosingBrace(
            closingBrace: PsiElement,
            block: PsiElement,
            editor: Editor,
            info: MoveInfo
        ): BraceStatus {
            var current: PsiElement? = block
            var nextElement: PsiElement? = null
            var nextExpression: PsiElement? = null
            do {
                val sibling = StatementUpDownMover.firstNonWhiteElement(current?.nextSibling, true)
                if (sibling != null && nextElement == null) {
                    nextElement = sibling
                }

                if (sibling is KtExpression) {
                    nextExpression = sibling
                    break
                }

                current = current?.parent
            } while (current != null && !PsiTreeUtil.instanceOf(current, *BLOCKLIKE_ELEMENT_CLASSES))

            if (nextExpression == null) return BraceStatus.NOT_MOVABLE
            val doc = editor.document
            info.toMove = LineRange(closingBrace, closingBrace, doc)
            info.toMove2 = nextElement?.let { LineRange(it, nextExpression) }
            info.indentSource = true
            return BraceStatus.MOVABLE
        }

        private fun checkForMovableUpClosingBrace(
            closingBrace: PsiElement,
            block: PsiElement,
            editor: Editor,
            info: MoveInfo
        ): BraceStatus {
            val prev = KtPsiUtil.getLastChildByType(block, KtExpression::class.java) ?: return BraceStatus.NOT_MOVABLE
            val doc = editor.document
            info.toMove = LineRange(closingBrace, closingBrace, doc)
            info.toMove2 = LineRange(prev, prev, doc)
            info.indentSource = true
            return BraceStatus.MOVABLE
        }

        // Returns null if standalone closing brace is not found
        private fun checkForMovableClosingBrace(
            editor: Editor,
            file: PsiFile,
            info: MoveInfo,
            down: Boolean
        ): BraceStatus {
            val closingBrace = getStandaloneClosingBrace(file, editor) ?: return BraceStatus.NOT_FOUND
            val blockLikeElement = closingBrace.parent as? KtBlockExpression ?: return BraceStatus.NOT_MOVABLE
            val blockParent = blockLikeElement.parent
            if (blockParent is KtWhenEntry) return BraceStatus.NOT_FOUND
            if (PsiTreeUtil.instanceOf(blockParent, *FUNCTIONLIKE_ELEMENT_CLASSES)) return BraceStatus.NOT_FOUND

            val enclosingExpression: PsiElement? = PsiTreeUtil.getParentOfType(blockLikeElement, KtExpression::class.java)
            return when {
                enclosingExpression is KtDoWhileExpression -> BraceStatus.NOT_MOVABLE
                enclosingExpression is KtIfExpression && blockLikeElement === enclosingExpression.then && enclosingExpression.getElse() != null -> BraceStatus.NOT_MOVABLE
                down -> checkForMovableDownClosingBrace(closingBrace, blockLikeElement, editor, info)
                else -> checkForMovableUpClosingBrace(closingBrace, blockLikeElement, editor, info)
            }
        }

        private fun findClosestBlock(
            anchor: PsiElement,
            down: Boolean,
            strict: Boolean
        ): KtBlockExpression? {
            var current: PsiElement? = PsiTreeUtil.getParentOfType(anchor, KtBlockExpression::class.java, strict)
            while (current != null) {
                val parent = current.parent
                if (parent is KtClassBody ||
                    parent is KtAnonymousInitializer && parent !is KtScriptInitializer ||
                    parent is KtNamedFunction ||
                    parent is KtProperty && !parent.isLocal
                ) {
                    return null
                }

                if (parent is KtBlockExpression) return parent
                val sibling = if (down) current.nextSibling else current.prevSibling
                current = if (sibling != null) {
                    val block = KtPsiUtil.getOutermostDescendantElement(sibling, down, CHECK_BLOCK) as? KtBlockExpression
                    if (block != null) return block
                    sibling
                } else {
                    parent
                }
            }

            return null
        }

        private fun getDSLLambdaBlock(
            editor: Editor,
            element: PsiElement,
            down: Boolean
        ): KtBlockExpression? {
            if (element is KtIfExpression ||
                element is KtWhenExpression ||
                element is KtWhenEntry ||
                element is KtTryExpression ||
                element is KtFinallySection ||
                element is KtCatchClause ||
                element is KtLoopExpression
            ) return null

            (element as? KtQualifiedExpression)?.selectorExpression?.let {
                return getDSLLambdaBlock(editor, it, down)
            }

            val callExpression =
                KtPsiUtil.getOutermostDescendantElement(element, down, IS_CALL_EXPRESSION) as KtCallExpression? ?: return null
            val functionLiterals = callExpression.lambdaArguments
            if (functionLiterals.isEmpty()) return null
            val lambdaExpression = functionLiterals.firstOrNull()?.getLambdaExpression() ?: return null
            val document = editor.document
            val range = lambdaExpression.textRange
            return if (document.getLineNumber(range.startOffset) == document.getLineNumber(range.endOffset)) null else lambdaExpression.bodyExpression
        }

        private fun getExpressionTargetRange(
            editor: Editor,
            elementToCheck: PsiElement,
            sibling: PsiElement,
            down: Boolean
        ): LineRange? {
            var currentSibling = sibling
            var start: PsiElement? = currentSibling
            var end: PsiElement? = currentSibling
            if (!down) {
                when (currentSibling) {
                    is KtIfExpression -> {
                        var elseExpression = currentSibling.getElse()
                        while (elseExpression is KtIfExpression) {
                            val elseIfExpression = elseExpression
                            val next = elseIfExpression.getElse()
                            if (next == null) {
                                elseExpression = elseIfExpression.then
                                break
                            }

                            elseExpression = next
                        }

                        if (elseExpression is KtBlockExpression) {
                            currentSibling = elseExpression
                            start = currentSibling
                        }
                    }
                    is KtWhenExpression -> {
                        val entries = currentSibling.entries
                        if (entries.isNotEmpty()) {
                            var lastEntry: KtWhenEntry? = null
                            for (entry in entries) {
                                if (entry.expression is KtBlockExpression) lastEntry = entry
                            }

                            if (lastEntry != null) {
                                currentSibling = lastEntry
                                start = currentSibling
                            }
                        }
                    }
                    is KtTryExpression -> {
                        val tryExpression = currentSibling
                        val finallyBlock = tryExpression.finallyBlock
                        if (finallyBlock != null) {
                            currentSibling = finallyBlock
                            start = currentSibling
                        } else {
                            val clauses = tryExpression.catchClauses
                            if (clauses.isNotEmpty()) {
                                currentSibling = clauses[clauses.size - 1]
                                start = currentSibling
                            }
                        }
                    }
                }
            }

            // moving out of code block
            if (currentSibling.node.elementType === (if (down) KtTokens.RBRACE else KtTokens.LBRACE)) {
                val parent = currentSibling.parent
                if (!(parent is KtBlockExpression || parent is KtFunctionLiteral)) return null
                val newBlock: KtBlockExpression?
                if (parent is KtFunctionLiteral) {
                    newBlock = parent.bodyExpression?.let { findClosestBlock(it, down, false) }
                    if (!down) {
                        val arrow = parent.arrow
                        if (arrow != null) {
                            end = arrow
                        }
                    }
                } else {
                    newBlock = findClosestBlock(currentSibling, down, true)
                }
                if (newBlock == null) return null
                if (PsiTreeUtil.isAncestor(newBlock, parent, true)) {
                    val outermostParent = KtPsiUtil.getOutermostParent(parent, newBlock, true)
                    if (down) {
                        end = outermostParent
                    } else {
                        start = outermostParent
                    }
                } else {
                    if (down) {
                        end = newBlock.lBrace
                    } else {
                        start = newBlock.rBrace
                    }
                }
            } else {
                val blockLikeElement: PsiElement?
                val dslBlock = getDSLLambdaBlock(editor, currentSibling, down)
                blockLikeElement = if (dslBlock != null) {
                    // Use JetFunctionLiteral (since it contains braces)
                    dslBlock.parent
                } else {
                    // JetBlockExpression and other block-like elements
                    KtPsiUtil.getOutermostDescendantElement(currentSibling, down, CHECK_BLOCK_LIKE_ELEMENT)
                }

                if (blockLikeElement != null) {
                    if (down) {
                        end = KtPsiUtil.findChildByType(blockLikeElement, KtTokens.LBRACE)
                        if (blockLikeElement is KtFunctionLiteral) {
                            val arrow = blockLikeElement.arrow
                            if (arrow != null) {
                                end = arrow
                            }
                        }
                    } else {
                        start = KtPsiUtil.findChildByType(blockLikeElement, KtTokens.RBRACE)
                    }
                }
            }
            if (elementToCheck !is PsiComment) {
                val extended = extendForSiblingComments(start, end, currentSibling, editor, down)
                if (extended != null) {
                    start = extended.first
                    end = extended.second
                }
            }
            return if (start != null && end != null) LineRange(start, end, editor.document) else null
        }

        private fun extendForSiblingComments(
            start: PsiElement?, end: PsiElement?, sibling: PsiElement,
            editor: Editor, down: Boolean
        ): Pair<PsiElement, PsiElement>? {
            var currentStart = start
            var currentEnd = end
            if (!(currentStart === currentEnd && currentStart === sibling)) return null

            var hasUpdate = false
            var current: PsiElement? = sibling
            while (true) {
                val nextLine = getElementLine(current, editor, !down) + if (down) 1 else -1
                current = current?.let { firstNonWhiteSibling(it, down) }
                if (current !is PsiComment) {
                    break
                }

                if (getElementLine(current, editor, down) != nextLine) {
                    // An empty line is between current element and next sibling
                    break
                }

                hasUpdate = true
                if (down) {
                    currentEnd = current
                } else {
                    currentStart = current
                }
            }

            if (down && currentEnd is PsiComment) {
                val next = firstNonWhiteSibling(currentEnd, true)
                if (getElementLine(next, editor, true) == getElementLine(currentEnd, editor, false) + 1) {
                    hasUpdate = true
                    currentEnd = next
                }
            }

            val resultStart = currentStart ?: return null
            val resultEnd = currentEnd ?: return null
            return if (hasUpdate) resultStart to resultEnd else null
        }

        private fun getWhenEntryTargetRange(
            editor: Editor,
            sibling: PsiElement,
            down: Boolean
        ): LineRange? =
            if (sibling.node.elementType === (if (down) KtTokens.RBRACE else KtTokens.LBRACE) &&
                PsiTreeUtil.getParentOfType(sibling, KtWhenEntry::class.java) == null
            )
                null
            else
                LineRange(sibling, sibling, editor.document)

        private fun getMovableElement(element: PsiElement, lookRight: Boolean): PsiElement? {
            if (element.node.elementType === KtTokens.SEMICOLON) {
                return element
            }

            if (getParentFileAnnotationEntry(element) != null) return null

            val movableElement = element.getParentOfTypesAndPredicate(
                strict = false,
                parentClasses = *MOVABLE_ELEMENT_CLASSES,
                predicate = MOVABLE_ELEMENT_CONSTRAINT
            ) ?: return null

            return if (isBracelessBlock(movableElement)) {
                StatementUpDownMover.firstNonWhiteElement(
                    if (lookRight)
                        movableElement.lastChild
                    else
                        movableElement.firstChild, !lookRight
                )
            } else {
                movableElement
            }
        }

        private fun isLastOfItsKind(element: PsiElement, down: Boolean): Boolean =
            getSiblingOfType(element, down, element.javaClass) == null

        private fun isForbiddenMove(editor: Editor, element: PsiElement, down: Boolean): Boolean {
            if (element is KtParameter || element is KtValueArgument) {
                val next = firstNonWhiteSibling(element, true)
                if (next?.node?.elementType == KtTokens.RPAR &&
                    getElementLine(next, editor, true) == getElementLine(element, editor, false)
                ) return true
                return isLastOfItsKind(element, down)
            }
            return false
        }

        private fun isBracelessBlock(element: PsiElement): Boolean =
            if (element !is KtBlockExpression)
                false
            else
                element.lBrace == null && element.rBrace == null

        private fun adjustSibling(
            editor: Editor,
            sourceRange: LineRange,
            info: MoveInfo,
            down: Boolean
        ): PsiElement? {
            val element = if (down) sourceRange.lastElement else sourceRange.firstElement
            var sibling = if (down) {
                val elementToCheck = sourceRange.firstElement
                if (element is PsiComment && (elementToCheck is KtParameter || elementToCheck is KtValueArgument)) {
                    element.getPrevSiblingIgnoringWhitespaceAndComments()
                } else {
                    element.nextSibling
                }
            } else {
                element.prevSibling
            }

            val whiteSpaceTestSubject = sibling ?: kotlin.run {
                val parent = element.parent
                if (parent == null || !isBracelessBlock(parent)) return@run null

                if (down) parent.nextSibling else parent.prevSibling
            }

            if (whiteSpaceTestSubject is PsiWhiteSpace) {
                if (whiteSpaceTestSubject.getLineCount() >= 3) {
                    val nearLine = if (down) sourceRange.endLine else sourceRange.startLine - 1
                    info.toMove = sourceRange
                    info.toMove2 = LineRange(nearLine, nearLine + 1)
                    info.indentTarget = false
                    return null
                }

                if (sibling != null) sibling = StatementUpDownMover.firstNonWhiteElement(sibling, down)
            }

            if (sibling != null) return sibling
            val callExpression = PsiTreeUtil.getParentOfType(element, KtCallExpression::class.java)
            if (callExpression != null) {
                val dslBlock = getDSLLambdaBlock(editor, callExpression, down)
                if (PsiTreeUtil.isAncestor(dslBlock, element, false)) {
                    dslBlock?.parent?.let { blockParent ->
                        return if (down)
                            KtPsiUtil.findChildByType(blockParent, KtTokens.RBRACE)
                        else
                            KtPsiUtil.findChildByType(blockParent, KtTokens.LBRACE)
                    }
                }
            }

            info.toMove2 = null
            return null
        }

        private fun getComma(element: PsiElement): PsiElement? = firstNonWhiteSibling(element, true)?.takeIf(PsiElement::isComma)

        private fun fixCommaIfNeeded(element: PsiElement, willBeLast: Boolean, withTrailingComma: Boolean) {
            val comma = getComma(element)
            if (willBeLast && comma != null && !withTrailingComma) {
                comma.delete()
            } else if (!willBeLast && comma == null) {
                element.children.lastOrNull()?.let {
                    element.addAfter(KtPsiFactory(element.project).createComma(), it)
                }
            }
        }

        private fun getLastSiblingOfSameTypeInLine(
            element: PsiElement,
            editor: Editor
        ): PsiElement {
            var lastElement = element
            val lineNumber = getElementLine(element, editor, true)
            while (true) {
                val nextElement = PsiTreeUtil.getNextSiblingOfType(lastElement, lastElement.javaClass)
                lastElement = if (nextElement != null && getElementLine(nextElement, editor, true) == lineNumber) {
                    nextElement
                } else {
                    break
                }
            }

            return lastElement
        }
    }
}