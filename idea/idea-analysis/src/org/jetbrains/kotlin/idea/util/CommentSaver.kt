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

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.*
import java.util.*
import kotlin.properties.Delegates

class CommentSaver(originalElements: PsiChildRange, private val saveLineBreaks: Boolean = false/*TODO?*/) {
    constructor(originalElement: PsiElement, saveLineBreaks: Boolean = false/*TODO?*/) : this(
        PsiChildRange.singleElement(originalElement),
        saveLineBreaks
    )

    private val SAVED_TREE_KEY = Key<TreeElement>("SAVED_TREE")
    private val psiFactory = KtPsiFactory(originalElements.first!!)

    private abstract class TreeElement {
        companion object {
            fun create(element: PsiElement): TreeElement? {
                val tokenType = element.tokenType
                return when {
                    element is PsiWhiteSpace -> if (element.textContains('\n')) LineBreakTreeElement() else null
                    element is PsiComment -> CommentTreeElement.create(element)
                    tokenType != null -> TokenTreeElement(tokenType)
                    else -> if (element.textLength > 0) StandardTreeElement() else null // don't save empty elements
                }
            }
        }

        var parent: TreeElement? = null
        var prev: TreeElement? = null
        var next: TreeElement? = null
        var firstChild: TreeElement? = null
        var lastChild: TreeElement? = null

        val children: Sequence<TreeElement>
            get() = generateSequence({ firstChild }, { it.next })

        val reverseChildren: Sequence<TreeElement>
            get() = generateSequence({ lastChild }, { it.prev })

        val prevSiblings: Sequence<TreeElement>
            get() = generateSequence({ prev }, { it.prev })

        val nextSiblings: Sequence<TreeElement>
            get() = generateSequence({ next }, { it.next })

        val parents: Sequence<TreeElement>
            get() = generateSequence({ parent }, { it.parent })

        val parentsWithSelf: Sequence<TreeElement>
            get() = generateSequence(this, { it.parent })

        val firstLeafInside: TreeElement
            get() {
                var result = this
                while (true) {
                    result = result.firstChild ?: break
                }
                return result
            }

        val lastLeafInside: TreeElement
            get() {
                var result = this
                while (true) {
                    result = result.lastChild ?: break
                }
                return result
            }

        val prevLeaf: TreeElement?
            get() {
                return (prev ?: return parent?.prevLeaf).lastLeafInside
            }

        val nextLeaf: TreeElement?
            get() {
                return (next ?: return parent?.nextLeaf).firstLeafInside
            }

        val prevLeafs: Sequence<TreeElement>
            get() = generateSequence({ prevLeaf }, { it.prevLeaf })

        val nextLeafs: Sequence<TreeElement>
            get() = generateSequence({ nextLeaf }, { it.nextLeaf })

        fun withDescendants(leftToRight: Boolean): Sequence<TreeElement> {
            val children = if (leftToRight) children else reverseChildren
            return sequenceOf(this) + children.flatMap { it.withDescendants(leftToRight) }
        }

        val prevElements: Sequence<TreeElement>
            get() = prevSiblings.flatMap { it.withDescendants(leftToRight = false) }

        val nextElements: Sequence<TreeElement>
            get() = nextSiblings.flatMap { it.withDescendants(leftToRight = true) }

//        var debugText: String? = null
    }

    private class StandardTreeElement() : TreeElement()
    private class TokenTreeElement(val tokenType: KtToken) : TreeElement()
    private class LineBreakTreeElement() : TreeElement()

    private class CommentTreeElement(
        val commentText: String,
        val spaceBefore: String,
        val spaceAfter: String
    ) : TreeElement() {
        companion object {
            fun create(comment: PsiComment): CommentTreeElement {
                val spaceBefore = (comment.prevLeaf(skipEmptyElements = true) as? PsiWhiteSpace)?.text ?: ""
                val spaceAfter = (comment.nextLeaf(skipEmptyElements = true) as? PsiWhiteSpace)?.text ?: ""
                return CommentTreeElement(comment.text, spaceBefore, spaceAfter)
            }
        }
    }

    private val commentsToRestore = ArrayList<CommentTreeElement>()
    private val lineBreaksToRestore = ArrayList<LineBreakTreeElement>()
    private var toNewPsiElementMap by Delegates.notNull<MutableMap<TreeElement, MutableCollection<PsiElement>>>()
    private var needAdjustIndentAfterRestore = false

    init {
        if (saveLineBreaks || originalElements.any { it.anyDescendantOfType<PsiComment>() }) {
            originalElements.save(null)
        }
    }

