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
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.*
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap

public class CommentSaver(originalElements: PsiChildRange) {
    public constructor(originalElement: PsiElement) : this(PsiChildRange.singleElement(originalElement))

    private val SAVED_TREE_KEY = Key<TreeElement>("SAVED_TREE")

    private abstract class TreeElement {
        public companion object {
            public fun create(element: PsiElement): TreeElement? {
                val elementType = element.getNode().getElementType()
                val treeElement = when (elementType) {
                    TokenType.WHITE_SPACE -> if (element.textContains('\n')) LineBreakTreeElement() else null
                    is JetToken -> if (element is PsiComment) CommentTreeElement.create(element) else TokenTreeElement(elementType)
                    else -> StandardTreeElement()
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

    init {
        if (originalElements.any { it.anyDescendantOfType<PsiComment>() }) {
            originalElements.save(null)

            for (element in originalElements) {
                element.accept(object : PsiRecursiveElementVisitor(){
                    override fun visitComment(comment: PsiComment) {
                        commentsToRestore.add(comment.savedTreeElement as CommentTreeElement)
                    }
                })
            }
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

    public fun restoreComments(resultElement: PsiElement) {
        restoreComments(PsiChildRange.singleElement(resultElement))
    }

    public fun restoreComments(resultElements: PsiChildRange) {
        doRestoreComments(resultElements)

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

    private fun doRestoreComments(resultElements: PsiChildRange) {
        assert(!resultElements.isEmpty)

        if (commentsToRestore.isEmpty()) return

        // remove comments that present inside resultElements from commentsToRestore
        resultElements.forEach { deleteCommentsInside(it) }
        if (commentsToRestore.isEmpty()) return

        val toNewPsiElementMap = HashMap<TreeElement, MutableCollection<PsiElement>>()
        for (element in resultElements) {
            element.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val treeElement = element.savedTreeElement
                    if (treeElement != null) {
                        toNewPsiElementMap.getOrPut(treeElement) { ArrayList(1) }.add(element)
                    }
                    super.visitElement(element)
                }
            })
        }

        val psiFactory = JetPsiFactory(resultElements.first!!)
        var putAbandonedCommentsAfter = resultElements.last!!

        for (commentTreeElement in commentsToRestore) {
            val comment = psiFactory.createComment(commentTreeElement.commentText)

            val anchorBefore = findAnchor(commentTreeElement, toNewPsiElementMap, before = true)
            val anchorAfter = findAnchor(commentTreeElement, toNewPsiElementMap, before = false)
            val anchor = chooseAnchor(anchorBefore, anchorAfter)

            if (anchor != null) {
                val anchorElement = findFinalAnchorElement(anchor)
                val parent = anchorElement.getParent()
                if (anchor.before) {
                    parent.addAfter(comment, anchorElement)
                    if (commentTreeElement.spaceBefore.isNotEmpty()) {
                        parent.addAfter(psiFactory.createWhiteSpace(commentTreeElement.spaceBefore), anchorElement)
                    }
                }
                else {
                    parent.addBefore(comment, anchorElement)
                    if (commentTreeElement.spaceAfter.isNotEmpty()) {
                        parent.addBefore(psiFactory.createWhiteSpace(commentTreeElement.spaceAfter), anchorElement)
                    }
                }
            }
            else {
                putAbandonedCommentsAfter = putAbandonedCommentsAfter.getParent().addAfter(comment, putAbandonedCommentsAfter)
            }
        }
    }

    private class Anchor(val element: PsiElement, val treeElementsBetween: Collection<TreeElement>, val before: Boolean)

    private fun findAnchor(commentTreeElement: CommentTreeElement, toNewPsiElementMap: Map<TreeElement, Collection<PsiElement>>, before: Boolean): Anchor? {
        val treeElementsBetween = ArrayList<TreeElement>()
        val sequence = if (before) commentTreeElement.prevElements else commentTreeElement.nextElements
        for (treeElement in sequence) {
            val newPsiElements = toNewPsiElementMap[treeElement]
            if (newPsiElements != null) {
                var psiElement = newPsiElements.first() //TODO: should we restore multiple?
                psiElement = psiElement.parents()
                        .dropWhile { it.getParent() !is PsiFile && (if (before) it.getNextSibling() else it.getPrevSibling()) == null }
                        .first()
                return Anchor(psiElement, treeElementsBetween, before)
            }
            if (treeElement.firstChild == null) { // we put only leafs into treeElementsBetween
                treeElementsBetween.add(treeElement)
            }
        }
        return null
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

    private fun findFinalAnchorElement(anchor: Anchor): PsiElement {
        val tokensBetween = anchor.treeElementsBetween.filterIsInstance<TokenTreeElement>()
        var psiElement = anchor.element
        if (tokensBetween.isEmpty()) return psiElement

        fun PsiElement.next(): PsiElement? {
            val filter = { element: PsiElement -> element !is PsiWhiteSpace && element.getTextLength() > 0 }
            return if (anchor.before) nextLeaf(filter) else prevLeaf(filter)
        }

        for (token in tokensBetween.reverse()) {
            val next = psiElement.next() ?: break
            if (next.getNode().getElementType() != token.tokenType) break
            psiElement = next
        }
        return psiElement
    }
}