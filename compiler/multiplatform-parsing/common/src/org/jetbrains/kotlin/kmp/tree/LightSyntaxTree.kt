/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.tree

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import com.intellij.platform.syntax.lexer.TokenList
import com.intellij.platform.syntax.parser.ProductionMarkerList
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import com.intellij.platform.syntax.parser.prepareProduction
import org.jetbrains.annotations.TestOnly
import kotlin.jvm.JvmInline

/**
 * Identifier for a node within a [LightSyntaxTree], encoded in a single Int.
 *
 * Encoding:
 *  - Non-negative values `[0..markerCount-1]`: composite (START) marker index.
 *  - `markerCount` (i.e. [LightSyntaxTree.rootIndex]): the synthetic file root that wraps all
 *    top-level productions.
 *  - Negative values: token index encoded as `-(tokenIndex + 1)`. So `-1` is token at index 0.
 *  - `Int.MIN_VALUE`: invalid / "not computed" sentinel ([LightSyntaxTree.NO_NODE]).
 *
 * Since equality alone does not distinguish nodes from different trees. Higher-level abstractions
 * need to compare both the node and the owning tree.
 */
@JvmInline
value class LightNode(val index: Int)

/**
 * Flat-array AST representation produced from a [SyntaxTreeBuilder]. Holds the raw
 * [ProductionMarkerList] / [TokenList] plus precomputed lookup arrays that permit O(1)
 * access to parent, children, and node type,precomputed during construction (in
 * [buildLanguageSpecificLightTree]).
 *
 * Inspired by Kotlin's internal LightTree implementation, but with an eager lookup computation
 * and without using IJ platform dependencies, aside from new parsing infrastructure.
 */
