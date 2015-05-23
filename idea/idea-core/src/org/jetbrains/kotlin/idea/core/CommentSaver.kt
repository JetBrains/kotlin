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
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.*
import java.util.*

public class CommentSaver(originalElements: PsiChildRange) {
    public constructor(originalElement: PsiElement) : this(PsiChildRange.singleElement(originalElement))

    private val SAVED_TREE_KEY = Key<TreeElement>("SAVED_TREE")

    private class TreeElement(
            var parent: TreeElement? = null,
            var prev: TreeElement? = null,
            var next: TreeElement? = null,
            var firstChild: TreeElement? = null,
            var lastChild: TreeElement? = null

    ) {
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
    }

    private data class CommentData(
            val commentText: String,
            val spaceBefore: String,
            val spaceAfter: String
    )

    private fun PsiComment.buildData(): CommentData {
        val spaceBefore = (prevLeaf(skipEmptyElements = true) as? PsiWhiteSpace)?.getText() ?: ""
        val spaceAfter = (nextLeaf(skipEmptyElements = true) as? PsiWhiteSpace)?.getText() ?: ""
        return CommentData(getText(), spaceBefore, spaceAfter)
    }

    private val commentsToRestore = LinkedHashMap<TreeElement, CommentData>()

    init {
        if (originalElements.any { it.anyDescendantOfTypeTemp<PsiComment>() }) {
            originalElements.save(null)

            for (element in originalElements) {
                element.accept(object : PsiRecursiveElementVisitor(){
                    override fun visitComment(comment: PsiComment) {
                        commentsToRestore.put(comment.savedTreeElement!!, comment.buildData())
                    }
                })
            }
        }
    }

    private fun PsiChildRange.save(parentTreeElement: TreeElement?) {
        var first: TreeElement? = null
        var last: TreeElement? = null
        for (child in this) {
            if (!child.shouldSave) continue
            assert(child.savedTreeElement == null)

            val savedChild = TreeElement(parent = parentTreeElement, prev = last)
            child.putCopyableUserData(SAVED_TREE_KEY, savedChild)
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

    private val PsiElement.shouldSave: Boolean
        get() = this !is PsiWhiteSpace

    private val PsiElement.savedTreeElement: TreeElement?
        get() = getCopyableUserData(SAVED_TREE_KEY)

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

    public fun restoreComments(resultElement: PsiElement) {
        restoreComments(PsiChildRange.singleElement(resultElement))
    }

    public fun restoreComments(resultElements: PsiChildRange) {
        doRestoreComments(resultElements)

        // clear user data
        resultElements.forEach {
            it.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    element.putCopyableUserData(SAVED_TREE_KEY, null)
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
        for ((treeElement, commentData) in commentsToRestore) {
            val comment = psiFactory.createComment(commentData.commentText)

            //TODO: should we restore multiple?
            val putAfter: PsiElement? = treeElement.prevElements.map { toNewPsiElementMap[it]?.first() }.filterNotNull().firstOrNull()
            if (putAfter != null) {
                val parent = putAfter.getParent()
                parent.addAfter(comment, putAfter)
                if (commentData.spaceBefore.isNotEmpty()) {
                    parent.addAfter(psiFactory.createWhiteSpace(commentData.spaceBefore), putAfter)
                }
                continue
            }

            val putBefore: PsiElement? = treeElement.nextElements.map { toNewPsiElementMap[it]?.first() }.filterNotNull().firstOrNull()
            if (putBefore != null) {
                val parent = putBefore.getParent()
                parent.addBefore(comment, putBefore)
                if (commentData.spaceAfter.isNotEmpty()) {
                    parent.addBefore(psiFactory.createWhiteSpace(commentData.spaceAfter), putBefore)
                }
                continue
            }

            // TODO: everything deleted, don't restore anything?
        }
    }

    //TODO
    private inline fun <reified T : PsiElement> PsiElement.anyDescendantOfTypeTemp(): Boolean {
        var result = false
        this.accept(object : PsiRecursiveElementVisitor(){
            override fun visitElement(element: PsiElement) {
                if (result) return
                if (element is T) {
                    result = true
                    return
                }
                super.visitElement(element)
            }
        })
        return result
    }
}