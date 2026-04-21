/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import com.intellij.platform.syntax.lexer.TokenList
import com.intellij.platform.syntax.parser.ProductionMarkerList
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import com.intellij.platform.syntax.parser.prepareProduction

/**
 * Identifier for a node within a [JavaLightTree], encoded as a single [Int].
 *
 * Encoding:
 *  - Non-negative values [0 .. markerCount-1]: composite (START) marker index.
 *  - `markerCount` (i.e. [JavaLightTree.rootIndex]): the synthetic file root that wraps all
 *    top-level productions.
 *  - Negative values: token index encoded as `-(tokenIndex + 1)`. So `-1` is token at index 0.
 *  - `Int.MIN_VALUE`: invalid / "not computed" sentinel ([JavaLightTree.NO_NODE]).
 *
 * Two value classes are equal iff their [index] values are equal — equality alone does not
 * distinguish nodes from different trees. Higher-level abstractions (e.g. [JavaElementOverAst])
 * compare both the node and the owning tree.
 */
@JvmInline
value class JavaLightNode(val index: Int)

/**
 * Flat-array AST representation produced from a [SyntaxTreeBuilder]. Holds the raw
 * [ProductionMarkerList] / [TokenList] plus precomputed lookup arrays that permit O(1)
 * access to parent, children, and node type — matching the access cost of the old
 * materialised `JavaSyntaxNode` tree while using ~97% less memory.
 *
 * Children and composite-node types are precomputed during construction (in
 * [buildJavaLightTree]) so that [getChildren], [getType], and [findChildByType] are
 * plain array lookups with no hashing, no volatile reads, and no marker-pool dispatch.
 *
 * One [JavaLightTree] instance per parsed file. All [JavaLightNode]s used with this tree must
 * have been produced by it; mixing nodes between trees is an error (no runtime check).
 */
