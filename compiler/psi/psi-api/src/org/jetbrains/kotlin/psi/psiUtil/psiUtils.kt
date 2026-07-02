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

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.psi.search.SearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.PLUS
import org.jetbrains.kotlin.psi.*
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// NOTE: in this file we collect only LANGUAGE INDEPENDENT methods working with PSI and not modifying it

// ----------- Walking children/siblings/parents -------------------------------------------------------------------------------------------

/**
 * Returns all descendants of this expression in flattened form if it is a string-literal concatenation; otherwise,
 * returns `null`.
 *
 * For example, for `"a0" /* comment before plus */ + /* comment after plus */ "a1"`, this returns `"a0"`,
 * whitespace, `/* comment before plus */`, whitespace, `+`, whitespace, `/* comment after plus */`, whitespace,
 * and `"a1"`.
 *
 * @see tryFlattenStringConcatenation
 */
@KtImplementationDetail
fun KtBinaryExpression.tryFlattenStringConcatenationDescendants(): List<PsiElement>? {
    return tryFlattenStringConcatenation(fullFidelity = true)
}

/**
 * Returns the string-template arguments of this expression if it is a string-literal concatenation; otherwise, returns
 * `null`.
 *
 * For example, for `"a0" /* comment before plus */ + /* comment after plus */ "a1"`, this returns `"a0"` and `"a1"`.
 *
 * @see tryFlattenStringConcatenation
 */
@KtImplementationDetail
fun KtBinaryExpression.tryFlattenStringConcatenationArguments(): List<KtStringTemplateExpression>? {
    @Suppress("UNCHECKED_CAST")
    return tryFlattenStringConcatenation(fullFidelity = false) as? List<KtStringTemplateExpression>
}

/**
 * Emulates recursion with an explicit stack to avoid stack overflows on large string-concatenation expressions such as
 * `val x = "a0" + "a1" + ... + "a9999"` (relatively common in machine-generated code).
 *
 * This method traverses this expression and tries to extract all string-template nodes. It returns the nested
 * expressions in source order if this [KtBinaryExpression] matches the string-literal concatenation pattern;
 * otherwise, it returns `null`. Nested expressions are processed iteratively by pushing nodes onto an input stack.
 *
 * For example, `"a" + "b" + "c"` is represented as:
 *
 * ```
 *          '+'(0)
 *      '+'(1)     'c'
 *  'a'        'b'
 * ```
 * The method returns `'a', 'b', 'c'` when [fullFidelity] is `false`. When [fullFidelity] is `true`, it returns
 * `'a', 'b', '+'(1), 'c', '+'(0)` plus hidden tokens between them (whitespace or comments). Use the full-fidelity mode
 * when callers need the complete tree structure.
 */
@KtImplementationDetail
private fun KtBinaryExpression.tryFlattenStringConcatenation(fullFidelity: Boolean): List<PsiElement>? {
    // Optimization: don't allocate anything if the root expression doesn't match the string concatenation folding pattern
    if (operationToken != PLUS) return null

    val input = mutableListOf<PsiElement>().also { it.add(this) }
    val output = ArrayDeque<PsiElement>()

    while (true) {
        when (val node = input.removeLastOrNull() ?: break) {
            is KtBinaryExpression -> {
                var child = node.firstChild
                while (child != null) {
                    input.add(child)
                    child = child.nextSibling
                }
            }
            is KtStringTemplateExpression -> {
                output.addFirst(node)
            }
            is PsiWhiteSpace,
            is PsiComment
                -> {
                if (fullFidelity) {
                    output.addFirst(node)
                }
            }
            is KtOperationReferenceExpression -> {
                if (node.operationSignTokenType != PLUS) {
                    return null
                }

                if (fullFidelity) {
                    output.addFirst(node)
                }
            }
            else -> {
                return null
            }
        }
    }

    return output
}

/** The range of all direct children of this element, or an empty range if it has none. */
val PsiElement.allChildren: PsiChildRange
    get() {
        val first = firstChild
        return if (first != null) PsiChildRange(first, lastChild) else PsiChildRange.EMPTY
    }

/**
 * Returns the siblings of this element as a lazy sequence, going [forward] (or backward) and optionally including the
 * element itself ([withItself]).
 */
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

/** This element and its ancestors, from the element itself up to and including the containing file. */
val PsiElement.parentsWithSelf: Sequence<PsiElement>
    get() = generateSequence(this) { if (it is PsiFile) null else it.parent }

/** The ancestors of this element, from its immediate parent up to and including the containing file. */
val PsiElement.parents: Sequence<PsiElement>
    get() = parentsWithSelf.drop(1)

/** Returns the previous leaf element in the tree, or `null` if there is none. Empty leaves are skipped if [skipEmptyElements]. */
fun PsiElement.prevLeaf(skipEmptyElements: Boolean = false): PsiElement? = PsiTreeUtil.prevLeaf(this, skipEmptyElements)

/** Returns the next leaf element in the tree, or `null` if there is none. Empty leaves are skipped if [skipEmptyElements]. */
fun PsiElement.nextLeaf(skipEmptyElements: Boolean = false): PsiElement? = PsiTreeUtil.nextLeaf(this, skipEmptyElements)

/** The preceding leaf elements as a lazy sequence, nearest first. */
val PsiElement.prevLeafs: Sequence<PsiElement>
    get() = generateSequence({ prevLeaf() }, { it.prevLeaf() })

/** The following leaf elements as a lazy sequence, nearest first. */
val PsiElement.nextLeafs: Sequence<PsiElement>
    get() = generateSequence({ nextLeaf() }, { it.nextLeaf() })

/** Returns the nearest preceding leaf that satisfies [filter], or `null` if there is none. */
fun PsiElement.prevLeaf(filter: (PsiElement) -> Boolean): PsiElement? {
    var leaf = prevLeaf()
    while (leaf != null && !filter(leaf)) {
        leaf = leaf.prevLeaf()
    }
    return leaf
}

/** Returns the nearest following leaf that satisfies [filter], or `null` if there is none. */
fun PsiElement.nextLeaf(filter: (PsiElement) -> Boolean): PsiElement? {
    var leaf = nextLeaf()
    while (leaf != null && !filter(leaf)) {
        leaf = leaf.nextLeaf()
    }
    return leaf
}

/**
 * Returns the nearest ancestor that is an instance of one of [parentClasses], or `null` if there is none. When [strict]
 * is `false`, this element itself is also considered.
 */
fun <T : PsiElement> PsiElement.getParentOfTypes(strict: Boolean = false, vararg parentClasses: Class<out T>): T? {
    return getParentOfTypesAndPredicate(strict, *parentClasses) { true }
}

/**
 * Returns the nearest ancestor that is an instance of one of [parentClasses] and satisfies [predicate], or `null`.
 * When [parentClasses] is empty, any type matches. When [strict] is `false`, this element itself is also considered.
 */
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

/** Returns the nearest ancestor of type [T] (including this element), or `null` if there is none. */
fun <T : PsiElement> PsiElement.getNonStrictParentOfType(parentClass: Class<T>): T? {
    return PsiTreeUtil.getParentOfType(this, parentClass, false)
}

/** Returns the nearest ancestor of type [T], or `null`. When [strict] is `false`, this element itself is also considered. */
inline fun <reified T : PsiElement> PsiElement.getParentOfType(strict: Boolean): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, strict)
}

/** Returns the nearest strict ancestor that is a [T] or [V], or `null` if there is none. */
inline fun <reified T : PsiElement, reified V : PsiElement> PsiElement.getParentOfTypes2(): PsiElement? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, V::class.java)
}

/** Returns the nearest strict ancestor that is a [T], [V], or [U], or `null` if there is none. */
inline fun <reified T : PsiElement, reified V : PsiElement, reified U : PsiElement> PsiElement.getParentOfTypes3(): PsiElement? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, V::class.java, U::class.java)
}

/**
 * Returns the nearest ancestor of type [T], stopping the search once an ancestor of any type in [stopAt] is reached,
 * or `null`. When [strict] is `false`, this element itself is also considered.
 */
inline fun <reified T : PsiElement> PsiElement.getParentOfType(strict: Boolean, vararg stopAt: Class<out PsiElement>): T? {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    return PsiTreeUtil.getParentOfType(this, T::class.java, strict, *stopAt)
}

/** Returns the nearest strict ancestor of type [T], or `null` if there is none. */
inline fun <reified T : PsiElement> PsiElement.getStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, true)
}

/** Returns the nearest ancestor of type [T] including this element, or `null` if there is none. */
inline fun <reified T : PsiElement> PsiElement.getNonStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, false)
}

/** Returns the outermost ancestor of type [T] including this element, or `null` if there is none. */
inline fun <reified T : PsiElement> PsiElement.getTopmostParentOfType(): T? {
    return PsiTreeUtil.getTopmostParentOfType(this, T::class.java)
}

/** Returns the first descendant of type [T], or `null` if there is none. */
inline fun <reified T : PsiElement> PsiElement.getChildOfType(): T? {
    return PsiTreeUtil.getChildOfType(this, T::class.java)
}

/** Returns the direct children of type [T], or an empty array if there are none. */
inline fun <reified T : PsiElement> PsiElement.getChildrenOfType(): Array<T> {
    return PsiTreeUtil.getChildrenOfType(this, T::class.java) ?: arrayOf()
}

/** Returns the next sibling that is neither whitespace nor a comment, or `null`. Considers this element if [withItself]. */
fun PsiElement.getNextSiblingIgnoringWhitespaceAndComments(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself).filter { it !is PsiWhiteSpace && it !is PsiComment }.firstOrNull()
}

/** Returns the next sibling that is not whitespace, or `null`. Considers this element if [withItself]. */
fun PsiElement.getNextSiblingIgnoringWhitespace(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself).filter { it !is PsiWhiteSpace }.firstOrNull()
}

/** Returns the previous sibling that is neither whitespace nor a comment, or `null`. Considers this element if [withItself]. */
fun PsiElement.getPrevSiblingIgnoringWhitespaceAndComments(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself, forward = false).filter { it !is PsiWhiteSpace && it !is PsiComment }.firstOrNull()
}

/** Returns the previous sibling that is not whitespace, or `null`. Considers this element if [withItself]. */
fun PsiElement.getPrevSiblingIgnoringWhitespace(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself, forward = false).filter { it !is PsiWhiteSpace }.firstOrNull()
}

/** Returns the next sibling of the same type [T], or `null` if there is none. */
inline fun <reified T : PsiElement> T.nextSiblingOfSameType() = PsiTreeUtil.getNextSiblingOfType(this, T::class.java)

/** Returns the previous sibling of the same type [T], or `null` if there is none. */
inline fun <reified T : PsiElement> T.prevSiblingOfSameType() = PsiTreeUtil.getPrevSiblingOfType(this, T::class.java)

/** Returns `true` if this element is an ancestor of [element]. When [strict] is `false`, an element is its own ancestor. */
fun PsiElement?.isAncestor(element: PsiElement, strict: Boolean = false): Boolean {
    return PsiTreeUtil.isAncestor(this, element, strict)
}

/** Returns this element if [element] lies within the subtree returned by [branch], otherwise `null`. */
fun <T : PsiElement> T.getIfChildIsInBranch(element: PsiElement, branch: T.() -> PsiElement?): T? {
    return if (branch().isAncestor(element)) this else null
}

/** Returns this element if [element] lies within any of the subtrees returned by [branches], otherwise `null`. */
fun <T : PsiElement> T.getIfChildIsInBranches(element: PsiElement, branches: T.() -> Iterable<PsiElement?>): T? {
    return if (branches().any { it.isAncestor(element) }) this else null
}

/**
 * Returns the nearest ancestor of type [T] for which this element lies within the subtree selected by [branch], or
 * `null`. When [strict] is `false`, this element itself is also considered.
 */
inline fun <reified T : PsiElement> PsiElement.getParentOfTypeAndBranch(strict: Boolean = false, noinline branch: T.() -> PsiElement?): T? {
    return getParentOfType<T>(strict)?.getIfChildIsInBranch(this, branch)
}

/**
 * Returns the nearest ancestor of type [T] for which this element lies within any subtree selected by [branches], or
 * `null`. When [strict] is `false`, this element itself is also considered.
 */
inline fun <reified T : PsiElement> PsiElement.getParentOfTypeAndBranches(
    strict: Boolean = false,
    noinline branches: T.() -> Iterable<PsiElement?>
): T? {
    return getParentOfType<T>(strict)?.getIfChildIsInBranches(this, branches)
}

/** Returns the outermost ancestor of this element that is still strictly contained in [container], or `null`. */
tailrec fun PsiElement.getOutermostParentContainedIn(container: PsiElement): PsiElement? {
    val parent = parent
    return if (parent == container) this else parent?.getOutermostParentContainedIn(container)
}

/** Returns `true` if this element is a descendant of any of the given [elements]. */
fun PsiElement.isInsideOf(elements: Iterable<PsiElement>): Boolean = elements.any { it.isAncestor(this) }

/** Returns a copy of this range with leading and trailing whitespace elements removed. */
fun PsiChildRange.trimWhiteSpaces(): PsiChildRange {
    if (first == null) return this
    return PsiChildRange(
        first.siblings().firstOrNull { it !is PsiWhiteSpace },
        last!!.siblings(forward = false).firstOrNull { it !is PsiWhiteSpace })
}

/**
 * See [unwrap()][org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilder.unwrap]
 */
val UNWRAPPABLE_TOKEN_TYPES: Set<IElementType> = setOf(PARENTHESIZED, LABELED_EXPRESSION, ANNOTATED_EXPRESSION)

/**
 * Returns the left-hand-side source element if it is wrapped in parentheses, a label, or annotations.
 *
 * This should only be called for a source element corresponding to an assignment, augmented-assignment operator call,
 * increment operator, or decrement operator.
 */
fun PsiElement.getAssignmentLhsIfUnwrappable(): PsiElement? =
    when {
        // In `++(x)` the LHS source `(x)` is the last child
        elementType == PREFIX_EXPRESSION -> children.lastOrNull()
        // In `(x)++` or `(x) = ...` the LHS source is the first child
        else -> children.firstOrNull()
    }.takeIf {
        it?.elementType in UNWRAPPABLE_TOKEN_TYPES
    }

/** Returns the source of the explicit receiver if this element is a dot-qualified expression, or `null` otherwise. */
fun PsiElement.getExplicitReceiverOfDotQualified(): PsiElement? =
    when {
        elementType == DOT_QUALIFIED_EXPRESSION -> children.firstOrNull()
        else -> null
    }


// -------------------- Recursive tree visiting --------------------------------------------------------------------------------------------

/** Applies [action] to every descendant of type [T], in post-order. */
inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(noinline action: (T) -> Unit) {
    forEachDescendantOfType({ true }, action)
}

/**
 * Applies [action] to every descendant of type [T], in post-order, descending into a subtree only when [canGoInside]
 * returns `true` for its root.
 */
inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline action: (T) -> Unit
) {
    checkDecompiledText()
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

/** Applies [action] to every descendant of type [T], in pre-order (parent before children). */
inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfTypeInPreorder(noinline action: (T) -> Unit) {
    forEachDescendantOfTypeInPreorder({ true }, action)
}

/**
 * Applies [action] to every descendant of type [T], in pre-order, descending into a subtree only when [canGoInside]
 * returns `true` for its root.
 */
inline fun <reified T : PsiElement> PsiElement.forEachDescendantOfTypeInPreorder(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline action: (T) -> Unit,
) {
    checkDecompiledText()
    this.accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is T) {
                action(element)
            }

            if (canGoInside(element)) {
                super.visitElement(element)
            }
        }
    })
}

/** Returns `true` if any descendant of type [T] satisfies [predicate]. */
inline fun <reified T : PsiElement> PsiElement.anyDescendantOfType(noinline predicate: (T) -> Boolean = { true }): Boolean {
    return findDescendantOfType(predicate) != null
}

/** Returns `true` if any descendant of type [T] satisfies [predicate], descending only where [canGoInside] allows. */
inline fun <reified T : PsiElement> PsiElement.anyDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): Boolean {
    return findDescendantOfType(canGoInside, predicate) != null
}

/** Returns the first descendant of type [T] satisfying [predicate], or `null` if there is none. */
inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(noinline predicate: (T) -> Boolean = { true }): T? {
    return findDescendantOfType({ true }, predicate)
}

/**
 * Returns the first descendant of type [T] satisfying [predicate], descending only where [canGoInside] allows, or
 * `null` if there is none.
 */
inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): T? {
    checkDecompiledText()
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

/**
 * Throws if this element belongs to a compiled file that is backed by a stub. Loading decompiled text is slow and
 * should be avoided; stubs should be used instead. Called by the descendant-traversal helpers as a guard.
 */
fun PsiElement.checkDecompiledText() {
    val file = containingFile
    if (file is KtFile && file.isCompiled && file.stub != null) {
        error("Attempt to load decompiled text, please use stubs instead. Decompile process might be slow and should be avoided")
    }
}

/** Collects all descendants of type [T] satisfying [predicate] into a list. */
inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(noinline predicate: (T) -> Boolean = { true }): List<T> {
    return collectDescendantsOfType({ true }, predicate)
}

/** Collects all descendants of type [T] satisfying [predicate] into a list, descending only where [canGoInside] allows. */
inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): List<T> = collectDescendantsOfTypeTo(ArrayList(), canGoInside, predicate)

/** Collects all descendants of type [T] satisfying [predicate] into [to], descending only where [canGoInside] allows. */
inline fun <reified T : PsiElement, C : MutableCollection<T>> PsiElement.collectDescendantsOfTypeTo(
    to: C,
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true }
): C {
    forEachDescendantOfType<T>(canGoInside) {
        if (predicate(it)) {
            to.add(it)
        }
    }
    return to
}

// ----------- Working with offsets, ranges, and text --------------------------------------------------------------------------------------

/** The offset of the start of this element's text range in the file. */
val PsiElement.startOffset: Int
    get() = textRange.startOffset

/** The offset just past the end of this element's text range in the file. */
val PsiElement.endOffset: Int
    get() = textRange.endOffset

/** The start offset of this element's source, excluding any leading comments. */
val KtPureElement.pureStartOffset: Int
    get() = psiOrParent.textRangeWithoutComments.startOffset

/** The end offset of this element's source. */
val KtPureElement.pureEndOffset: Int
    get() = psiOrParent.textRangeWithoutComments.endOffset

/** The start offset of this element, skipping leading comments and whitespace. */
val PsiElement.startOffsetSkippingComments: Int
    get() {
        if (!startsWithComment()) return startOffset // fast path
        val firstNonCommentChild = generateSequence(firstChild) { it.nextSibling }
            .firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
        return firstNonCommentChild?.startOffset ?: startOffset
    }

/** Returns the offset of this element relative to the start of [ancestor] (which must be an ancestor of this element). */
fun PsiElement.getStartOffsetIn(ancestor: PsiElement): Int {
    var offset = 0
    var parent = this
    while (parent != ancestor) {
        offset += parent.startOffsetInParent
        parent = parent.parent
    }
    return offset
}

/** Returns `true` if [offset] lies strictly inside this range (excluding both endpoints). */
fun TextRange.containsInside(offset: Int): Boolean = startOffset < offset && offset < endOffset

/** The text range spanning this child range, or `null` if the range is empty. */
val PsiChildRange.textRange: TextRange?
    get() {
        if (isEmpty) return null
        return TextRange(first!!.startOffset, last!!.endOffset)
    }

/** Returns the concatenated text of all elements in this range, or an empty string if the range is empty. */
fun PsiChildRange.getText(): String {
    if (isEmpty) return ""
    return this.map { it.text }.joinToString("")
}

/** Returns the top-level elements that together cover the given [range] within this file. */
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

/** The text range of this element excluding any leading comments. */
val PsiElement.textRangeWithoutComments: TextRange
    get() = if (!startsWithComment()) textRange else TextRange(startOffsetSkippingComments, endOffset)

/** Returns `true` if this element's first child is a comment. */
fun PsiElement.startsWithComment(): Boolean = firstChild is PsiComment


// ---------------------------------- Debug/logging ----------------------------------------------------------------------------------------

/** Returns this element's text with surrounding source context, for use in diagnostics and log messages. */
fun PsiElement.getElementTextWithContext(): String = org.jetbrains.kotlin.utils.getElementTextWithContext(this)

/** Returns this element's text together with its source location, for use in diagnostics and log messages. */
fun PsiElement.getTextWithLocation(): String = "'${this.text}' at ${PsiDiagnosticUtils.atLocation(this)}"

@Deprecated(
    "Use file.replaceFileAnnotationList(annotationList) instead",
    ReplaceWith(
        "file.replaceFileAnnotationList(annotationList)",
        "org.jetbrains.kotlin.idea.base.psi.replaceFileAnnotationList",
    ),
)
@OptIn(KtNonPublicApi::class)
fun replaceFileAnnotationList(file: KtFile, annotationList: KtFileAnnotationList): KtFileAnnotationList {
    return KtPsiMutationService.getInstance().replaceFileAnnotationList(file, annotationList)
}

// -----------------------------------------------------------------------------------------------------------------------------------------

/** Returns `true` if [element] lies within this search scope. */
operator fun SearchScope.contains(element: PsiElement): Boolean = PsiSearchScopeUtil.isInScope(this, element)

@Deprecated(
    "Use only in 'kotlin' repo until the alternative method from 'com.intellij.psi' package becomes available from the IJ platform",
    ReplaceWith("this.createSmartPointer()", "com.intellij.psi.createSmartPointer"),
)
fun <E : PsiElement> E.createSmartPointer(): SmartPsiElementPointer<E> =
    SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)

/** Returns `true` if this element ends at or before [element] starts (that is, it precedes [element] in the file). */
fun PsiElement.before(element: PsiElement) = textRange.endOffset <= element.textRange.startOffset

/** Returns the outermost element in an uninterrupted chain of ancestors of type [T], or `null` if the parent is not a [T]. */
inline fun <reified T : PsiElement> PsiElement.getLastParentOfTypeInRow() = parents.takeWhile { it is T }.lastOrNull() as? T

/** Like [getLastParentOfTypeInRow], but the chain may start at this element itself. */
inline fun <reified T : PsiElement> PsiElement.getLastParentOfTypeInRowWithSelf() = parentsWithSelf
    .takeWhile { it is T }.lastOrNull() as? T

/** Returns `true` if this declaration has the `expect` modifier. */
fun KtModifierListOwner.hasExpectModifier() = hasModifier(KtTokens.EXPECT_KEYWORD)

/** Returns `true` if this modifier list contains the `expect` modifier. */
fun KtModifierList.hasExpectModifier() = hasModifier(KtTokens.EXPECT_KEYWORD)

/** Returns `true` if this declaration has the `actual` modifier. */
fun KtModifierListOwner.hasActualModifier() = hasModifier(KtTokens.ACTUAL_KEYWORD)

/** Returns `true` if this modifier list contains the `actual` modifier. */
fun KtModifierList.hasActualModifier() = hasModifier(KtTokens.ACTUAL_KEYWORD)

/** Returns `true` if this modifier list contains the `suspend` modifier. */
fun KtModifierList.hasSuspendModifier() = hasModifier(KtTokens.SUSPEND_KEYWORD)

/** Returns `true` if this modifier list contains the `fun` modifier (a functional interface). */
fun KtModifierList.hasFunModifier() = hasModifier(KtTokens.FUN_KEYWORD)

/** Returns `true` if this modifier list contains the `value` modifier (a value class). */
fun KtModifierList.hasValueModifier() = hasModifier(KtTokens.VALUE_KEYWORD)

/** Returns `true` if this declaration has the `inner` modifier. */
fun KtModifierListOwner.hasInnerModifier() = hasModifier(KtTokens.INNER_KEYWORD)

/** Returns `true` if this declaration has the `external` modifier. */
fun KtModifierListOwner.hasExternalModifier(): Boolean = hasModifier(KtTokens.EXTERNAL_KEYWORD)

/** The direct child nodes of this AST node, as a lazy sequence. */
fun ASTNode.children() = generateSequence(firstChildNode) { node -> node.treeNext }

/** The ancestor nodes of this AST node, from its parent upward, as a lazy sequence. */
fun ASTNode.parents() = generateSequence(treeParent) { node -> node.treeParent }

/** The sibling nodes of this AST node, going [forward] (or backward), as a lazy sequence. */
fun ASTNode.siblings(forward: Boolean = true): Sequence<ASTNode> {
    if (forward) {
        return generateSequence(treeNext) { it.treeNext }
    } else {
        return generateSequence(treePrev) { it.treePrev }
    }
}

/** The leaf nodes following (or preceding, if not [forward]) this AST node, as a lazy sequence. */
fun ASTNode.leaves(forward: Boolean = true): Sequence<ASTNode> {
    if (forward) {
        return generateSequence(TreeUtil.nextLeaf(this)) { TreeUtil.nextLeaf(it) }
    } else {
        return generateSequence(TreeUtil.prevLeaf(this)) { TreeUtil.prevLeaf(it) }
    }
}

/** Returns the nearest PSI element for this node, walking up to a parent node if this node has no PSI of its own. */
fun ASTNode.closestPsiElement(): PsiElement? {
    var node = this
    while (node.psi == null) {
        node = node.treeParent
    }
    return node.psi
}

/**
 * Returns the [KtFile] containing this element.
 *
 * @throws IllegalStateException if the element is not inside a [KtFile]
 */
fun LazyParseablePsiElement.getContainingKtFile(): KtFile {

    val file = this.containingFile

    if (file is KtFile) return file

    val fileString = if (file != null && file.isValid) file.text else ""
    throw IllegalStateException("KtElement not inside KtFile: $file with text \"$fileString\" for element $this of type ${this::class.java} node = ${this.node}")
}

/** Returns `true` if this expression is the `null` literal. Smart-casts the receiver to [KtConstantExpression] on `true`. */
@OptIn(ExperimentalContracts::class)
fun KtExpression.isNull(): Boolean {
    contract {
        returns(true) implies (this@isNull is KtConstantExpression)
    }
    return this is KtConstantExpression && this.node.elementType == KtNodeTypes.NULL
}

/** Recursively unwraps parentheses, labels, and annotations from this element, returning the innermost expression. */
fun PsiElement?.unwrapParenthesesLabelsAndAnnotations(): PsiElement? {
    var unwrapped = this
    while (true) {
        unwrapped = when (unwrapped) {
            is KtParenthesizedExpression -> unwrapped.expression
            is KtLabeledExpression -> unwrapped.baseExpression
            is KtAnnotatedExpression -> unwrapped.baseExpression
            else -> return unwrapped
        }
    }
}

/**
 * Walks up from this element through enclosing parentheses, labels, and annotations, returning the outermost element
 * that still wraps it.
 */
fun PsiElement.unwrapParenthesesLabelsAndAnnotationsDeeply(): PsiElement {
    var current: PsiElement = this
    var unwrapped: PsiElement?

    do {
        unwrapped = current.parent?.unwrapParenthesesLabelsAndAnnotations()
        current = current.parent
    } while (unwrapped != current)

    return unwrapped
}
