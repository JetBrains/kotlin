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

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import java.util.*

// NOTE: in this file we collect only LANGUAGE INDEPENDENT methods working with PSI and not modifying it

// ----------- Walking children/siblings/parents -------------------------------------------------------------------------------------------

public val PsiElement.allChildren: PsiChildRange
    get() {
        val first = getFirstChild()
        return if (first != null) PsiChildRange(first, getLastChild()) else PsiChildRange.EMPTY
    }

public fun PsiElement.siblings(forward: Boolean = true, withItself: Boolean = true): Sequence<PsiElement> {
    return object : Sequence<PsiElement> {
        override fun iterator(): Iterator<PsiElement> {
            var next: PsiElement? = this@siblings
            return object : Iterator<PsiElement> {
                init {
                    if (!withItself) next()
                }

                override fun hasNext(): Boolean = next != null
                override fun next(): PsiElement {
                    val result = next ?: throw NoSuchElementException()
                    if (forward)
                        next = result.nextSibling
                    else
                        next = result.prevSibling
                    return result
                }
            }
        }
    }
}

public val PsiElement.parentsWithSelf: Sequence<PsiElement>
    get() = sequence(this) { if (it is PsiFile) null else it.getParent() }

public val PsiElement.parents: Sequence<PsiElement>
    get() = parentsWithSelf.drop(1)

public fun PsiElement.prevLeaf(skipEmptyElements: Boolean = false): PsiElement?
        = PsiTreeUtil.prevLeaf(this, skipEmptyElements)

public fun PsiElement.nextLeaf(skipEmptyElements: Boolean = false): PsiElement?
        = PsiTreeUtil.nextLeaf(this, skipEmptyElements)

public fun PsiElement.prevLeaf(filter: (PsiElement) -> Boolean): PsiElement? {
    var leaf = prevLeaf()
    while (leaf != null && !filter(leaf)) {
        leaf = leaf.prevLeaf()
    }
    return leaf
}

public fun PsiElement.nextLeaf(filter: (PsiElement) -> Boolean): PsiElement? {
    var leaf = nextLeaf()
    while (leaf != null && !filter(leaf)) {
        leaf = leaf.nextLeaf()
    }
    return leaf
}

public fun PsiElement.getParentOfTypesAndPredicate<T : PsiElement>(
        strict: Boolean = false, vararg parentClasses: Class<T>, predicate: (T) -> Boolean
): T? {
    var element = if (strict) getParent() else this
    while (element != null) {
        @Suppress("UNCHECKED_CAST")
        when {
            (parentClasses.isEmpty() || parentClasses.any { parentClass -> parentClass.isInstance(element) }) && predicate(element!! as T) ->
                return element as T
            element is PsiFile ->
                return null
            else ->
                element = element!!.getParent()
        }
    }

    return null
}

public fun PsiElement.getNonStrictParentOfType<T : PsiElement>(parentClass: Class<T>): T? {
    return PsiTreeUtil.getParentOfType(this, parentClass, false)
}

inline public fun PsiElement.getParentOfType<reified T : PsiElement>(strict: Boolean): T? {
    return PsiTreeUtil.getParentOfType(this, javaClass<T>(), strict)
}

inline public fun PsiElement.getStrictParentOfType<reified T : PsiElement>(): T? {
    return PsiTreeUtil.getParentOfType(this, javaClass<T>(), true)
}

inline public fun PsiElement.getNonStrictParentOfType<reified T : PsiElement>(): T? {
    return PsiTreeUtil.getParentOfType(this, javaClass<T>(), false)
}

inline public fun PsiElement.getChildOfType<reified T : PsiElement>(): T? {
    return PsiTreeUtil.getChildOfType(this, javaClass<T>())
}

inline public fun PsiElement.getChildrenOfType<reified T : PsiElement>(): Array<T> {
    return PsiTreeUtil.getChildrenOfType(this, javaClass<T>()) ?: arrayOf()
}

public fun PsiElement.getNextSiblingIgnoringWhitespaceAndComments(): PsiElement? {
    return siblings(withItself = false).filter { it !is PsiWhiteSpace && it !is PsiComment }.firstOrNull()
}

public fun PsiElement?.isAncestor(element: PsiElement, strict: Boolean = false): Boolean {
    return PsiTreeUtil.isAncestor(this, element, strict)
}

public fun <T : PsiElement> T.getIfChildIsInBranch(element: PsiElement, branch: T.() -> PsiElement?): T? {
    return if (branch().isAncestor(element)) this else null
}

public inline fun PsiElement.getParentOfTypeAndBranch<reified T : PsiElement>(strict: Boolean = false, noinline branch: T.() -> PsiElement?): T? {
    return getParentOfType<T>(strict)?.getIfChildIsInBranch(this, branch)
}

public tailrec fun PsiElement.getOutermostParentContainedIn(container: PsiElement): PsiElement? {
    val parent = getParent()
    return if (parent == container) this else parent?.getOutermostParentContainedIn(container)
}

public fun PsiElement.isInsideOf(elements: Iterable<PsiElement>): Boolean = elements.any { it.isAncestor(this) }

// -------------------- Recursive tree visiting --------------------------------------------------------------------------------------------

public inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(noinline action: (T) -> Unit) {
    forEachDescendantOfType<T>({ true }, action)
}

public inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(crossinline canGoInside: (PsiElement) -> Boolean, noinline action: (T) -> Unit) {
    this.accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (canGoInside(element)) {
                super.visitElement(element)
            }

            if (element is T) {
                action(element)
            }
        }
    })
}

public inline fun <reified T : PsiElement> PsiElement.anyDescendantOfType(noinline predicate: (T) -> Boolean = { true }): Boolean {
    return findDescendantOfType<T>(predicate) != null
}

public inline fun <reified T : PsiElement> PsiElement.anyDescendantOfType(crossinline canGoInside: (PsiElement) -> Boolean, noinline predicate: (T) -> Boolean = { true }): Boolean {
    return findDescendantOfType<T>(canGoInside, predicate) != null
}

public inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(noinline predicate: (T) -> Boolean = { true }): T? {
    return findDescendantOfType<T>({ true }, predicate)
}

public inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(crossinline canGoInside: (PsiElement) -> Boolean, noinline predicate: (T) -> Boolean = { true }): T? {
    var result: T? = null
    this.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is T && predicate(element)) {
                result = element
                stopWalking()
                return
            }

            if (canGoInside(element)) {
                super.visitElement(element)
            }
        }
    })
    return result
}

public inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(noinline predicate: (T) -> Boolean = { true }): List<T> {
    return collectDescendantsOfType<T>({ true }, predicate)
}

public inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(crossinline canGoInside: (PsiElement) -> Boolean, noinline predicate: (T) -> Boolean = { true }): List<T> {
    val result = ArrayList<T>()
    forEachDescendantOfType<T>(canGoInside) {
        if (predicate(it)) {
            result.add(it)
        }
    }
    return result
}

// ----------- Working with offsets, ranges and texts ----------------------------------------------------------------------------------------------

public val PsiElement.startOffset: Int
    get() = getTextRange().getStartOffset()

public val PsiElement.endOffset: Int
    get() = getTextRange().getEndOffset()

public fun PsiElement.getStartOffsetIn(ancestor: PsiElement): Int {
    var offset = 0
    var parent = this
    while (parent != ancestor) {
        offset += parent.getStartOffsetInParent()
        parent = parent.getParent()
    }
    return offset
}

public fun TextRange.containsInside(offset: Int): Boolean = getStartOffset() < offset && offset < getEndOffset()

public val PsiChildRange.textRange: TextRange?
    get() {
        if (isEmpty) return null
        return TextRange(first!!.startOffset, last!!.endOffset)
    }

public fun PsiChildRange.getText(): String {
    if (isEmpty) return ""
    return this.map { it.getText() }.joinToString("")
}

public fun PsiFile.elementsInRange(range: TextRange): List<PsiElement> {
    var offset = range.getStartOffset()
    val result = ArrayList<PsiElement>()
    while (offset < range.getEndOffset()) {
        val currentRange = TextRange(offset, range.getEndOffset())
        val leaf = findFirstLeafWhollyInRange(this, currentRange) ?: break

        val element = leaf
                .parentsWithSelf
                .first {
                    val parent = it.getParent()
                    it is PsiFile || parent.getTextRange() !in currentRange
                }
        result.add(element)

        offset = element.endOffset
    }
    return result
}

private fun findFirstLeafWhollyInRange(file: PsiFile, range: TextRange): PsiElement? {
    var element = file.findElementAt(range.getStartOffset()) ?: return null
    var elementRange = element.getTextRange()
    if (elementRange.getStartOffset() < range.getStartOffset()) {
        element = element.nextLeaf(skipEmptyElements = true) ?: return null
        elementRange = element.getTextRange()
    }
    assert(elementRange.getStartOffset() >= range.getStartOffset())
    return if (elementRange.getEndOffset() <= range.getEndOffset()) element else null
}

// ---------------------------------- Debug/logging ----------------------------------------------------------------------------------------

public fun PsiElement.getElementTextWithContext(): String {
    assert(isValid) { "Invalid element $this" }

    if (this is PsiFile) {
        return containingFile.getText()
    }

    // Find parent for element among file children
    val topLevelElement = PsiTreeUtil.findFirstParent(this, { it.getParent() is PsiFile }) ?:
                          throw AssertionError("For non-file element we should always be able to find parent in file children")

    val startContextOffset = topLevelElement.startOffset
    val elementContextOffset = getTextRange().getStartOffset()

    val inFileParentOffset = elementContextOffset - startContextOffset


    return StringBuilder(topLevelElement.getText())
            .insert(inFileParentOffset, "<caret>")
            .insert(0, "File name: ${containingFile.getName()} Physical: ${containingFile.isPhysical}\n")
            .toString()
}

public fun PsiElement.getTextWithLocation(): String = "'${this.getText()}' at ${DiagnosticUtils.atLocation(this)}"

// -----------------------------------------------------------------------------------------------------------------------------------------

public fun SearchScope.contains(element: PsiElement): Boolean = PsiSearchScopeUtil.isInScope(this, element)

public fun <E : PsiElement> E.createSmartPointer(): SmartPsiElementPointer<E> =
        SmartPointerManager.getInstance(getProject()).createSmartPsiElementPointer(this)
