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

package org.jetbrains.kotlin.backend.konan

internal interface DirectedGraphNode<out K> {
    val key: K
    val directEdges: List<K>?
    val reversedEdges: List<K>?
}

internal interface DirectedGraph<K, out N: DirectedGraphNode<K>> {
    val nodes: Collection<N>
    fun get(key: K): N
}

internal class DirectedGraphMultiNode<out K>(val nodes: Set<K>)

internal class DirectedGraphCondensation<out K>(val topologicalOrder: List<DirectedGraphMultiNode<K>>)

internal class DirectedGraphCondensationBuilder<K, out N: DirectedGraphNode<K>>(private val graph: DirectedGraph<K, N>) {
    private val visited = mutableSetOf<K>()
    private val order = mutableListOf<N>()
    private val nodeToMultiNodeMap = mutableMapOf<N, DirectedGraphMultiNode<K>>()
    private val multiNodesOrder = mutableListOf<DirectedGraphMultiNode<K>>()

    fun build(): DirectedGraphCondensation<K> {
        // First phase.
        graph.nodes.forEach {
            if (!visited.contains(it.key))
                findOrder(it)
        }

        // Second phase.
        visited.clear()
        val multiNodes = mutableListOf<DirectedGraphMultiNode<K>>()
        order.reversed().forEach {
            if (!visited.contains(it.key)) {
                val nodes = mutableSetOf<K>()
                paint(it, nodes)
                multiNodes += DirectedGraphMultiNode(nodes)
            }
        }

        // Topsort of built condensation.
        multiNodes.forEach { multiNode ->
            multiNode.nodes.forEach { nodeToMultiNodeMap.put(graph.get(it), multiNode) }
        }
        visited.clear()
        multiNodes.forEach {
            if (!visited.contains(it.nodes.first()))
                findMultiNodesOrder(it)
        }

        return DirectedGraphCondensation(multiNodesOrder.reversed())
    }

    private fun findOrder(node: N) {
        visited += node.key
        node.directEdges?.forEach {
            if (!visited.contains(it))
                findOrder(graph.get(it))
        }
        order += node
    }

    private fun paint(node: N, multiNode: MutableSet<K>) {
        visited += node.key
        multiNode += node.key
        node.reversedEdges?.forEach {
            if (!visited.contains(it))
                paint(graph.get(it), multiNode)
        }
    }

    private fun findMultiNodesOrder(node: DirectedGraphMultiNode<K>) {
        visited.addAll(node.nodes)
        node.nodes.forEach {
            graph.get(it).directEdges?.forEach {
                if (!visited.contains(it))
                    findMultiNodesOrder(nodeToMultiNodeMap[graph.get(it)]!!)
            }
        }
        multiNodesOrder += node
    }
}
