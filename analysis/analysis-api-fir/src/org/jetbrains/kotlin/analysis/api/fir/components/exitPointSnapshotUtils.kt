/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.util.SmartList
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.isImplicitWhenSubjectVariable
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import java.util.*
import kotlin.jvm.optionals.getOrNull

internal class ExitPointSnapshotPathSearcher(statements: Collection<FirElement>) : FirDefaultVisitorVoid() {
    private companion object {
        val FORBIDDEN_FAKE_SOURCE_KINDS: Set<KtFakeSourceElementKind> = setOf(
            KtFakeSourceElementKind.WhenCondition,
            KtFakeSourceElementKind.SingleExpressionBlock
        )
    }

    private val statements = statements.toHashSet()

    private val mapping = HashMap<FirElement, Optional<FirElement>>()
    private var unmappedCount = statements.size

    private val stack = ArrayDeque<FirElement>()

    operator fun get(fir: FirElement): FirElement? {
        return mapping[fir]?.getOrNull()
    }

    override fun visitElement(element: FirElement) {
        withElement(element) {
            if (element in statements) {
                // The leaf is in mapping, but its value is still 'null'
                mapping.computeIfAbsent(element) { _ ->
                    unmappedCount -= 1
                    // Here we intentionally use the 'Optional' wrapper to make 'computeIfAbsent' work smoothly
                    Optional.ofNullable(computeTarget(element))
                }
            }

            if (unmappedCount > 0) {
                element.acceptChildren(this)
            }
        }
    }

    private fun computeTarget(element: FirElement): FirElement? {
        val psi = element.psi
        return stack.firstOrNull { it.psi == psi && isAppropriateTarget(it) }
    }

    private fun isAppropriateTarget(element: FirElement): Boolean {
        if (element !is FirStatement && element !is FirReference) {
            return false
        }

        val source = element.source
        val isImplicitWhenSubjectVariable = element is FirProperty && element.isImplicitWhenSubjectVariable
        return source?.kind !in FORBIDDEN_FAKE_SOURCE_KINDS && !isImplicitWhenSubjectVariable
    }

    private inline fun withElement(element: FirElement, block: () -> Unit) {
        stack.addLast(element)
        try {
            block()
        } finally {
            stack.removeLast()
        }
    }
}

internal class ExitPointSnapshotGraphIndex(graphProvider: () -> ControlFlowGraph) {
    private val mapping: Map<FirElement, List<CFGNode<*>>> by lazy {
        val result = HashMap<FirElement, MutableList<CFGNode<*>>>()

        fun addGraph(graph: ControlFlowGraph) {
            for (node in graph.nodes) {
                result.getOrPut(node.fir) { SmartList() }.add(node)

                if (node is CFGNodeWithSubgraphs<*>) {
                    node.subGraphs.forEach(::addGraph)
                }
            }
        }

        addGraph(graphProvider())

        return@lazy result
    }

    /**
     * Find the last node in a graph (or its subgraphs) that point to the given [fir] element.
     */
    fun findLast(fir: FirElement): CFGNode<*>? {
        val directNodes = mapping[fir]
        if (directNodes != null) {
            return directNodes.last()
        }

        if (fir is FirBlock) {
            return fir.statements
                .asReversed()
                .firstNotNullOfOrNull(::findLast)
        }

        return null
    }
}

internal fun ExitPointSnapshotGraphIndex.hasMultipleExitPoints(firTargets: Set<FirElement>): Boolean {
    if (firTargets.size < 2) {
        return false
    }

    val exitPoints = firTargets
        .mapNotNull { findLast(it) }
        .flatMap { node ->
            node.followingNodes
                .filter { it !is StubNode }
                .map { it.unwrap() }
                .distinct()
                .sortedBy { it.id }
        }.distinct()

    return exitPoints.size > 1
}

private fun CFGNode<*>.unwrap(): CFGNode<*> {
    var current = this

    while (current.isExitNode()) {
        val following = current.followingNodes
        if (following.size == 1) {
            current = following.first()
        } else {
            break
        }
    }

    return current
}

private fun CFGNode<*>.isExitNode(): Boolean {
    return when (this) {
        is ExitNodeMarker, is ExitValueParameterNode, is WhenSubjectExpressionExitNode, is AnonymousObjectExpressionExitNode,
        is SmartCastExpressionExitNode, is PostponedLambdaExitNode, is DelegateExpressionExitNode, is WhenBranchResultExitNode,
        is ElvisExitNode, is ExitSafeCallNode, is LocalClassExitNode, is ElvisLhsExitNode,
            -> {
            true
        }
        else -> {
            false
        }
    }
}
