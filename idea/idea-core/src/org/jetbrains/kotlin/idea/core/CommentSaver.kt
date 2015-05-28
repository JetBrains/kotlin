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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.*
import java.util.ArrayList
import java.util.HashMap
import kotlin.properties.Delegates

public class CommentSaver(originalElements: PsiChildRange, private val saveLineBreaks: Boolean = false/*TODO?*/) {
    public constructor(originalElement: PsiElement, saveLineBreaks: Boolean = false/*TODO?*/) : this(PsiChildRange.singleElement(originalElement), saveLineBreaks)

    private val SAVED_TREE_KEY = Key<TreeElement>("SAVED_TREE")
    private val psiFactory = JetPsiFactory(originalElements.first!!)

    private abstract class TreeElement {
        public companion object {
            public fun create(element: PsiElement): TreeElement? {
                val tokenType = element.tokenType
                val treeElement = when {
                    element is PsiWhiteSpace -> if (element.textContains('\n')) LineBreakTreeElement() else null
                    element is PsiComment -> CommentTreeElement.create(element)
                    tokenType != null -> TokenTreeElement(tokenType)
                    else -> if (element.getTextLength() > 0) StandardTreeElement() else null // don't save empty elements
                }
//                treeElement?.debugText = element.getText()
                return treeElement
            }
        }

        var parent: TreeElement? = null
        var prev: TreeElement? = null
        var next: TreeElement? = null
        var firstChild: TreeElement? = null
        var lastChild: TreeElement? = null

        val children: Sequence<TreeElement>
            get() = sequence({ firstChild }, { it.next })

        val reverseChildren: Sequence<TreeElement>
            get() = sequence({ lastChild }, { it.prev })

        val prevSiblings: Sequence<TreeElement>
            get() = sequence({ prev }, { it.prev })

        val nextSiblings: Sequence<TreeElement>
            get() = sequence({ next }, { it.next })

        val parents: Sequence<TreeElement>
            get() = sequence({ parent }, { it.parent })

        val parentsWithSelf: Sequence<TreeElement>
            get() = sequence(this, { it.parent })

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
            get() = sequence({ prevLeaf }, { it.prevLeaf })

        val nextLeafs: Sequence<TreeElement>
            get() = sequence({ nextLeaf }, { it.nextLeaf })

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
    private class TokenTreeElement(val tokenType: JetToken) : TreeElement()
    private class LineBreakTreeElement() : TreeElement()

