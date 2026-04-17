/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder

import com.intellij.platform.syntax.parser.prepareProduction
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter

class JavaSyntaxNode(
    val type: SyntaxElementType,
    val children: List<JavaSyntaxNode>,
    val source: CharSequence,
    val startOffset: Int,
    val endOffset: Int,
    var parent: JavaSyntaxNode? = null,
) {
    // Step 3.6 memory reduction: manual @Volatile fields replace `by lazy(PUBLICATION)`.
    // Kotlin's lazy delegates allocate a wrapper object (`SafePublicationLazyImpl`) per property
    // per instance — ~32 bytes each on a typical 64-bit JVM with compressed oops, including the
    // captured initialiser lambda. For a project with millions of `JavaSyntaxNode`s (one per
    // token + one per composite marker), two lazy delegates per node would account for a
    // hundreds-of-MB fixed overhead that is overwhelmingly paid for nodes that are never queried
    // (token leaves never have their `text` materialised; the `childByTypeIndex` is only useful
    // for a handful of composite nodes with many children).
    //
    // The manual scheme keeps identical safe-publication semantics — `@Volatile` ensures the
    // happens-before edge required by the JMM — and reduces per-node overhead to a single
    // reference slot for [cachedText] (populated on first [text] read only) and the three-state
    // [cachedChildByTypeIndex] slot (null = not computed; [BELOW_THRESHOLD_SENTINEL] = small node,
    // fall back to linear scan; otherwise the built map).

    @Volatile private var cachedText: String? = null

    val text: String
        get() {
            cachedText?.let { return it }
            val computed = source.subSequence(startOffset, endOffset).toString()
            // Benign race: multiple threads may compute the same string; the write is atomic
            // and all observers see an equal (and equals-equivalent) value.
            cachedText = computed
            return computed
        }

    /**
     * Equivalent to `text == expected`, but without materialising the [text] `String`.
     * Prefer this for identifier/keyword comparisons on hot paths: the vast majority of
     * [JavaSyntaxNode]s never need their full `text` — creating one only to compare it
     * against a short literal wastes both an allocation and an `O(length)` copy.
     */
    fun textEquals(expected: String): Boolean {
        val length = endOffset - startOffset
        if (length != expected.length) return false
        for (i in 0 until length) {
            if (source[startOffset + i] != expected[i]) return false
        }
        return true
    }

    /**
     * Type-indexed view over [children], used by [findChildByType] and [getChildrenByType]
     * (the `SyntaxElementType` overloads) to avoid repeated O(n) linear scans on hot nodes
     * (class / method bodies, modifier lists, …), where several different child types are queried
     * sequentially — yielding O(n²) behaviour overall.
     *
     * Built only when [children] is larger than [CHILD_INDEX_THRESHOLD]; for small lists a linear
     * scan is both faster and cheaper than building and retaining a [HashMap].
     *
     * Tri-state:
     * - `null` field — not yet computed.
     * - [BELOW_THRESHOLD_SENTINEL] — small-children node, caller must fall back to linear scan.
     * - otherwise — the real type → children map.
     */
    @Volatile private var cachedChildByTypeIndex: Map<SyntaxElementType, List<JavaSyntaxNode>>? = null

    internal val childByTypeIndex: Map<SyntaxElementType, List<JavaSyntaxNode>>?
        get() {
            cachedChildByTypeIndex?.let { return if (it === BELOW_THRESHOLD_SENTINEL) null else it }
            val computed: Map<SyntaxElementType, List<JavaSyntaxNode>> =
                if (children.size <= CHILD_INDEX_THRESHOLD) BELOW_THRESHOLD_SENTINEL else children.groupBy { it.type }
            cachedChildByTypeIndex = computed
            return if (computed === BELOW_THRESHOLD_SENTINEL) null else computed
        }

    fun dump(indent: String = ""): String {
        val sb = StringBuilder()
        sb.append(indent).append(type).append(": ").append(text.replace("\n", "\\n")).append("\n")
        for (child in children) {
            sb.append(child.dump(indent + "  "))
        }
        return sb.toString()
    }
}

