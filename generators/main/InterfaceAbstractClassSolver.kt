/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.util

/*
 * Assume that we have element `E` with parents `P1, P2`
 *
 * Introduce variables E, P1, P2. If variable `X` is true then `X` is class, otherwise it is interface
 *
 * Build 2SAT function for it: (E || !P1) && (E || !P2) && (!P1 || !P2)
 * Simple explanation:
 *   if `P1` is a class then `E` also should be a class
 *   if `P1` is a class then `P2` can not be a class (because both of them a parents of E`
 */

interface Node {
    val parents: List<Node>
    val origin: Node
}

fun solveGraphForClassVsInterface(
    elements: List<Node>, requiredInterfaces: Collection<Node>, requiredClasses: Collection<Node>,
): List<Boolean> {
    val elementMapping = ElementMapping(elements)
    val solution = solve2sat(elements, elementMapping)
    processRequirementsFromConfig(solution, elementMapping, requiredInterfaces, requiredClasses)
    return solution
}

private class ElementMapping(val elements: Collection<Node>) {
    private val varToElements: Map<Int, Node> = elements.mapIndexed { index, element -> 2 * index to element.origin }.toMap() +
            elements.mapIndexed { index, element -> 2 * index + 1 to element }.toMap()
    private val elementsToVar: Map<Node, Int> = elements.mapIndexed { index, element -> element.origin to index }.toMap()

    operator fun get(element: Node): Int = elementsToVar.getValue(element)
    operator fun get(index: Int): Node = varToElements.getValue(index)

    val size: Int = elements.size
}

private fun processRequirementsFromConfig(
    solution: MutableList<Boolean>,
    elementMapping: ElementMapping,
    requiredInterfaces: Collection<Node>,
    requiredClasses: Collection<Node>,
) {
    fun forceParentsToBeInterfaces(element: Node) {
        val origin = element.origin
        val index = elementMapping[origin]
        if (!solution[index]) return
        solution[index] = false
        origin.parents.forEach { forceParentsToBeInterfaces(it) }
    }

    fun forceInheritorsToBeClasses(element: Node) {
        val queue = ArrayDeque<Node>()
        queue.add(element)
        while (queue.isNotEmpty()) {
            val e = queue.removeFirst().origin
            val index = elementMapping[e]
            if (solution[index]) continue
            solution[index] = true
            for (inheritor in elementMapping.elements) {
                if (e in inheritor.parents.map { it.origin }) {
                    queue.add(inheritor)
                }
            }
        }
    }

    requiredInterfaces.forEach(::forceParentsToBeInterfaces)
    requiredClasses.forEach(::forceInheritorsToBeClasses)
}

private fun solve2sat(elements: Collection<Node>, elementsToVar: ElementMapping): MutableList<Boolean> {
    val (g, gt) = buildGraphs(elements, elementsToVar)

    val used = g.indices.mapTo(mutableListOf()) { false }
    val order = mutableListOf<Int>()
    val comp = g.indices.mapTo(mutableListOf()) { -1 }
    val n = g.size

    fun dfs1(v: Int) {
        used[v] = true
        for (to in g[v]) {
            if (!used[to]) {
                dfs1(to)
            }
        }
        order += v
    }

    fun dfs2(v: Int, cl: Int) {
        comp[v] = cl
        for (to in gt[v]) {
            if (comp[to] == -1) {
                dfs2(to, cl)
            }
        }
    }

    for (i in g.indices) {
        if (!used[i]) {
            dfs1(i)
        }
    }

    var j = 0
    for (i in g.indices) {
        val v = order[n - i - 1]
        if (comp[v] == -1) {
            dfs2(v, j++)
        }
    }

    val res = (1..elements.size).mapTo(mutableListOf()) { false }

    for (i in 0 until n step 2) {
        if (comp[i] == comp[i + 1]) {
            throw IllegalStateException("Somehow there is no solution. Please contact with @dmitriy.novozhilov")
        }
        res[i / 2] = comp[i] > comp[i + 1]
    }
    return res
}


private fun buildGraphs(elements: Collection<Node>, elementMapping: ElementMapping): Pair<List<List<Int>>, List<List<Int>>> {
    val g = (1..elementMapping.size * 2).map { mutableListOf<Int>() }
    val gt = (1..elementMapping.size * 2).map { mutableListOf<Int>() }

    fun Int.direct(): Int = this
    fun Int.invert(): Int = this + 1

    fun extractIndex(element: Node) = elementMapping[element] * 2

    for (element in elements) {
        val elementVar = extractIndex(element)
        for (parent in element.parents) {
            val parentVar = extractIndex(parent.origin)
            // parent -> element
            g[parentVar.direct()] += elementVar.direct()
            g[elementVar.invert()] += parentVar.invert()
        }
        for (i in 0 until element.parents.size) {
            for (j in i + 1 until element.parents.size) {
                val firstParentVar = extractIndex(element.parents[i].origin)
                val secondParentVar = extractIndex(element.parents[j].origin)
                // firstParent -> !secondParent
                g[firstParentVar.direct()] += secondParentVar.invert()
                g[secondParentVar.direct()] += firstParentVar.invert()
            }
        }
    }

    for (from in g.indices) {
        for (to in g[from]) {
            gt[to] += from
        }
    }
    return g to gt
}