    private fun PsiChildRange.save(parentTreeElement: TreeElement?) {
        var first: TreeElement? = null
        var last: TreeElement? = null
        for (child in this) {
            assert(child.savedTreeElement == null)

            val savedChild = TreeElement.create(child) ?: continue
            savedChild.parent = parentTreeElement
            savedChild.prev = last
            if (child !is PsiWhiteSpace) { // we don't try to anchor comments to whitespaces
                child.savedTreeElement = savedChild
            }
            last?.next = savedChild
            last = savedChild

            if (first == null) {
                first = savedChild
            }

            when (savedChild) {
                is CommentTreeElement -> commentsToRestore.add(savedChild)
                is LineBreakTreeElement -> if (saveLineBreaks) lineBreaksToRestore.add(savedChild)
            }

            child.allChildren.save(savedChild)
        }

        parentTreeElement?.firstChild = first
        parentTreeElement?.lastChild = last
    }

    private var PsiElement.savedTreeElement: TreeElement?
        get() = getCopyableUserData(SAVED_TREE_KEY)
        set(value) = putCopyableUserData(SAVED_TREE_KEY, value)

    var isFinished = false
        private set

    fun deleteCommentsInside(element: PsiElement) {
        assert(!isFinished)

        element.accept(object : PsiRecursiveElementVisitor() {
            override fun visitComment(comment: PsiComment) {
                val treeElement = comment.savedTreeElement
                if (treeElement != null) {
                    commentsToRestore.remove(treeElement)
                }
            }
        })
    }

    fun elementCreatedByText(createdElement: PsiElement, original: PsiElement, rangeInOriginal: TextRange) {
        assert(!isFinished)
        assert(createdElement.textLength == rangeInOriginal.length)
        assert(createdElement.text == original.text.substring(rangeInOriginal.startOffset, rangeInOriginal.endOffset))

        createdElement.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PsiWhiteSpace) return

                val token = original.findElementAt(element.getStartOffsetIn(createdElement) + rangeInOriginal.startOffset)
                if (token != null) {
                    val elementLength = element.textLength
                    for (originalElement in token.parentsWithSelf) {
                        val length = originalElement.textLength
                        if (length < elementLength) continue
                        if (length == elementLength) {
                            element.savedTreeElement = originalElement.savedTreeElement
                        }
                        break
                    }
                }

