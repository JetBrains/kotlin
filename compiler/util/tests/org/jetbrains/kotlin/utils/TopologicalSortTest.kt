/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import junit.framework.TestCase
import kotlin.test.assertFails

class TopologicalSortTest : TestCase() {
    fun testEmptyGraph() {
        assertEquals("", emptyList<Any>(), topologicalSort(emptyList<Any>()) { emptyList<Any>() })
    }

    fun testSingleNode() {
        checkGraph(
            """
            A
            
        """.trimIndent(),
            listOf("A")
        )
    }

    fun testSimpleGraph() {
        checkGraph(
            """
            A; B; C
            A > B; B > C; A > C
        """.trimIndent(),
            listOf("A", "B", "C")
        )
    }

    fun testSimpleGraphShuffledNodes() {
        checkGraph(
            """
            C; A; B
            A > B; B > C; A > C
        """.trimIndent(),
            listOf("A", "B", "C")
        )
    }

    fun testBamboo() {
        checkGraph(
            """
            A; B; C
            C > B; B > A
        """.trimIndent(),
            listOf("C", "B", "A")
        )
    }

    fun testLongerPath() {
        checkGraph(
            """
            A; B; C; D; E
            E > D; E > B; D > C; C > B; C > A; B > A
        """.trimIndent(),
            listOf("E", "D", "C", "B", "A")
        )
    }

    fun testRepeatedEdges() {
        checkGraph(
            """
            A; B; C
            C > B; C > B; B > A; C > A
        """.trimIndent(),
            listOf("C", "B", "A")
        )
    }

    fun testSelfLoopReport() {
        val graph = parseFromString(
            """
            A
            A > A
        """.trimIndent()
        )
        assertFails { topologicalSort(graph.nodes, dependencies = { graph.edges[this].orEmpty() }) }
    }

    fun testDirectLoopReport() {
        val graph = parseFromString(
            """
            A; B
            A > B; B > A
        """.trimIndent()
        )
        assertFails { topologicalSort(graph.nodes, dependencies = { graph.edges[this].orEmpty() }) }
    }

    fun testLongLoopReport() {
        val graph = parseFromString(
            """
            A; B; C; D
            D > C; C > B; C > A; A > D
        """.trimIndent()
        )
        assertFails { topologicalSort(graph.nodes, dependencies = { graph.edges[this].orEmpty() }) }
    }

    private fun checkGraph(description: String, expected: List<String>) {
        val graph = parseFromString(description)
        assertEquals("Incorrect order of sorted nodes", expected, sortedNodes(graph))
    }

    private fun <T> sortedNodes(graph: Graph<T>): List<T> {
        return topologicalSort(graph.nodes) { graph.edges[this].orEmpty() }
    }
}

private class Graph<T>(
    val nodes: Set<T>,
    val edges: Map<T, List<T>>
)

/**
 * Expected format: line with the list of nodes, line with the list of edges.
 * [node[;...]]
 * [node > node[;...]]
 */
private fun parseFromString(description: String): Graph<String> {
    val (nodeStrings, edgeStrings) = description.lines()
    val nodes = nodeStrings.split(";").map(String::trim).toSet()
    val edges = buildMap<String, MutableList<String>> {
        edgeStrings.takeIf { it.isNotBlank() }?.split(";")?.forEach { edgeDescription ->
            val (from, to) = edgeDescription.split(">").map(String::trim)
            getOrPut(from) { mutableListOf() }.add(to)
        }
    }

    return Graph(nodes, edges)
}
