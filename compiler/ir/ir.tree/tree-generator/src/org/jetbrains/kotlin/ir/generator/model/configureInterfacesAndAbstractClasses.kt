/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.util.Node
import org.jetbrains.kotlin.generators.util.solveGraphForClassVsInterface

private class NodeImpl(val element: Element) : Node {
    override val parents: List<Node>
        get() = element.elementParents.map { NodeImpl(it.element) }

    override val origin: Node
        get() = this

    override fun equals(other: Any?): Boolean =
        other is NodeImpl && element == other.element

    override fun hashCode(): Int =
        element.hashCode()
}

fun configureInterfacesAndAbstractClasses(elements: List<Element>) {
    val nodes = elements.map(::NodeImpl)
    val solution = solveGraphForClassVsInterface(
        nodes,
        nodes.filter { it.element.targetKind == TypeKind.Interface },
        nodes.filter { it.element.targetKind == TypeKind.Class },
    )
    updateKinds(nodes, solution)
}

private fun updateKinds(nodes: List<NodeImpl>, solution: List<Boolean>) {
    for (index in solution.indices) {
        val isClass = solution[index]
        val element = nodes[index].element
        if (isClass) {
            check(element.targetKind != TypeKind.Interface) { element }
            element.kind = ImplementationKind.AbstractClass
        } else {
            element.kind = ImplementationKind.Interface
        }
    }
}
