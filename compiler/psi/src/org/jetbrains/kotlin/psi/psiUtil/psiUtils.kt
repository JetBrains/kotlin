/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
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
                    next = if (forward) result.nextSibling else result.prevSibling
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

fun PsiElement.prevLeaf(skipEmptyElements: Boolean = false): PsiElement? = PsiTreeUtil.prevLeaf(this, skipEmptyElements)

fun PsiElement.nextLeaf(skipEmptyElements: Boolean = false): PsiElement? = PsiTreeUtil.nextLeaf(this, skipEmptyElements)

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

fun <T : PsiElement> PsiElement.getParentOfTypes(strict: Boolean = false, vararg parentClasses: Class<out T>): T? {
    return getParentOfTypesAndPredicate(strict, *parentClasses) { true }
}

fun <T : PsiElement> PsiElement.getParentOfTypesAndPredicate(
    strict: Boolean = false, vararg parentClasses: Class<out T>, predicate: (T) -> Boolean
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

inline fun <reified T : PsiElement, reified V : PsiElement> PsiElement.getParentOfTypes2(): PsiElement? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, V::class.java)
}

inline fun <reified T : PsiElement, reified V : PsiElement, reified U : PsiElement> PsiElement.getParentOfTypes3(): PsiElement? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, V::class.java, U::class.java)
}

inline fun <reified T : PsiElement> PsiElement.getParentOfType(strict: Boolean, vararg stopAt: Class<out PsiElement>): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, strict, *stopAt)
}

inline fun <reified T : PsiElement> PsiElement.getStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, true)
}

inline fun <reified T : PsiElement> PsiElement.getNonStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, false)
}

inline fun <reified T : PsiElement> PsiElement.getTopmostParentOfType(): T? {
    return PsiTreeUtil.getTopmostParentOfType(this, T::class.java)
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

fun PsiElement.getNextSiblingIgnoringWhitespace(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself).filter { it !is PsiWhiteSpace }.firstOrNull()
}

fun PsiElement.getPrevSiblingIgnoringWhitespaceAndComments(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself, forward = false).filter { it !is PsiWhiteSpace && it !is PsiComment }.firstOrNull()
}

fun PsiElement.getPrevSiblingIgnoringWhitespace(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself, forward = false).filter { it !is PsiWhiteSpace }.firstOrNull()
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

inline fun <reified T : PsiElement> PsiElement.getParentOfTypeAndBranches(
    strict: Boolean = false,
    noinline branches: T.() -> Iterable<PsiElement?>
): T? {
    return getParentOfType<T>(strict)?.getIfChildIsInBranches(this, branches)
}

tailrec fun PsiElement.getOutermostParentContainedIn(container: PsiElement): PsiElement? {
    val parent = parent
    return if (parent == container) this else parent?.getOutermostParentContainedIn(container)
}

fun PsiElement.isInsideOf(elements: Iterable<PsiElement>): Boolean = elements.any { it.isAncestor(this) }

fun PsiChildRange.trimWhiteSpaces(): PsiChildRange {
    if (first == null) return this
    return PsiChildRange(
        first.siblings().firstOrNull { it !is PsiWhiteSpace },
        last!!.siblings(forward = false).firstOrNull { it !is PsiWhiteSpace })
}

// -------------------- Recursive tree visiting --------------------------------------------------------------------------------------------

inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(noinline action: (T) -> Unit) {
    forEachDescendantOfType({ true }, action)
}

inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline action: (T) -> Unit
) {
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

inline fun <reified T : PsiElement> PsiElement.anyDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): Boolean {
    return findDescendantOfType(canGoInside, predicate) != null
}

inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(noinline predicate: (T) -> Boolean = { true }): T? {
    return findDescendantOfType({ true }, predicate)
}

inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): T? {
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

inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): List<T> {
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

val KtPureElement.pureStartOffset: Int
    get() = psiOrParent.textRangeWithoutComments.startOffset

val KtPureElement.pureEndOffset: Int
    get() = psiOrParent.textRangeWithoutComments.endOffset

val PsiElement.startOffsetSkippingComments: Int
    get() {
        if (!startsWithComment()) return startOffset // fastpath
        val firstNonCommentChild = generateSequence(firstChild) { it.nextSibling }
            .firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
        return firstNonCommentChild?.startOffset ?: startOffset
    }

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

val PsiElement.textRangeWithoutComments: TextRange
    get() = if (!startsWithComment()) textRange else TextRange(startOffsetSkippingComments, endOffset)

fun PsiElement.startsWithComment(): Boolean = firstChild is PsiComment


// ---------------------------------- Debug/logging ----------------------------------------------------------------------------------------

fun PsiElement.getElementTextWithContext(): String {
    if (!isValid) return "<invalid element $this>"

    if (this is PsiFile) {
        return containingFile.text
    }

    // Find parent for element among file children
    val topLevelElement = PsiTreeUtil.findFirstParent(this, { it.parent is PsiFile })
        ?: throw AssertionError("For non-file element we should always be able to find parent in file children")

    val startContextOffset = topLevelElement.startOffset
    val elementContextOffset = textRange.startOffset

    val inFileParentOffset = elementContextOffset - startContextOffset


    val isInjected = containingFile is VirtualFileWindow
    return StringBuilder(topLevelElement.text)
        .insert(inFileParentOffset, "<caret>")
        .insert(0, "File name: ${containingFile.name} Physical: ${containingFile.isPhysical} Injected: $isInjected\n")
        .toString()
}

fun PsiElement.getTextWithLocation(): String = "'${this.text}' at ${PsiDiagnosticUtils.atLocation(this)}"

fun replaceFileAnnotationList(file: KtFile, annotationList: KtFileAnnotationList): KtFileAnnotationList {
    if (file.fileAnnotationList != null) {
        return file.fileAnnotationList!!.replace(annotationList) as KtFileAnnotationList
    }

    val beforeAnchor: PsiElement? = when {
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

inline fun <reified T : PsiElement> PsiElement.getLastParentOfTypeInRowWithSelf() = parentsWithSelf
    .takeWhile { it is T }.lastOrNull() as? T

fun KtModifierListOwner.hasExpectModifier() = hasModifier(KtTokens.HEADER_KEYWORD) || hasModifier(KtTokens.EXPECT_KEYWORD)
fun KtModifierList.hasExpectModifier() = hasModifier(KtTokens.HEADER_KEYWORD) || hasModifier(KtTokens.EXPECT_KEYWORD)

fun KtModifierListOwner.hasActualModifier() = hasModifier(KtTokens.IMPL_KEYWORD) || hasModifier(KtTokens.ACTUAL_KEYWORD)
fun KtModifierList.hasActualModifier() = hasModifier(KtTokens.IMPL_KEYWORD) || hasModifier(KtTokens.ACTUAL_KEYWORD)

fun ASTNode.children() = generateSequence(firstChildNode) { node -> node.treeNext }
fun ASTNode.parents() = generateSequence(treeParent) { node -> node.treeParent }

fun ASTNode.siblings(forward: Boolean = true): Sequence<ASTNode> {
    if (forward) {
        return generateSequence(treeNext) { it.treeNext }
    } else {
        return generateSequence(treePrev) { it.treePrev }
    }
}

fun ASTNode.leaves(forward: Boolean = true): Sequence<ASTNode> {
    if (forward) {
        return generateSequence(TreeUtil.nextLeaf(this)) { TreeUtil.nextLeaf(it) }
    } else {
        return generateSequence(TreeUtil.prevLeaf(this)) { TreeUtil.prevLeaf(it) }
    }
}

fun ASTNode.closestPsiElement(): PsiElement? {
    var node = this
    while (node.psi == null) {
        node = node.treeParent
    }
    return node.psi
}

fun LazyParseablePsiElement.getContainingKtFile(): KtFile {

    val file = this.containingFile

    if (file is KtFile) return file

    val fileString = if (file != null && file.isValid) file.text else ""
    throw IllegalStateException("KtElement not inside KtFile: $file with text \"$fileString\" for element $this of type ${this::class.java} node = ${this.node}")
}