    private class CommentTreeElement(
            val commentText: String,
            val spaceBefore: String,
            val spaceAfter: String
    ) : TreeElement() {
        public companion object {
            fun create(comment: PsiComment): CommentTreeElement {
                val spaceBefore = (comment.prevLeaf(skipEmptyElements = true) as? PsiWhiteSpace)?.getText() ?: ""
                val spaceAfter = (comment.nextLeaf(skipEmptyElements = true) as? PsiWhiteSpace)?.getText() ?: ""
                return CommentTreeElement(comment.getText(), spaceBefore, spaceAfter)
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

    public fun deleteCommentsInside(element: PsiElement) {
        element.accept(object : PsiRecursiveElementVisitor() {
            override fun visitComment(comment: PsiComment) {
                val treeElement = comment.savedTreeElement
                if (treeElement != null) {
                    commentsToRestore.remove(treeElement)
                }
            }
        })
    }

    public fun elementCreatedByText(createdElement: PsiElement, original: PsiElement, rangeInOriginal: TextRange) {
        assert(createdElement.getTextLength() == rangeInOriginal.getLength())
        assert(createdElement.getText() == original.getText().substring(rangeInOriginal.getStartOffset(), rangeInOriginal.getEndOffset()))

        createdElement.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PsiWhiteSpace) return

                val token = original.findElementAt(element.getStartOffsetIn(createdElement) + rangeInOriginal.getStartOffset())
                if (token != null) {
                    val elementLength = element.getTextLength()
                    for (originalElement in token.parents()) {
                        val length = originalElement.getTextLength()
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

    public fun restore(resultElement: PsiElement) {
        restore(PsiChildRange.singleElement(resultElement))
    }

    public fun restore(resultElements: PsiChildRange) {
        assert(!resultElements.isEmpty)

        if (commentsToRestore.isEmpty() && lineBreaksToRestore.isEmpty()) return

        // remove comments that present inside resultElements from commentsToRestore
        resultElements.forEach { deleteCommentsInside(it) }
        if (commentsToRestore.isEmpty() && lineBreaksToRestore.isEmpty()) return

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

        restoreComments(resultElements)

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

        if (needAdjustIndentAfterRestore) {
            val file = resultElements.first().getContainingFile()
            val project = file.getProject()
            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(psiDocumentManager.getDocument(file)!!)
            CodeStyleManager.getInstance(project).adjustLineIndent(file, resultElements.textRange)
        }
    }

    private fun restoreComments(resultElements: PsiChildRange) {
        var putAbandonedCommentsAfter = resultElements.last!!

        for (commentTreeElement in commentsToRestore) {
            val comment = psiFactory.createComment(commentTreeElement.commentText)

            val anchorBefore = findAnchor(commentTreeElement, before = true)
            val anchorAfter = findAnchor(commentTreeElement, before = false)
            val anchor = chooseAnchor(anchorBefore, anchorAfter)

            val restored: PsiComment
            if (anchor != null) {
                val anchorElement = findFinalAnchorElement(anchor, comment)
                val parent = anchorElement.getParent()
                if (anchor.before) {
                    restored = parent.addAfter(comment, anchorElement) as PsiComment
                    if (commentTreeElement.spaceBefore.isNotEmpty()) {
                        parent.addAfter(psiFactory.createWhiteSpace(commentTreeElement.spaceBefore), anchorElement)
                    }
                }
                else {
                    restored = parent.addBefore(comment, anchorElement) as PsiComment
                    if (commentTreeElement.spaceAfter.isNotEmpty()) {
                        parent.addBefore(psiFactory.createWhiteSpace(commentTreeElement.spaceAfter), anchorElement)
                    }
                }
            }
            else {
                restored = putAbandonedCommentsAfter.getParent().addAfter(comment, putAbandonedCommentsAfter) as PsiComment
                putAbandonedCommentsAfter = restored
            }

            bindNewElement(restored, commentTreeElement) // will be used when restoring line breaks

            if (restored.tokenType == JetTokens.EOL_COMMENT) {
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
                        .map { toNewPsiElementMap[it]?.first() } //TODO: what about multiple?
                        .filterNotNull()
                        .firstOrNull()
            }


            val tokensToMatch = arrayListOf<JetToken>()
            for (leaf in lineBreakElement.prevLeafs) {
                var psiElement = findRestored(leaf)
                if (psiElement != null) {
                    psiElement = skipTokensForward(psiElement, tokensToMatch.reverse())
                    psiElement?.restoreLineBreakAfter()
                    break
                }
                else {
                    if (leaf !is TokenTreeElement) break
                    tokensToMatch.add(leaf.tokenType)
                }
            }
        }
    }

    private fun skipTokensForward(psiElement: PsiElement, tokensToMatch: List<JetToken>): PsiElement? {
        var currentPsiElement = psiElement
        for (token in tokensToMatch) {
            currentPsiElement = currentPsiElement.nextLeaf(nonSpaceAndNonEmptyFilter) ?: return null
            if (currentPsiElement.tokenType != token) return null
        }
        return currentPsiElement
    }

    private fun PsiElement.restoreLineBreakAfter() {
        val addAfter = shiftNewLineAnchor(this).anchorToAddCommentOrSpace(before = false)
        var whitespace = addAfter.getNextSibling() as? PsiWhiteSpace
        //TODO: restore blank lines
        if (whitespace != null && whitespace.getText().contains('\n')) return // line break is already there

        if (whitespace == null) {
            addAfter.getParent().addAfter(psiFactory.createNewLine(), addAfter)
        }
        else {
            whitespace.replace(psiFactory.createWhiteSpace("\n" + whitespace.getText()))
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
        return parents()
                .dropWhile { it.getParent() !is PsiFile && (if (before) it.getPrevSibling() else it.getNextSibling()) == null }
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
        for (token in tokensBetween.reverse()) {
            val next = psiElement.next() ?: break
            if (next.tokenType != token.tokenType) break
            psiElement = next
        }

        // don't put end of line comment right before comma
        if (anchor.before && comment.tokenType == JetTokens.EOL_COMMENT) {
            psiElement = shiftNewLineAnchor(psiElement)
        }

        return psiElement
    }

    // don't put line break right before comma
    private fun shiftNewLineAnchor(putAfter: PsiElement): PsiElement {
        val next = putAfter.nextLeaf(nonSpaceAndNonEmptyFilter)
        return if (next?.tokenType == JetTokens.COMMA) next!! else putAfter
    }

    private val nonSpaceAndNonEmptyFilter = { element: PsiElement -> element !is PsiWhiteSpace && element.getTextLength() > 0 }

    companion object {
        //TODO: making it private causes error on runtime
        val PsiElement.tokenType: JetToken?
            get() = getNode().getElementType() as? JetToken
    }
}