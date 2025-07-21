/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push

class StronglyConnectedComponents<T>(val enumerateOutgoingEdges: (T) -> Sequence<T>) {

    private val visited = mutableSetOf<T>()
    private val stack = mutableListOf<T>()
    private val reversedGraph = mutableMapOf<T, MutableSet<T>>()

    fun visit(edge: T) {
        if (visited.add(edge)) {
            for (outgoingEdge in enumerateOutgoingEdges(edge)) {
                (reversedGraph.getOrPut(outgoingEdge) { mutableSetOf() }).add(edge)
                visit(outgoingEdge)
            }
            stack.push(edge)
        }
    }

    fun findComponents(): MutableList<MutableList<T>> {
        visited.clear()
        val result = mutableListOf<MutableList<T>>()
        while (stack.isNotEmpty()) {
            val edge = stack.pop()
            if (visited.add(edge)) {
                val component = mutableListOf<T>()
                visitTransposedEdge(edge, component)
                result.add(component)
            }
        }
        result.reverse()
        return result
    }

    private fun visitTransposedEdge(edge: T, component: MutableList<T>) {
        component.add(edge)
        val outgoingEdges = reversedGraph[edge]
        if (outgoingEdges != null) {
            for (outgoingEdge in outgoingEdges) {
                if (visited.add(outgoingEdge)) {
                    visitTransposedEdge(outgoingEdge, component)
                }
            }
        }
    }
}