class LightSyntaxTree(
    val tokens: TokenList,
    val source: CharSequence,
    /** For each START-marker index, the START-marker index of its parent (or [rootIndex] for top-level markers). */
    private val parentStartIndex: IntArray,
    /** For each token index, the START-marker index of its enclosing composite, or [rootIndex] for top-level tokens. */
    private val tokenParentStart: IntArray,
    /** Index used by [getRoot]: synthetic root wraps all top-level production markers. */
    val rootIndex: Int,
    /** [SyntaxElementType] used for the synthetic root node. */
    private val rootNodeType: SyntaxElementType,
    /**
     * Precomputed [SyntaxElementType] for each composite marker, indexed by START-marker index.
     * Entries for DONE markers and error markers are also populated during construction.
     */
    private val compositeTypes: Array<SyntaxElementType?>,
    /**
     * Precomputed children for each composite node, indexed by START-marker index.
     * Entry at [rootIndex] holds the synthetic root's children.
     * Each entry is a [ChildrenList] backed by an [IntArray] of child node indices.
     * Error markers and tokens map to [emptyList].
     */
    private val childrenByIndex: Array<List<LightNode>>,
    /**
     * Precomputed end offsets for composite nodes, indexed by START-marker index.
     * For normal composites this is the DONE marker's end offset; for error markers
     * it is the marker's own end offset.
     */
    private val compositeEndOffsets: IntArray,
    /**
     * Precomputed start offsets for composite nodes, indexed by START-marker index.
     */
    private val compositeStartOffsets: IntArray,
    /**
     * Builder of tree structure like `JavaLightTreeStructure`
     */
    buildLanguageSpecificTreeStructure: (LightSyntaxTree) -> Any
) {
    /**
     * Memoized language-specific structure adapter over this tree, used to build
     * `KtLightSourceElement`s for java-direct FIR declarations. One instance per tree, so all source
     * elements from the same file share a single tree structure (stable identity/equality).
     */
    val lightSourceTreeStructure: Any by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildLanguageSpecificTreeStructure(this)
    }

    fun getRoot(): LightNode = LightNode(rootIndex)

    private fun isSyntheticRoot(node: LightNode): Boolean = node.index == rootIndex

    fun isComposite(node: LightNode): Boolean = node.index in 0..rootIndex
    fun isToken(node: LightNode): Boolean = node.index < 0 && node.index != Int.MIN_VALUE

    fun getType(node: LightNode): SyntaxElementType {
        val idx = node.index
        if (idx >= 0) {
            // Composite (including synthetic root at idx == rootIndex)
            return if (idx == rootIndex) rootNodeType else compositeTypes[idx]!!
        }
        // Token
        val tokenIdx = -(idx + 1)
        return tokens.getTokenType(tokenIdx) ?: error("No token type at index $tokenIdx")
    }

    fun getStartOffset(node: LightNode): Int {
        val idx = node.index
        if (idx >= 0) return if (idx == rootIndex) 0 else compositeStartOffsets[idx]
        val tokenIdx = -(idx + 1)
        return tokens.getTokenStart(tokenIdx)
    }

    fun getEndOffset(node: LightNode): Int {
        val idx = node.index
        if (idx >= 0) return if (idx == rootIndex) source.length else compositeEndOffsets[idx]
        val tokenIdx = -(idx + 1)
        return tokens.getTokenEnd(tokenIdx)
    }

    fun getText(node: LightNode): CharSequence = source.subSequence(getStartOffset(node), getEndOffset(node))

    fun textEquals(node: LightNode, expected: String): Boolean {
        val start = getStartOffset(node)
        val end = getEndOffset(node)
        val length = end - start
        if (length != expected.length) return false
        for (i in 0 until length) {
            if (source[start + i] != expected[i]) return false
        }
        return true
    }

    /**
     * Returns the parent of [node], or `null` for the root.
     */
    fun getParent(node: LightNode): LightNode? {
        if (node.index == Int.MIN_VALUE) return null
        if (isSyntheticRoot(node)) return null
        return if (isComposite(node)) {
            LightNode(parentStartIndex[node.index])
        } else {
            val tokenIdx = -(node.index + 1)
            LightNode(tokenParentStart[tokenIdx])
        }
    }

    /**
     * Returns the immediate children of [node] in source order.
     */
    fun getChildren(node: LightNode): List<LightNode> {
        val idx = node.index
        if (idx < 0) return emptyList()
        return childrenByIndex[idx]
    }

    fun findChildByType(node: LightNode, type: SyntaxElementType): LightNode? {
        val children = getChildren(node)
        for (i in children.indices) {
            val child = children[i]
            if (getType(child) == type) return child
        }
        return null
    }

    fun getChildrenByType(node: LightNode, type: SyntaxElementType): List<LightNode> {
        val children = getChildren(node)
        if (children.isEmpty()) return emptyList()
        return children.filterTo(ArrayList(minOf(4, children.size))) { getType(it) == type }
    }

    fun hasChildOfType(node: LightNode, type: SyntaxElementType): Boolean = findChildByType(node, type) != null

    companion object {
        /** Sentinel "no node" value for use as a not-computed marker in callers. */
        val NO_NODE: LightNode = LightNode(Int.MIN_VALUE)
    }
}

/**
 * Lightweight [List] view over a precomputed [IntArray] of child node indices.
 */
private class ChildrenList(private val indices: IntArray) : AbstractList<LightNode>() {
    override val size: Int get() = indices.size
    override fun get(index: Int): LightNode = LightNode(indices[index])
}

/**
 * Builds a [LightSyntaxTree] from a populated [SyntaxTreeBuilder].
 *
 * Performs two passes over the production markers:
 * 1. Composite and token index computation (parent, done-index, type, offsets, token-to-parent mapping).
 * 2. Children list construction.
 */
