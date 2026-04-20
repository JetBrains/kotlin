/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.platform.syntax.SyntaxElementType
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
 * Flat-array AST representation produced from a [SyntaxTreeBuilder] without materialising any
 * tree-of-objects. Holds the raw [ProductionMarkerList] / [TokenList] plus precomputed lookup
 * arrays that permit O(1) parent / start↔done navigation.
 *
 * One [JavaLightTree] instance per parsed file. All [JavaLightNode]s used with this tree must
 * have been produced by it; mixing nodes between trees is an error (no runtime check).
 *
 * The tree exposes a synthetic root (index = [rootIndex]) that wraps all top-level production
 * markers as direct children. `getChildren(root)` yields the file's top-level declarations in
 * source order, so model-class consumers can iterate a compilation unit uniformly regardless
 * of how many top-level productions the parser emits.
 */
class JavaLightTree(
    val productionMarkers: ProductionMarkerList,
    val tokens: TokenList,
    val source: CharSequence,
    /** For each START-marker index, the START-marker index of its parent (or [rootIndex] for top-level markers). */
    private val parentStartIndex: IntArray,
    /** For each marker index that is a START, the index of its matching DONE marker. */
    private val doneForStart: IntArray,
    /** For each marker index that is a DONE, the index of its matching START marker. */
    @Suppress("unused") private val startForDone: IntArray,
    /** For each token index, the START-marker index of its enclosing composite, or [rootIndex] for top-level tokens. */
    private val tokenParentStart: IntArray,
    /** Index used by [getRoot]: synthetic root wraps all top-level production markers. */
    val rootIndex: Int,
    /** [SyntaxElementType] used for the synthetic root node — taken from the last production marker's nodeType, matching legacy behaviour. */
    private val rootNodeType: SyntaxElementType,
) {
    /** Synthetic root node — wraps all top-level production markers. */
    fun getRoot(): JavaLightNode = JavaLightNode(rootIndex)

    private fun isSyntheticRoot(node: JavaLightNode): Boolean = node.index == rootIndex

    fun isComposite(node: JavaLightNode): Boolean = node.index >= 0 && node.index <= rootIndex
    fun isToken(node: JavaLightNode): Boolean = node.index < 0 && node.index != Int.MIN_VALUE

    /**
     * True when [node] is a composite index referring to an error marker. Error markers are
     * leaf children in the materialised tree (no sub-children) and have no matching done pair,
     * so they must be treated specially by offset / children lookups.
     */
    private fun isErrorMarker(node: JavaLightNode): Boolean =
        isComposite(node) && !isSyntheticRoot(node) && productionMarkers.getMarker(node.index).isErrorMarker()

    fun getType(node: JavaLightNode): SyntaxElementType {
        if (isSyntheticRoot(node)) return rootNodeType
        return if (isComposite(node)) {
            productionMarkers.getMarker(node.index).getNodeType()
        } else {
            val tokenIdx = -(node.index + 1)
            tokens.getTokenType(tokenIdx) ?: error("No token type at index $tokenIdx")
        }
    }

    fun getStartOffset(node: JavaLightNode): Int {
        if (isSyntheticRoot(node)) return 0
        return if (isComposite(node)) {
            productionMarkers.getMarker(node.index).getStartOffset()
        } else {
            val tokenIdx = -(node.index + 1)
            tokens.getTokenStart(tokenIdx)
        }
    }

    fun getEndOffset(node: JavaLightNode): Int {
        if (isSyntheticRoot(node)) return source.length
        return if (isComposite(node)) {
            val marker = productionMarkers.getMarker(node.index)
            if (marker.isErrorMarker()) {
                // Error markers have no done-marker pair; use the marker's own end offset.
                marker.getEndOffset()
            } else {
                // For START markers, report the END of the corresponding DONE marker — the
                // composite node spans the full range from its START to its DONE.
                val doneIdx = doneForStart[node.index]
                productionMarkers.getMarker(doneIdx).getEndOffset()
            }
        } else {
            val tokenIdx = -(node.index + 1)
            tokens.getTokenEnd(tokenIdx)
        }
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
     * For tokens, returns an empty list (tokens are leaves). For composites, walks the marker
     * list between the START marker and its matching DONE marker, collecting:
     *  - Direct child START markers (whose parent is [node]) — recorded as composite child nodes,
     *    skipping their interior markers via [doneForStart].
     *  - Direct child ERROR markers (whose parent is [node]).
     *  - Tokens in the gaps between child markers (empty tokens such as DANGLING_NEWLINE and
     *    whitespace with `start == end` are skipped).
     *
     * For the synthetic root, walks all top-level markers and the surrounding tokens.
     */
    fun getChildren(node: JavaLightNode): List<JavaLightNode> {
        if (!isComposite(node)) return emptyList()
        // Error markers have no done-pair — they are treated as leaves with no children.
        if (isErrorMarker(node)) return emptyList()
        val firstTokenIndex: Int
        val lastTokenIndex: Int
        val startIdx: Int
        val doneIdx: Int
        if (isSyntheticRoot(node)) {
            startIdx = -1                     // walk from index 0
            doneIdx = productionMarkers.size  // walk to end
            firstTokenIndex = 0
            lastTokenIndex = tokens.tokenCount
        } else {
            startIdx = node.index
            doneIdx = doneForStart[startIdx]
            firstTokenIndex = productionMarkers.getMarker(startIdx).getStartTokenIndex()
            lastTokenIndex = productionMarkers.getMarker(doneIdx).getEndTokenIndex()
        }

        val result = ArrayList<JavaLightNode>(8)

        var prevTokenIndex = firstTokenIndex
        var i = startIdx + 1
        while (i < doneIdx) {
            val marker = productionMarkers.getMarker(i)
            val isDone = productionMarkers.isDoneMarker(i)
            if (marker.isErrorMarker()) {
                appendTokensRange(result, prevTokenIndex, marker.getStartTokenIndex())
                result.add(JavaLightNode(i))
                prevTokenIndex = marker.getStartTokenIndex()
                i++
            } else if (!isDone) {
                appendTokensRange(result, prevTokenIndex, marker.getStartTokenIndex())
                result.add(JavaLightNode(i))
                val childDone = doneForStart[i]
                prevTokenIndex = productionMarkers.getMarker(childDone).getEndTokenIndex()
                i = childDone + 1
            } else {
                // A DONE marker for some START that is not a direct child — skip silently.
                i++
            }
        }
        appendTokensRange(result, prevTokenIndex, lastTokenIndex)
        return result
    }

    private fun appendTokensRange(out: MutableList<JavaLightNode>, fromInclusive: Int, toExclusive: Int) {
        for (t in fromInclusive until toExclusive) {
            tokens.getTokenType(t) ?: continue
            val s = tokens.getTokenStart(t)
            val e = tokens.getTokenEnd(t)
            if (s == e) continue
            out.add(JavaLightNode(-(t + 1)))
        }
    }

    fun findChildByType(node: JavaLightNode, type: SyntaxElementType): JavaLightNode? {
        if (!isComposite(node)) return null
        for (child in getChildren(node)) {
            if (getType(child) == type) return child
        }
        return null
    }

    fun findChildByType(node: JavaLightNode, typeName: String): JavaLightNode? {
        if (!isComposite(node)) return null
        for (child in getChildren(node)) {
            if (getType(child).toString() == typeName) return child
        }
        return null
    }

    fun getChildrenByType(node: JavaLightNode, type: SyntaxElementType): List<JavaLightNode> {
        if (!isComposite(node)) return emptyList()
        val all = getChildren(node)
        val result = ArrayList<JavaLightNode>(4)
        for (child in all) {
            if (getType(child) == type) result.add(child)
        }
        return result
    }

    fun getChildrenByType(node: JavaLightNode, typeName: String): List<JavaLightNode> {
        if (!isComposite(node)) return emptyList()
        val all = getChildren(node)
        val result = ArrayList<JavaLightNode>(4)
        for (child in all) {
            if (getType(child).toString() == typeName) result.add(child)
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
 * Builds a [JavaLightTree] from a populated [SyntaxTreeBuilder].
 *
 * The construction performs a single pass over the production markers using a stack to compute
 * parent indices for both composite markers and tokens, without allocating any per-node objects.
 */
fun buildJavaLightTree(builder: SyntaxTreeBuilder, source: CharSequence): JavaLightTree {
    val productionMarkers = prepareProduction(builder).productionMarkers
    val tokens = builder.tokens
    val markerCount = productionMarkers.size
    val rootIndex = markerCount

    val parentStartIndex = IntArray(markerCount) { rootIndex }
    val doneForStart = IntArray(markerCount) { -1 }
    val startForDone = IntArray(markerCount) { -1 }

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
                startForDone[i] = startIdx
            }
            marker.isErrorMarker() -> {
                parentStartIndex[i] = peekOrRoot()
            }
            else -> {
                parentStartIndex[i] = peekOrRoot()
                push(i)
            }
        }
    }

    require(stackSize == 0) { "Unbalanced production markers: $stackSize unmatched START markers remain" }

    // Token → enclosing-composite mapping. Walks the marker stream with a parent stack so that
    // every emitted token (non-empty range, non-null type) records its enclosing START index
    // (or [rootIndex] for top-level tokens).
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
        // Trailing tokens after the last marker (e.g. comments at end of file) attach to root.
        assignTokens(tokens.tokenCount)
    }

    // The synthetic root carries the nodeType of the last production marker. Callers do not
    // generally read the root's type, but this assignment gives it a deterministic value.
    val rootNodeType = if (markerCount > 0)
        productionMarkers.getMarker(markerCount - 1).getNodeType()
    else
        error("No production markers")

    return JavaLightTree(
        productionMarkers = productionMarkers,
        tokens = tokens,
        source = source,
        parentStartIndex = parentStartIndex,
        doneForStart = doneForStart,
        startForDone = startForDone,
        tokenParentStart = tokenParentStart,
        rootIndex = rootIndex,
        rootNodeType = rootNodeType,
    )
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