class JavaLightTree(
    val productionMarkers: ProductionMarkerList,
    val tokens: TokenList,
    val source: CharSequence,
    /** For each START-marker index, the START-marker index of its parent (or [rootIndex] for top-level markers). */
    private val parentStartIndex: IntArray,
    /** For each marker index that is a START, the index of its matching DONE marker. */
    private val doneForStart: IntArray,
    /** For each token index, the START-marker index of its enclosing composite, or [rootIndex] for top-level tokens. */
    private val tokenParentStart: IntArray,
    /** Index used by [getRoot]: synthetic root wraps all top-level production markers. */
    val rootIndex: Int,
    /** [SyntaxElementType] used for the synthetic root node. */
    private val rootNodeType: SyntaxElementType,
    /**
     * Precomputed [SyntaxElementType] for each composite marker, indexed by START-marker index.
     * Entries for DONE markers and error markers are also populated during construction.
     * Access: `compositeTypes[startMarkerIndex]` — O(1), no [ProductionMarkerList.getMarker] call.
     */
    private val compositeTypes: Array<SyntaxElementType?>,
    /**
     * Precomputed children for each composite node, indexed by START-marker index.
     * Entry at [rootIndex] holds the synthetic root's children.
     * Each entry is a [ChildrenList] backed by an [IntArray] of child node indices — no boxing
     * on iteration because [ChildrenList.get] creates a [JavaLightNode] value class inline.
     * Error markers and tokens map to [emptyList].
     */
    private val childrenByIndex: Array<List<JavaLightNode>>,
    /**
     * Per-marker error flag. `true` at index `i` means marker `i` is an error marker
     * (leaf, no done-pair). Avoids [ProductionMarkerList.getMarker] calls in hot paths.
     */
    private val errorFlags: BooleanArray,
    /**
     * Precomputed end offsets for composite nodes, indexed by START-marker index.
     * For normal composites this is the DONE marker's end offset; for error markers
     * it is the marker's own end offset. Avoids [ProductionMarkerList.getMarker] calls
     * in [getEndOffset].
     */
    private val compositeEndOffsets: IntArray,
    /**
     * Precomputed start offsets for composite nodes, indexed by START-marker index.
     * Avoids [ProductionMarkerList.getMarker] calls in [getStartOffset].
     */
    private val compositeStartOffsets: IntArray,
) {
    /** Synthetic root node — wraps all top-level production markers. */
    fun getRoot(): JavaLightNode = JavaLightNode(rootIndex)

    private fun isSyntheticRoot(node: JavaLightNode): Boolean = node.index == rootIndex

    fun isComposite(node: JavaLightNode): Boolean = node.index >= 0 && node.index <= rootIndex
    fun isToken(node: JavaLightNode): Boolean = node.index < 0 && node.index != Int.MIN_VALUE

    fun getType(node: JavaLightNode): SyntaxElementType {
        val idx = node.index
        if (idx >= 0) {
            // Composite (including synthetic root at idx == rootIndex)
            return if (idx == rootIndex) rootNodeType else compositeTypes[idx]!!
        }
        // Token
        val tokenIdx = -(idx + 1)
        return tokens.getTokenType(tokenIdx) ?: error("No token type at index $tokenIdx")
    }

    fun getStartOffset(node: JavaLightNode): Int {
        val idx = node.index
        if (idx >= 0) return if (idx == rootIndex) 0 else compositeStartOffsets[idx]
        val tokenIdx = -(idx + 1)
        return tokens.getTokenStart(tokenIdx)
    }

    fun getEndOffset(node: JavaLightNode): Int {
        val idx = node.index
        if (idx >= 0) return if (idx == rootIndex) source.length else compositeEndOffsets[idx]
        val tokenIdx = -(idx + 1)
        return tokens.getTokenEnd(tokenIdx)
    }

    /** Returns a view over [source] without copying — substring materialisation is the caller's choice. */
    fun getText(node: JavaLightNode): CharSequence = source.subSequence(getStartOffset(node), getEndOffset(node))

    /** O(length) char-by-char comparison; avoids the [String] allocation that [getText]+[CharSequence.toString] performs. */
    fun textEquals(node: JavaLightNode, expected: String): Boolean {
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
     *
     * Composite (start-marker) nodes use the precomputed [parentStartIndex]. Token nodes use
     * the precomputed [tokenParentStart]. Both lookups are O(1).
     */
    fun getParent(node: JavaLightNode): JavaLightNode? {
        if (node.index == Int.MIN_VALUE) return null
        if (isSyntheticRoot(node)) return null
        return if (isComposite(node)) {
            JavaLightNode(parentStartIndex[node.index])
        } else {
            val tokenIdx = -(node.index + 1)
            JavaLightNode(tokenParentStart[tokenIdx])
        }
    }

    /**
     * Returns the immediate children of [node] in source order.
     *
     * Children are precomputed during construction — this is a direct array lookup, O(1).
     * For tokens, returns an empty list (tokens are leaves).
     */
    fun getChildren(node: JavaLightNode): List<JavaLightNode> {
        val idx = node.index
        if (idx < 0) return emptyList()
        return childrenByIndex[idx]
    }

    fun findChildByType(node: JavaLightNode, type: SyntaxElementType): JavaLightNode? {
        val children = getChildren(node)
        for (i in children.indices) {
            val child = children[i]
            if (getType(child) == type) return child
        }
        return null
    }

    fun getChildrenByType(node: JavaLightNode, type: SyntaxElementType): List<JavaLightNode> {
        val children = getChildren(node)
        if (children.isEmpty()) return emptyList()
        val result = ArrayList<JavaLightNode>(4)
        for (i in children.indices) {
            val child = children[i]
            if (getType(child) == type) result.add(child)
        }
        return result
    }

    fun hasChildOfType(node: JavaLightNode, type: SyntaxElementType): Boolean = findChildByType(node, type) != null

    companion object {
        /** Sentinel "no node" value for use as a not-computed marker in callers. */
        val NO_NODE: JavaLightNode = JavaLightNode(Int.MIN_VALUE)
    }
}

/**
 * Lightweight [List] view over a precomputed [IntArray] of child node indices.
 * [get] creates a [JavaLightNode] value class inline — no boxing when the JIT
 * inlines the loop body.
 */
private class ChildrenList(private val indices: IntArray) : AbstractList<JavaLightNode>() {
    override val size: Int get() = indices.size
    override fun get(index: Int): JavaLightNode = JavaLightNode(indices[index])
}

/**
 * Builds a [JavaLightTree] from a populated [SyntaxTreeBuilder].
 *
 * Performs three passes over the production markers:
 * 1. Parent/done index computation (stack-based).
 * 2. Token-to-parent mapping.
 * 3. Children list construction + type/offset/error-flag extraction.
 *
 * After construction, all accessors ([JavaLightTree.getChildren], [JavaLightTree.getType],
 * [JavaLightTree.getStartOffset], [JavaLightTree.getEndOffset]) are plain array lookups.
 */
fun buildJavaLightTree(builder: SyntaxTreeBuilder, source: CharSequence): JavaLightTree {
    val productionMarkers = prepareProduction(builder).productionMarkers
    val tokens = builder.tokens
    val markerCount = productionMarkers.size
    val rootIndex = markerCount

    // ---- Pass 1: parent/done indices + extract types, offsets, error flags ----
    val parentStartIndex = IntArray(markerCount) { rootIndex }
    val doneForStart = IntArray(markerCount) { -1 }
    val compositeTypes = arrayOfNulls<SyntaxElementType>(markerCount)
    val errorFlags = BooleanArray(markerCount)
    val compositeStartOffsets = IntArray(markerCount)
    val compositeEndOffsets = IntArray(markerCount)

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

    for (i in 0 until markerCount) {
        val marker = productionMarkers.getMarker(i)
        when {
            productionMarkers.isDoneMarker(i) -> {
                val startIdx = pop()
                doneForStart[startIdx] = i
                // Record end offset for the composite (DONE marker's end offset)
                compositeEndOffsets[startIdx] = marker.getEndOffset()
            }
            marker.isErrorMarker() -> {
                parentStartIndex[i] = peekOrRoot()
                compositeTypes[i] = marker.getNodeType()
                errorFlags[i] = true
                compositeStartOffsets[i] = marker.getStartOffset()
                compositeEndOffsets[i] = marker.getEndOffset()
            }
            else -> {
                // START marker
                parentStartIndex[i] = peekOrRoot()
                compositeTypes[i] = marker.getNodeType()
                compositeStartOffsets[i] = marker.getStartOffset()
                push(i)
            }
        }
    }

    require(stackSize == 0) { "Unbalanced production markers: $stackSize unmatched START markers remain" }

    // ---- Pass 2: token-to-parent mapping ----
    val tokenParentStart = IntArray(tokens.tokenCount) { rootIndex }
    run {
        var stack2 = IntArray(64)
        var size2 = 0
        fun push2(v: Int) {
            if (size2 >= stack2.size) {
                val grown = IntArray(stack2.size * 2)
                stack2.copyInto(grown)
                stack2 = grown
            }
            stack2[size2++] = v
        }

        fun pop2() {
            --size2
        }

        fun top2(): Int = if (size2 == 0) rootIndex else stack2[size2 - 1]

        var prevTokenIndex = 0
        fun assignTokens(upToExclusive: Int) {
            val parent = top2()
            for (t in prevTokenIndex until upToExclusive) {
                tokens.getTokenType(t) ?: continue
                val s = tokens.getTokenStart(t)
                val e = tokens.getTokenEnd(t)
                if (s == e) continue
                tokenParentStart[t] = parent
            }
            prevTokenIndex = upToExclusive
        }

        for (i in 0 until markerCount) {
            val marker = productionMarkers.getMarker(i)
            when {
                productionMarkers.isDoneMarker(i) -> {
                    assignTokens(marker.getEndTokenIndex())
                    pop2()
                }
                marker.isErrorMarker() -> {
                    assignTokens(marker.getStartTokenIndex())
                }
                else -> {
                    assignTokens(marker.getStartTokenIndex())
                    push2(i)
                }
            }
        }
        assignTokens(tokens.tokenCount)
    }

    // ---- Pass 3: precompute children for every composite node ----
    // Uses the same algorithm as the old on-demand getChildren, but runs once for all nodes.
    // Whitespace tokens are EXCLUDED from children — they are never matched positively by any
    // caller (always filtered out), so including them only inflates children lists and wastes
    // getType() calls during findChildByType/getChildrenByType scans.
    @Suppress("UNCHECKED_CAST")
    val childrenByIndex = arrayOfNulls<List<JavaLightNode>>(rootIndex + 1) as Array<List<JavaLightNode>>
    val emptyChildren: List<JavaLightNode> = emptyList()
    for (idx in 0..rootIndex) {
        childrenByIndex[idx] = emptyChildren
    }

    /** Returns true if token [t] should be included as a child (non-null type, non-empty, non-whitespace). */
    fun isIncludedToken(t: Int): Boolean {
        val type = tokens.getTokenType(t) ?: return false
        if (tokens.getTokenStart(t) == tokens.getTokenEnd(t)) return false
        if (type === SyntaxTokenTypes.WHITE_SPACE) return false
        return true
    }

    fun addTokensInRange(childIndices: ArrayList<Int>, from: Int, to: Int) {
        for (t in from until to) {
            if (isIncludedToken(t)) childIndices.add(-(t + 1))
        }
    }

    fun buildChildrenFor(startIdx: Int, doneIdx: Int, firstTokenIndex: Int, lastTokenIndex: Int) {
        val childIndices = ArrayList<Int>(8)

        var prevTokenIndex = firstTokenIndex
        var i = startIdx + 1
        while (i < doneIdx) {
            if (errorFlags[i]) {
                val errTokenStart = productionMarkers.getMarker(i).getStartTokenIndex()
                addTokensInRange(childIndices, prevTokenIndex, errTokenStart)
                childIndices.add(i)
                prevTokenIndex = errTokenStart
                i++
            } else if (!productionMarkers.isDoneMarker(i)) {
                val childStartToken = productionMarkers.getMarker(i).getStartTokenIndex()
                addTokensInRange(childIndices, prevTokenIndex, childStartToken)
                childIndices.add(i)
                val childDone = doneForStart[i]
                prevTokenIndex = productionMarkers.getMarker(childDone).getEndTokenIndex()
                i = childDone + 1
            } else {
                i++
            }
        }
        addTokensInRange(childIndices, prevTokenIndex, lastTokenIndex)

        childrenByIndex[startIdx] = if (childIndices.isEmpty()) emptyChildren
        else ChildrenList(childIndices.toIntArray())
    }

    // Build children for each non-error, non-DONE composite
    for (i in 0 until markerCount) {
        if (productionMarkers.isDoneMarker(i) || errorFlags[i]) continue
        val doneIdx = doneForStart[i]
        val firstToken = productionMarkers.getMarker(i).getStartTokenIndex()
        val lastToken = productionMarkers.getMarker(doneIdx).getEndTokenIndex()
        buildChildrenFor(i, doneIdx, firstToken, lastToken)
    }

    // Build children for synthetic root (startIdx=-1, so can't use buildChildrenFor)
    run {
        val childIndices = ArrayList<Int>(8)
        var prevTokenIndex = 0
        var i = 0
        while (i < markerCount) {
            if (errorFlags[i]) {
                val errTokenStart = productionMarkers.getMarker(i).getStartTokenIndex()
                addTokensInRange(childIndices, prevTokenIndex, errTokenStart)
                childIndices.add(i)
                prevTokenIndex = errTokenStart
                i++
            } else if (!productionMarkers.isDoneMarker(i)) {
                val childStartToken = productionMarkers.getMarker(i).getStartTokenIndex()
                addTokensInRange(childIndices, prevTokenIndex, childStartToken)
                childIndices.add(i)
                val childDone = doneForStart[i]
                prevTokenIndex = productionMarkers.getMarker(childDone).getEndTokenIndex()
                i = childDone + 1
            } else {
                i++
            }
        }
        addTokensInRange(childIndices, prevTokenIndex, tokens.tokenCount)
        childrenByIndex[rootIndex] = if (childIndices.isEmpty()) emptyChildren
        else ChildrenList(childIndices.toIntArray())
    }

    val rootNodeType = if (markerCount > 0)
        productionMarkers.getMarker(markerCount - 1).getNodeType()
    else
        error("No production markers")

    val tree = JavaLightTree(
        productionMarkers = productionMarkers,
        tokens = tokens,
        source = source,
        parentStartIndex = parentStartIndex,
        doneForStart = doneForStart,
        tokenParentStart = tokenParentStart,
        rootIndex = rootIndex,
        rootNodeType = rootNodeType,
        compositeTypes = compositeTypes,
        childrenByIndex = childrenByIndex,
        errorFlags = errorFlags,
        compositeEndOffsets = compositeEndOffsets,
        compositeStartOffsets = compositeStartOffsets,
    )
    return tree
}

/**
 * Convenience: pretty-prints the subtree rooted at [node] for debugging. Each line prints the
 * node type and (newline-escaped) text, indented by depth.
 */
fun JavaLightTree.dump(node: JavaLightNode = getRoot(), indent: String = ""): String {
    val sb = StringBuilder()
    sb.append(indent).append(getType(node)).append(": ").append(getText(node).toString().replace("\n", "\\n")).append("\n")
    for (child in getChildren(node)) {
        sb.append(dump(child, "$indent  "))
    }
    return sb.toString()
}