fun buildLanguageSpecificLightTree(
    builder: SyntaxTreeBuilder,
    source: CharSequence,
    buildLanguageSpecificTreeStructure: (LightSyntaxTree) -> Any,
    isComment: (SyntaxElementType) -> Boolean,
): LightSyntaxTree {
    val productionMarkers = prepareProduction(builder).productionMarkers
    val tokens = builder.tokens
    val markerCount = productionMarkers.size
    if (markerCount == 0) error("No production markers")

    // The outermost marker pair is the `JAVA_FILE` node opened by [parse]; both passes skip it and attribute
    // its children to the tree root instead, so the root remains the compilation unit.
    val fileDoneIndex = markerCount - 1

    val parentStartIndex = IntArray(markerCount) { markerCount }
    val doneForStart = IntArray(markerCount) { -1 }
    val compositeTypes = arrayOfNulls<SyntaxElementType>(markerCount)
    val errorFlags = BooleanArray(markerCount)
    val compositeStartOffsets = IntArray(markerCount)
    val compositeEndOffsets = IntArray(markerCount)
    val tokenParentStart = IntArray(tokens.tokenCount) { markerCount }
    buildCompositeAndTokenIndices(
        productionMarkers, tokens, fileDoneIndex, markerCount,
        parentStartIndex, doneForStart, compositeTypes, errorFlags, compositeStartOffsets, compositeEndOffsets,
        tokenParentStart,
    )

    @Suppress("UNCHECKED_CAST")
    val childrenByIndex = arrayOfNulls<List<LightNode>>(markerCount + 1) as Array<List<LightNode>>
    val emptyChildren: List<LightNode> = emptyList()
    for (idx in 0..markerCount) {
        childrenByIndex[idx] = emptyChildren
    }
    buildChildrenIndex(
        productionMarkers, tokens, fileDoneIndex, markerCount,
        doneForStart, errorFlags, childrenByIndex, emptyChildren, isComment
    )

    val rootNodeType = productionMarkers.getMarker(fileDoneIndex).getNodeType()

    return LightSyntaxTree(
        tokens = tokens,
        source = source,
        parentStartIndex = parentStartIndex,
        tokenParentStart = tokenParentStart,
        rootIndex = markerCount,
        rootNodeType = rootNodeType,
        compositeTypes = compositeTypes,
        childrenByIndex = childrenByIndex,
        compositeEndOffsets = compositeEndOffsets,
        compositeStartOffsets = compositeStartOffsets,
        buildLanguageSpecificTreeStructure = buildLanguageSpecificTreeStructure,
    )
}

/**
 * Pass 1 of [buildLanguageSpecificLightTree]. Walks the production markers once, using a single
 * stack of pending START-marker indices to:
 * - determine each composite's parent and populate [doneForStart],
 * - extract node type / error flag / start / end offsets,
 * - assign each token to its innermost enclosing composite in [tokenParentStart].
 */
private fun buildCompositeAndTokenIndices(
    productionMarkers: ProductionMarkerList,
    tokens: TokenList,
    fileDoneIndex: Int,
    rootIndex: Int,
    parentStartIndex: IntArray,
    doneForStart: IntArray,
    compositeTypes: Array<SyntaxElementType?>,
    errorFlags: BooleanArray,
    compositeStartOffsets: IntArray,
    compositeEndOffsets: IntArray,
    tokenParentStart: IntArray,
) {
    var openStack = IntArray(64)
    var stackSize = 0
    fun push(v: Int) {
        if (stackSize >= openStack.size) {
            val grown = IntArray(openStack.size * 2)
            openStack.copyInto(grown)
            openStack = grown
        }
        openStack[stackSize++] = v
    }

    fun pop(): Int = openStack[--stackSize]
    fun peekOrRoot(): Int = if (stackSize == 0) rootIndex else openStack[stackSize - 1]

    var prevTokenIndex = 0
    fun assignTokens(upToExclusive: Int) {
        val parent = peekOrRoot()
        for (t in prevTokenIndex until upToExclusive) {
            tokens.getTokenType(t) ?: continue
            val s = tokens.getTokenStart(t)
            val e = tokens.getTokenEnd(t)
            if (s == e) continue
            tokenParentStart[t] = parent
        }
        prevTokenIndex = upToExclusive
    }

    for (i in 1 until fileDoneIndex) {
        val marker = productionMarkers.getMarker(i)
        when {
            productionMarkers.isDoneMarker(i) -> {
                assignTokens(marker.getEndTokenIndex())
                val startIdx = pop()
                doneForStart[startIdx] = i
                compositeEndOffsets[startIdx] = marker.getEndOffset()
            }
            marker.isErrorMarker() -> {
                assignTokens(marker.getStartTokenIndex())
                parentStartIndex[i] = peekOrRoot()
                compositeTypes[i] = marker.getNodeType()
                errorFlags[i] = true
                compositeStartOffsets[i] = marker.getStartOffset()
                compositeEndOffsets[i] = marker.getEndOffset()
            }
            else -> {
                // START marker
                assignTokens(marker.getStartTokenIndex())
                parentStartIndex[i] = peekOrRoot()
                compositeTypes[i] = marker.getNodeType()
                compositeStartOffsets[i] = marker.getStartOffset()
                push(i)
            }
        }
    }
    assignTokens(tokens.tokenCount)

    require(stackSize == 0) { "Unbalanced production markers: $stackSize unmatched START markers remain" }
}

