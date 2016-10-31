/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import java.util.*

// NOTE: in this file we collect only LANGUAGE INDEPENDENT methods working with PSI and not modifying it

// ----------- Walking children/siblings/parents -------------------------------------------------------------------------------------------

val PsiElement.allChildren: PsiChildRange
    get() {
        val first = firstChild
        return if (first != null) PsiChildRange(first, lastChild) else PsiChildRange.EMPTY
    }

fun PsiElement.siblings(forward: Boolean = true, withItself: Boolean = true): Sequence<PsiElement> {
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

val PsiElement.parentsWithSelf: Sequence<PsiElement>
    get() = generateSequence(this) { if (it is PsiFile) null else it.parent }

val PsiElement.parents: Sequence<PsiElement>
    get() = parentsWithSelf.drop(1)

fun PsiElement.prevLeaf(skipEmptyElements: Boolean = false): PsiElement?
        = PsiTreeUtil.prevLeaf(this, skipEmptyElements)

fun PsiElement.nextLeaf(skipEmptyElements: Boolean = false): PsiElement?
        = PsiTreeUtil.nextLeaf(this, skipEmptyElements)

val PsiElement.prevLeafs: Sequence<PsiElement>
    get() = generateSequence({ prevLeaf() }, { it.prevLeaf() })

val PsiElement.nextLeafs: Sequence<PsiElement>
    get() = generateSequence({ nextLeaf() }, { it.nextLeaf() })

fun PsiElement.prevLeaf(filter: (PsiElement) -> Boolean): PsiElement? {
    var leaf = prevLeaf()
    while (leaf != null && !filter(leaf)) {
        leaf = leaf.prevLeaf()
    }
    return leaf
}

fun PsiElement.nextLeaf(filter: (PsiElement) -> Boolean): PsiElement? {
    var leaf = nextLeaf()
    while (leaf != null && !filter(leaf)) {
        leaf = leaf.nextLeaf()
    }
    return leaf
}

fun <T : PsiElement> PsiElement.getParentOfTypesAndPredicate(
        strict: Boolean = false, vararg parentClasses: Class<T>, predicate: (T) -> Boolean
): T? {
    var element = if (strict) parent else this
    while (element != null) {
        @Suppress("UNCHECKED_CAST")
        when {
            (parentClasses.isEmpty() || parentClasses.any { parentClass -> parentClass.isInstance(element) }) && predicate(element as T) ->
                return element
            element is PsiFile ->
                return null
            else ->
                element = element.parent
        }
    }

    return null
}

fun <T : PsiElement> PsiElement.getNonStrictParentOfType(parentClass: Class<T>): T? {
    return PsiTreeUtil.getParentOfType(this, parentClass, false)
}

inline fun <reified T : PsiElement> PsiElement.getParentOfType(strict: Boolean): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, strict)
}

inline fun <reified T : PsiElement> PsiElement.getStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, true)
}

inline fun <reified T : PsiElement> PsiElement.getNonStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, false)
}

inline fun <reified T : PsiElement> PsiElement.getChildOfType(): T? {
    return PsiTreeUtil.getChildOfType(this, T::class.java)
}

inline fun <reified T : PsiElement> PsiElement.getChildrenOfType(): Array<T> {
    return PsiTreeUtil.getChildrenOfType(this, T::class.java) ?: arrayOf()
}

fun PsiElement.getNextSiblingIgnoringWhitespaceAndComments(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself).filter { it !is PsiWhiteSpace && it !is PsiComment }.firstOrNull()
}

fun PsiElement.getPrevSiblingIgnoringWhitespaceAndComments(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself, forward = false).filter { it !is PsiWhiteSpace && it !is PsiComment }.firstOrNull()
}

inline fun <reified T : PsiElement> T.nextSiblingOfSameType() = PsiTreeUtil.getNextSiblingOfType(this, T::class.java)

inline fun <reified T : PsiElement> T.prevSiblingOfSameType() = PsiTreeUtil.getPrevSiblingOfType(this, T::class.java)

fun PsiElement?.isAncestor(element: PsiElement, strict: Boolean = false): Boolean {
    return PsiTreeUtil.isAncestor(this, element, strict)
}

fun <T : PsiElement> T.getIfChildIsInBranch(element: PsiElement, branch: T.() -> PsiElement?): T? {
    return if (branch().isAncestor(element)) this else null
}

fun <T : PsiElement> T.getIfChildIsInBranches(element: PsiElement, branches: T.() -> Iterable<PsiElement?>): T? {
    return if (branches().any { it.isAncestor(element) }) this else null
}

inline fun <reified T : PsiElement> PsiElement.getParentOfTypeAndBranch(strict: Boolean = false, noinline branch: T.() -> PsiElement?): T? {
    return getParentOfType<T>(strict)?.getIfChildIsInBranch(this, branch)
}

inline fun <reified T : PsiElement> PsiElement.getParentOfTypeAndBranches(strict: Boolean = false, noinline branches: T.() -> Iterable<PsiElement?>): T? {
    return getParentOfType<T>(strict)?.getIfChildIsInBranches(this, branches)
}

tailrec fun PsiElement.getOutermostParentContainedIn(container: PsiElement): PsiElement? {
    val parent = parent
    return if (parent == container) this else parent?.getOutermostParentContainedIn(container)
}

fun PsiElement.isInsideOf(elements: Iterable<PsiElement>): Boolean = elements.any { it.isAncestor(this) }

fun PsiChildRange.trimWhiteSpaces(): PsiChildRange {
    if (first == null) return this
    return PsiChildRange(first.siblings().firstOrNull { it !is PsiWhiteSpace }, last!!.siblings(forward = false).firstOrNull { it !is PsiWhiteSpace })
}

