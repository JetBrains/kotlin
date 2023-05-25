/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

/**
 * Sorts [nodes] topologically collecting direct edges via [dependencies]. [nodes] and [dependencies] must form a directed, acyclic graph.
 * [topologicalSort] will throw an [IllegalStateException] if it encounters a cycle.
 *
 * The algorithm is based on depth-first search, starting in order from each node in [nodes]. Kahn's algorithm is harder to apply to the
 * ad-hoc dependency structure because it's not easily apparent whether a node has no other incoming edges.
 *
 * For example, consider the following structure: `C -> A, C -> B, B -> A`. The resulting order should be `[C, B, A]`. However, `A` is
 * first in the list of dependencies of `C`. Without a way to find the incoming edge from `B` to `A` while processing `C -> A`, a naive
 * implementation of Kahn's algorithm might order `A` before `B`.
 */
fun <A> topologicalSort(
    nodes: Iterable<A>,
    reportCycle: (A) -> Nothing = { throw IllegalStateException("Cannot compute a topological sort: The node $it is in a cycle.") },
    dependencies: A.() -> Iterable<A>,
): List<A> {
    val visiting = mutableSetOf<A>()
    val visited = mutableSetOf<A>()

    fun visit(node: A) {
        if (node in visited) return
        if (node in visiting) reportCycle(node)

        // Keeping track of the nodes that are being visited allows the algorithm to throw an exception in case of a cycle. The input should
        // never be cyclic, but this approach gives some additional safety in case of bugs.
        visiting.add(node)
        node.dependencies().forEach(::visit)
        visiting.remove(node)

        visited.add(node)
    }

    nodes.forEach(::visit)

    // The paper algorithm inserts nodes at the head of the result list. Because our `visited` set remembers elements in their order of
    // insertion, the result needs to be reversed.
    return visited.toMutableList().apply { reverse() }
}