/**
 * Pass 2 of [buildLanguageSpecificLightTree]: precompute children for each composite and the synthetic root.
 * Drops whitespace and bad-character tokens everywhere; drops comments only under the synthetic
 * root so declaration `DOC_COMMENT` children remain available for `isDeprecatedInJavaDoc`.
 */
private fun buildChildrenIndex(
    productionMarkers: ProductionMarkerList,
    tokens: TokenList,
    fileDoneIndex: Int,
    rootIndex: Int,
    doneForStart: IntArray,
    errorFlags: BooleanArray,
    childrenByIndex: Array<List<LightNode>>,
    emptyChildren: List<LightNode>,
    isComment: (SyntaxElementType) -> Boolean,
) {
    fun isIncludedToken(t: Int, isRoot: Boolean): Boolean {
        val type = tokens.getTokenType(t) ?: return false
        if (tokens.getTokenStart(t) == tokens.getTokenEnd(t)) return false
        if (type === SyntaxTokenTypes.WHITE_SPACE) return false
        if (type === SyntaxTokenTypes.BAD_CHARACTER) return false
        if (isRoot && isComment(type)) return false
        return true
    }

    fun addTokensInRange(childIndices: ArrayList<Int>, from: Int, to: Int, isRoot: Boolean) {
        for (t in from until to) {
            if (isIncludedToken(t, isRoot)) childIndices.add(-(t + 1))
        }
    }

    fun buildChildrenFor(startIdx: Int, doneIdx: Int, firstTokenIndex: Int, lastTokenIndex: Int, isRoot: Boolean = false) {
        val childIndices = ArrayList<Int>(8)

        var prevTokenIndex = firstTokenIndex
        var i = startIdx + 1
        while (i < doneIdx) {
            if (errorFlags[i]) {
                val errTokenStart = productionMarkers.getMarker(i).getStartTokenIndex()
                addTokensInRange(childIndices, prevTokenIndex, errTokenStart, isRoot)
                childIndices.add(i)
                prevTokenIndex = errTokenStart
                i++
            } else if (!productionMarkers.isDoneMarker(i)) {
                val childStartToken = productionMarkers.getMarker(i).getStartTokenIndex()
                addTokensInRange(childIndices, prevTokenIndex, childStartToken, isRoot)
                childIndices.add(i)
                val childDone = doneForStart[i]
                prevTokenIndex = productionMarkers.getMarker(childDone).getEndTokenIndex()
                i = childDone + 1
            } else {
                i++
            }
        }
        addTokensInRange(childIndices, prevTokenIndex, lastTokenIndex, isRoot)

        val slot = if (isRoot) rootIndex else startIdx
        childrenByIndex[slot] = if (childIndices.isEmpty()) emptyChildren
        else ChildrenList(childIndices.toIntArray())
    }

    // Build children for each non-error, non-DONE composite.
    for (i in 1 until fileDoneIndex) {
        if (productionMarkers.isDoneMarker(i) || errorFlags[i]) continue
        val doneIdx = doneForStart[i]
        val firstToken = productionMarkers.getMarker(i).getStartTokenIndex()
        val lastToken = productionMarkers.getMarker(doneIdx).getEndTokenIndex()
        buildChildrenFor(i, doneIdx, firstToken, lastToken)
    }

    // The `JAVA_FILE` children become the root's ones (routed to childrenByIndex[rootIndex]).
    buildChildrenFor(
        startIdx = 0, doneIdx = fileDoneIndex, firstTokenIndex = 0, lastTokenIndex = tokens.tokenCount, isRoot = true,
    )
}

/**
 * Convenience: pretty-prints the subtree rooted at [node] for debugging. Each line prints the
 * node type and (newline-escaped) text, indented by depth.
 */
@TestOnly
fun LightSyntaxTree.dump(node: LightNode = getRoot(), indent: String = ""): String {
    val sb = StringBuilder()
    sb.append(indent).append(getType(node)).append(": ").append(getText(node).toString().replace("\n", "\\n")).append("\n")
    for (child in getChildren(node)) {
        sb.append(dump(child, "$indent  "))
    }
    return sb.toString()
}