// -------------------- Recursive tree visiting --------------------------------------------------------------------------------------------

inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(noinline action: (T) -> Unit) {
    forEachDescendantOfType({ true }, action)
}

inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(crossinline canGoInside: (PsiElement) -> Boolean, noinline action: (T) -> Unit) {
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

inline fun <reified T : PsiElement> PsiElement.anyDescendantOfType(noinline predicate: (T) -> Boolean = { true }): Boolean {
    return findDescendantOfType(predicate) != null
}

inline fun <reified T : PsiElement> PsiElement.anyDescendantOfType(crossinline canGoInside: (PsiElement) -> Boolean, noinline predicate: (T) -> Boolean = { true }): Boolean {
    return findDescendantOfType(canGoInside, predicate) != null
}

inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(noinline predicate: (T) -> Boolean = { true }): T? {
    return findDescendantOfType({ true }, predicate)
}

inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(crossinline canGoInside: (PsiElement) -> Boolean, noinline predicate: (T) -> Boolean = { true }): T? {
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

inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(noinline predicate: (T) -> Boolean = { true }): List<T> {
    return collectDescendantsOfType({ true }, predicate)
}

inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(crossinline canGoInside: (PsiElement) -> Boolean, noinline predicate: (T) -> Boolean = { true }): List<T> {
    val result = ArrayList<T>()
    forEachDescendantOfType<T>(canGoInside) {
        if (predicate(it)) {
            result.add(it)
        }
    }
    return result
}

// ----------- Working with offsets, ranges and texts ----------------------------------------------------------------------------------------------

val PsiElement.startOffset: Int
    get() = textRange.startOffset

val PsiElement.endOffset: Int
    get() = textRange.endOffset

fun PsiElement.getStartOffsetIn(ancestor: PsiElement): Int {
    var offset = 0
    var parent = this
    while (parent != ancestor) {
        offset += parent.startOffsetInParent
        parent = parent.parent
    }
    return offset
}

fun TextRange.containsInside(offset: Int): Boolean = startOffset < offset && offset < endOffset

val PsiChildRange.textRange: TextRange?
    get() {
        if (isEmpty) return null
        return TextRange(first!!.startOffset, last!!.endOffset)
    }

fun PsiChildRange.getText(): String {
    if (isEmpty) return ""
    return this.map { it.text }.joinToString("")
}

fun PsiFile.elementsInRange(range: TextRange): List<PsiElement> {
    var offset = range.startOffset
    val result = ArrayList<PsiElement>()
    while (offset < range.endOffset) {
        val currentRange = TextRange(offset, range.endOffset)
        val leaf = findFirstLeafWhollyInRange(this, currentRange) ?: break

        val element = leaf
                .parentsWithSelf
                .first {
                    val parent = it.parent
                    it is PsiFile || parent.textRange !in currentRange
                }
        result.add(element)

        offset = element.endOffset
    }
    return result
}

private fun findFirstLeafWhollyInRange(file: PsiFile, range: TextRange): PsiElement? {
    var element = file.findElementAt(range.startOffset) ?: return null
    var elementRange = element.textRange
    if (elementRange.startOffset < range.startOffset) {
        element = element.nextLeaf(skipEmptyElements = true) ?: return null
        elementRange = element.textRange
    }
    assert(elementRange.startOffset >= range.startOffset)
    return if (elementRange.endOffset <= range.endOffset) element else null
}

// ---------------------------------- Debug/logging ----------------------------------------------------------------------------------------

fun PsiElement.getElementTextWithContext(): String {
    assert(isValid) { "Invalid element $this" }

    if (this is PsiFile) {
        return containingFile.text
    }

    // Find parent for element among file children
    val topLevelElement = PsiTreeUtil.findFirstParent(this, { it.parent is PsiFile }) ?:
                          throw AssertionError("For non-file element we should always be able to find parent in file children")

    val startContextOffset = topLevelElement.startOffset
    val elementContextOffset = textRange.startOffset

    val inFileParentOffset = elementContextOffset - startContextOffset


    return StringBuilder(topLevelElement.text)
            .insert(inFileParentOffset, "<caret>")
            .insert(0, "File name: ${containingFile.name} Physical: ${containingFile.isPhysical}\n")
            .toString()
}

fun PsiElement.getTextWithLocation(): String = "'${this.text}' at ${DiagnosticUtils.atLocation(this)}"

fun replaceFileAnnotationList(file: KtFile, annotationList: KtFileAnnotationList): KtFileAnnotationList {
    if (file.fileAnnotationList != null) {
        return file.fileAnnotationList!!.replace(annotationList) as KtFileAnnotationList
    }

    val beforeAnchor : PsiElement? = when {
        file.packageDirective?.packageKeyword != null -> file.packageDirective!!
        file.importList != null -> file.importList!!
        file.declarations.firstOrNull() != null -> file.declarations.first()
        else -> null
    }

    if (beforeAnchor != null) {
        return file.addBefore(annotationList, beforeAnchor) as KtFileAnnotationList
    }

    if (file.lastChild == null) {
        return file.add(annotationList) as KtFileAnnotationList
    }

    return file.addAfter(annotationList, file.lastChild) as KtFileAnnotationList
}

// -----------------------------------------------------------------------------------------------------------------------------------------

operator fun SearchScope.contains(element: PsiElement): Boolean = PsiSearchScopeUtil.isInScope(this, element)

fun <E : PsiElement> E.createSmartPointer(): SmartPsiElementPointer<E> =
        SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)

fun PsiElement.before(element: PsiElement) = textRange.endOffset <= element.textRange.startOffset

inline fun <reified T : PsiElement> PsiElement.getLastParentOfTypeInRow() = parents.takeWhile { it is T }.lastOrNull() as? T