                super.visitElement(element)
            }
        })
    }

    private fun putNewElementIntoMap(psiElement: PsiElement, treeElement: TreeElement) {
        toNewPsiElementMap.getOrPut(treeElement) { ArrayList(1) }.add(psiElement)
    }

    private fun bindNewElement(newPsiElement: PsiElement, treeElement: TreeElement) {
        newPsiElement.savedTreeElement = treeElement
        putNewElementIntoMap(newPsiElement, treeElement)
    }

    fun restore(
        resultElement: PsiElement,
        isCommentBeneathSingleLine: Boolean,
        isCommentInside: Boolean,
        forceAdjustIndent: Boolean
    ) {
        restore(PsiChildRange.singleElement(resultElement), forceAdjustIndent, isCommentBeneathSingleLine, isCommentInside)
    }

    fun restore(resultElement: PsiElement, forceAdjustIndent: Boolean = false) {
        restore(PsiChildRange.singleElement(resultElement), forceAdjustIndent)
    }

    fun restore(
        resultElements: PsiChildRange,
        forceAdjustIndent: Boolean = false,
        isCommentBeneathSingleLine: Boolean = false,
        isCommentInside: Boolean = false
    ) {
        assert(!isFinished)
        assert(!resultElements.isEmpty)

        if (commentsToRestore.isNotEmpty() || lineBreaksToRestore.isNotEmpty()) {
            // remove comments that present inside resultElements from commentsToRestore
            resultElements.forEach { deleteCommentsInside(it) }

            if (commentsToRestore.isNotEmpty() || lineBreaksToRestore.isNotEmpty()) {
                toNewPsiElementMap = HashMap<TreeElement, MutableCollection<PsiElement>>()
                for (element in resultElements) {
                    element.accept(object : PsiRecursiveElementVisitor() {
                        override fun visitElement(element: PsiElement) {
                            val treeElement = element.savedTreeElement
                            if (treeElement != null) {
                                putNewElementIntoMap(element, treeElement)
                            }
                            super.visitElement(element)
                        }
                    })
                }

                restoreComments(resultElements, isCommentBeneathSingleLine, isCommentInside)

                restoreLineBreaks()

                // clear user data
                resultElements.forEach {
                    it.accept(object : PsiRecursiveElementVisitor() {
                        override fun visitElement(element: PsiElement) {
                            element.savedTreeElement = null
                            super.visitElement(element)
                        }
                    })
                }
            }
        }

        if (needAdjustIndentAfterRestore || forceAdjustIndent) {
            val file = resultElements.first().containingFile
            val project = file.project
            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            val document = psiDocumentManager.getDocument(file)
            if (document != null) {
                psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)
                psiDocumentManager.commitDocument(document)
            }
            CodeStyleManager.getInstance(project).adjustLineIndent(file, resultElements.textRange)
        }

        isFinished = true
    }

    private fun restoreComments(
        resultElements: PsiChildRange,
        isCommentBeneathSingleLine: Boolean = false,
        isCommentInside: Boolean = false
    ) {
        var putAbandonedCommentsAfter = resultElements.last!!

        for (commentTreeElement in commentsToRestore) {
            val comment = psiFactory.createComment(commentTreeElement.commentText)

            val anchorBefore = findAnchor(commentTreeElement, before = true)
            val anchorAfter = findAnchor(commentTreeElement, before = false)
            val anchor = chooseAnchor(anchorBefore, anchorAfter)

            val restored: PsiComment
            if (anchor != null) {
                val anchorElement = findFinalAnchorElement(anchor, comment)
                val parent = anchorElement.parent
                if (anchor.before) {
                    restored = parent.addAfter(comment, anchorElement) as PsiComment
                    if (commentTreeElement.spaceBefore.isNotEmpty()) {
                        parent.addAfter(psiFactory.createWhiteSpace(commentTreeElement.spaceBefore), anchorElement)
                    }

                    // make sure that there is a line break after EOL_COMMENT
                    if (restored.tokenType == KtTokens.EOL_COMMENT) {
                        val whiteSpace = restored.nextLeaf(skipEmptyElements = true) as? PsiWhiteSpace
                        if (whiteSpace == null) {
                            parent.addAfter(psiFactory.createWhiteSpace("\n"), restored)
                        } else if (!whiteSpace.textContains('\n')) {
                            val newWhiteSpace = psiFactory.createWhiteSpace("\n" + whiteSpace.text)
                            whiteSpace.replace(newWhiteSpace)
                        }
                    }
                } else {
                    restored = parent.addBefore(comment, anchorElement) as PsiComment
                    if (commentTreeElement.spaceAfter.isNotEmpty()) {
                        parent.addBefore(psiFactory.createWhiteSpace(commentTreeElement.spaceAfter), anchorElement)
                    }
                }
            } else {
                restored = putAbandonedCommentsAfter.parent.addBefore(comment, putAbandonedCommentsAfter) as PsiComment

                if (isCommentInside) {
                    val element = resultElements.first
                    val innerExpression = element?.lastChild?.getPrevSiblingIgnoringWhitespace()
                    innerExpression?.add(psiFactory.createWhiteSpace())
                    innerExpression?.add(restored)
                    restored.delete()
                }
                putAbandonedCommentsAfter = restored
            }

            // shift (possible contained) comment in expression underneath braces
            if (isCommentBeneathSingleLine && resultElements.count() == 1) {
                val element = resultElements.first
                element?.add(psiFactory.createWhiteSpace("\n"))
                element?.add(restored)
                restored.delete()
            }

            bindNewElement(restored, commentTreeElement) // will be used when restoring line breaks

            if (restored.tokenType == KtTokens.EOL_COMMENT) {
                needAdjustIndentAfterRestore = true // TODO: do we really need it?
            }
        }
    }

    private fun restoreLineBreaks() {
        for (lineBreakElement in lineBreaksToRestore) {
            if (toNewPsiElementMap[lineBreakElement] != null) continue

            val lineBreakParent = lineBreakElement.parent

            fun findRestored(leaf: TreeElement): PsiElement? {
                if (leaf is LineBreakTreeElement) return null
                return leaf.parentsWithSelf
                    .takeWhile { it != lineBreakParent }
                    .mapNotNull { toNewPsiElementMap[it]?.first() } //TODO: what about multiple?
                    .firstOrNull()
            }


            val tokensToMatch = arrayListOf<KtToken>()
            for (leaf in lineBreakElement.prevLeafs) {
                var psiElement = findRestored(leaf)
                if (psiElement != null) {
                    psiElement = skipTokensForward(psiElement, tokensToMatch.asReversed())
                    psiElement?.restoreLineBreakAfter()
                    break
                } else {
                    if (leaf !is TokenTreeElement) break
                    tokensToMatch.add(leaf.tokenType)
                }
            }
        }
    }

    private fun skipTokensForward(psiElement: PsiElement, tokensToMatch: List<KtToken>): PsiElement? {
        var currentPsiElement = psiElement
        for (token in tokensToMatch) {
            currentPsiElement = currentPsiElement.nextLeaf(nonSpaceAndNonEmptyFilter) ?: return null
            if (currentPsiElement.tokenType != token) return null
        }
        return currentPsiElement
    }

    private fun PsiElement.restoreLineBreakAfter() {
        val addAfter = shiftNewLineAnchor(this).anchorToAddCommentOrSpace(before = false)
        var whitespace = addAfter.nextSibling as? PsiWhiteSpace
        //TODO: restore blank lines
        if (whitespace != null && whitespace.text.contains('\n')) return // line break is already there

        if (whitespace == null) {
            addAfter.parent.addAfter(psiFactory.createNewLine(), addAfter)
        } else {
            whitespace.replace(psiFactory.createWhiteSpace("\n" + whitespace.text))
        }

        needAdjustIndentAfterRestore = true
    }

    private class Anchor(val element: PsiElement, val treeElementsBetween: Collection<TreeElement>, val before: Boolean)

    private fun findAnchor(commentTreeElement: CommentTreeElement, before: Boolean): Anchor? {
        val treeElementsBetween = ArrayList<TreeElement>()
        val sequence = if (before) commentTreeElement.prevElements else commentTreeElement.nextElements
        for (treeElement in sequence) {
            val newPsiElements = toNewPsiElementMap[treeElement]
            if (newPsiElements != null) {
                val psiElement = newPsiElements.first().anchorToAddCommentOrSpace(!before) //TODO: should we restore multiple?
                return Anchor(psiElement, treeElementsBetween, before)
            }
            if (treeElement.firstChild == null) { // we put only leafs into treeElementsBetween
                treeElementsBetween.add(treeElement)
            }
        }
        return null
    }

    private fun PsiElement.anchorToAddCommentOrSpace(before: Boolean): PsiElement {
        return parentsWithSelf
            .dropWhile { it.parent !is PsiFile && (if (before) it.prevSibling else it.nextSibling) == null }
            .first()
    }

    private fun chooseAnchor(anchorBefore: Anchor?, anchorAfter: Anchor?): Anchor? {
        if (anchorBefore == null) return anchorAfter
        if (anchorAfter == null) return anchorBefore

        val elementsBefore = anchorBefore.treeElementsBetween
        val elementsAfter = anchorAfter.treeElementsBetween

        val lineBreakBefore = elementsBefore.any { it is LineBreakTreeElement }
        val lineBreakAfter = elementsAfter.any { it is LineBreakTreeElement }
        if (lineBreakBefore && !lineBreakAfter) return anchorAfter

        if (elementsBefore.isNotEmpty() && elementsAfter.isEmpty()) return anchorAfter

        return anchorBefore //TODO: more analysis?
    }

    private fun findFinalAnchorElement(anchor: Anchor, comment: PsiComment): PsiElement {
        val tokensBetween = anchor.treeElementsBetween.filterIsInstance<TokenTreeElement>()

        fun PsiElement.next(): PsiElement? {
            return if (anchor.before) nextLeaf(nonSpaceAndNonEmptyFilter) else prevLeaf(nonSpaceAndNonEmptyFilter)
        }

        var psiElement = anchor.element
        for (token in tokensBetween.asReversed()) {
            val next = psiElement.next() ?: break
            if (next.tokenType != token.tokenType) break
            psiElement = next
        }

        // don't put end of line comment right before comma
        if (anchor.before && comment.tokenType == KtTokens.EOL_COMMENT) {
            psiElement = shiftNewLineAnchor(psiElement)
        }

        return psiElement
    }

    // don't put line break right before comma
    private fun shiftNewLineAnchor(putAfter: PsiElement): PsiElement {
        val next = putAfter.nextLeaf(nonSpaceAndNonEmptyFilter)
        return if (next?.tokenType == KtTokens.COMMA) next!! else putAfter
    }

    private val nonSpaceAndNonEmptyFilter = { element: PsiElement -> element !is PsiWhiteSpace && element.textLength > 0 }

    companion object {
        //TODO: making it private causes error on runtime (KT-7874?)
        val PsiElement.tokenType: KtToken?
            get() = node.elementType as? KtToken
    }
}