fun buildSyntaxTree(builder: SyntaxTreeBuilder, source: CharSequence): JavaSyntaxNode {
    val productionMarkers = prepareProduction(builder).productionMarkers
    val tokens = builder.tokens
    val childrenStack = mutableListOf<MutableList<JavaSyntaxNode>>()
    childrenStack.add(mutableListOf())

    var prevTokenIndex = 0

    fun MutableList<JavaSyntaxNode>.appendTokens(lastTokenIndex: Int) {
        for (i in prevTokenIndex until lastTokenIndex) {
            val tokenType = tokens.getTokenType(i) ?: continue
            val start = tokens.getTokenStart(i)
            val end = tokens.getTokenEnd(i)
            if (start == end) continue
            add(JavaSyntaxNode(tokenType, emptyList(), source, start, end))
        }
        prevTokenIndex = lastTokenIndex
    }

    for (i in 0 until productionMarkers.size) {
        val production = productionMarkers.getMarker(i)
        when {
            productionMarkers.isDoneMarker(i) -> {
                val lastChildren = childrenStack.removeAt(childrenStack.size - 1)
                lastChildren.appendTokens(production.getEndTokenIndex())
                val node =
                    JavaSyntaxNode(production.getNodeType(), lastChildren, source, production.getStartOffset(), production.getEndOffset())
                for (child in lastChildren) {
                    child.parent = node
                }
                childrenStack.last().add(node)
            }
            production.isErrorMarker() -> {
                // Error markers don't have start/done pairs, handle them as leaf nodes
                val errorTokenIndex = production.getStartTokenIndex()
                childrenStack.peek().appendTokens(errorTokenIndex)
                val errorNode =
                    JavaSyntaxNode(production.getNodeType(), emptyList(), source, production.getStartOffset(), production.getEndOffset())
                childrenStack.peek().add(errorNode)
            }
            else -> {
                // Start marker
                childrenStack.peek().appendTokens(production.getStartTokenIndex())
                childrenStack.add(mutableListOf())
            }
        }
    }

    val rootNodeType = productionMarkers.getMarker(productionMarkers.size - 1).getNodeType()
    val rootChildren = childrenStack.single()
    val root = JavaSyntaxNode(rootNodeType, rootChildren, source, 0, source.length)
    for (child in rootChildren) {
        child.parent = root
    }
    return root
}

private fun <T> MutableList<T>.peek(): T = last()

fun JavaSyntaxNode.findChildByType(typeName: String): JavaSyntaxNode? {
    return children.find { it.type.toString() == typeName }
}

fun JavaSyntaxNode.getChildrenByType(typeName: String): List<JavaSyntaxNode> {
    return children.filter { it.type.toString() == typeName }
}

/**
 * Above this many children we start indexing nodes by type (see [JavaSyntaxNode.childByTypeIndex]).
 * Small values keep memory usage low; tune via profiling if new hot paths appear.
 */
internal const val CHILD_INDEX_THRESHOLD: Int = 4

/**
 * Sentinel stored in [JavaSyntaxNode.cachedChildByTypeIndex] to record "already computed and the
 * node is below [CHILD_INDEX_THRESHOLD] — no index needed". Using a shared immutable empty map as
 * the sentinel avoids a separate "computed?" boolean field and keeps [JavaSyntaxNode]'s per-instance
 * footprint to a single reference slot for this feature.
 */
private val BELOW_THRESHOLD_SENTINEL: Map<SyntaxElementType, List<JavaSyntaxNode>> = emptyMap()

fun JavaSyntaxNode.findChildByType(type: SyntaxElementType): JavaSyntaxNode? {
    childByTypeIndex?.let { return it[type]?.firstOrNull() }
    return children.find { it.type == type }
}

fun JavaSyntaxNode.getChildrenByType(type: SyntaxElementType): List<JavaSyntaxNode> {
    childByTypeIndex?.let { return it[type] ?: emptyList() }
    return children.filter { it.type == type }
}

internal fun computeTypeParameters(
    node: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
): List<JavaTypeParameter> {
    val typeParamNodes = node.findChildByType(JavaSyntaxElementType.TYPE_PARAMETER_LIST)
        ?.getChildrenByType(JavaSyntaxElementType.TYPE_PARAMETER)
        ?: return emptyList()

    // Create type parameter instances first
    val typeParams = typeParamNodes.map { JavaTypeParameterOverAst(it, resolutionContext) }

    // Create a resolution context with ALL type parameters in scope.
    // This is needed for resolving bounds like `<E, S extends List<E>>`.
    val contextWithTypeParams = resolutionContext.withTypeParameters(typeParams)

    // Update each type parameter to use the enriched context for bounds resolution
    typeParams.forEach { it.updateResolutionContext(contextWithTypeParams) }

    return typeParams
}

