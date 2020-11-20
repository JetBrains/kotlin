/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation
import org.jetbrains.kotlin.fir.tree.generator.model.ImplementationWithArg
import org.jetbrains.kotlin.fir.tree.generator.model.KindOwner

fun configureInterfacesAndAbstractClasses(builder: AbstractFirTreeBuilder) {
    val elements = collectElements(builder)
    val elementMapping = ElementMapping(elements)

    val solution = solve2sat(elements, elementMapping)
    processRequirementsFromConfig(solution, elementMapping)
    updateKinds(solution, elementMapping)
}

private class ElementMapping(elements: Collection<KindOwner>) {
    private val varToElements: Map<Int, KindOwner> = elements.mapIndexed { index, element -> 2 * index to element.origin }.toMap() +
            elements.mapIndexed { index, element -> 2 * index + 1 to element }.toMap()
    private val elementsToVar: Map<KindOwner, Int> = elements.mapIndexed { index, element -> element.origin to index }.toMap()
    private val hasInheritors = elements.map { it to false }.toMap(mutableMapOf()).also {
        for (element in elements) {
            for (parent in element.allParents) {
                it[parent.origin] = true
            }
        }
    }

    operator fun get(element: KindOwner): Int = elementsToVar.getValue(element)
    operator fun get(index: Int): KindOwner = varToElements.getValue(index)

    fun hasInheritors(element: KindOwner): Boolean {
        return hasInheritors[element]!!
    }

    val size: Int = elements.size
}

private fun collectElements(builder: AbstractFirTreeBuilder): List<KindOwner> {
    return (builder.elements + builder.elements.flatMap { it.allImplementations }).map { it.origin }
}

private fun updateKinds(solution: List<Boolean>, elementMapping: ElementMapping) {
    for (index in solution.indices) {
        val isClass = solution[index]
        val element = elementMapping[index * 2].origin
        val existingKind = element.kind
        if (isClass) {
            if (existingKind == Implementation.Kind.Interface)
                throw IllegalStateException(element.toString())

            if (existingKind == null) {
                element.kind = when (element) {
                    is Implementation -> {
                        if (elementMapping.hasInheritors(element))
                            Implementation.Kind.AbstractClass
                        else
                            Implementation.Kind.FinalClass
                    }
                    is Element -> Implementation.Kind.AbstractClass
                    else -> throw IllegalStateException()
                }
            }
        } else {
            element.kind = Implementation.Kind.Interface
        }
    }
}

private fun processRequirementsFromConfig(solution: MutableList<Boolean>, elementMapping: ElementMapping) {
    fun processParents(element: KindOwner) {
        val origin = element.origin
        solution[elementMapping[origin]] = false
        origin.allParents.forEach { processParents(it) }
    }

    for (index in solution.indices) {
        val element = elementMapping[index * 2]
        if (element.kind != Implementation.Kind.Interface) continue
        if (!solution[index]) continue
        processParents(element)
    }
}

private fun solve2sat(elements: Collection<KindOwner>, elementsToVar: ElementMapping): MutableList<Boolean> {
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


private fun buildGraphs(elements: Collection<KindOwner>, elementMapping: ElementMapping): Pair<List<List<Int>>, List<List<Int>>> {
    val g = (1..elementMapping.size * 2).map { mutableListOf<Int>() }
    val gt = (1..elementMapping.size * 2).map { mutableListOf<Int>() }

    fun Int.direct(): Int = this
    fun Int.invert(): Int = this + 1

    fun extractIndex(element: KindOwner) = elementMapping[element] * 2

    for (element in elements) {
        val elementVar = extractIndex(element)
        for (parent in element.allParents) {
            val parentVar = extractIndex(parent.origin)
            // parent -> element
            g[parentVar.direct()] += elementVar.direct()
            g[elementVar.invert()] += parentVar.invert()
        }
        for (i in 0 until element.allParents.size) {
            for (j in i + 1 until element.allParents.size) {
                val firstParentVar = extractIndex(element.allParents[i].origin)
                val secondParentVar = extractIndex(element.allParents[j].origin)
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

private val KindOwner.origin: KindOwner get() = if (this is ImplementationWithArg) implementation else this