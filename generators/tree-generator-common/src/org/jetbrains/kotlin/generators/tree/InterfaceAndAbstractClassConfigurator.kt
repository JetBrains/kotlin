/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.util.Node
import org.jetbrains.kotlin.generators.util.solveGraphForClassVsInterface

/**
 * Decides which element in the tree must be an (abstract) class, and which must be an interface.
 *
 * @property elements The list of elements of the tree to infer their [ImplementationKind].
 */
class InterfaceAndAbstractClassConfigurator(val elements: List<ImplementationKindOwner>) {

    private inner class NodeImpl(val element: ImplementationKindOwner) : Node {
        override val parents: List<NodeImpl>
            get() = element.allParents.map(::NodeImpl)

        override val origin: NodeImpl
            get() = this

        override fun equals(other: Any?): Boolean = other is NodeImpl && element == other.element

        override fun hashCode(): Int = element.hashCode()
    }

    private fun shouldBeFinalClass(element: ImplementationKindOwner, allParents: Set<ImplementationKindOwner>): Boolean =
        element is AbstractImplementation<*, *, *> && element !in allParents

    private fun updateKinds(nodes: List<NodeImpl>, solution: List<Boolean>) {
        val allParents = nodes.flatMapTo(mutableSetOf()) { element -> element.parents.map { it.origin.element } }

        for (index in solution.indices) {
            val isClass = solution[index]
            val node = nodes[index].origin
            val element = node.element
            val existingKind = element.kind
            if (isClass) {
                require(existingKind != ImplementationKind.Interface) {
                    "$element must NOT be an interface"
                }
                if (existingKind == null) {
                    element.kind = if (shouldBeFinalClass(element, allParents))
                        ImplementationKind.FinalClass
                    else
                        ImplementationKind.AbstractClass
                }
            } else {
                element.kind = ImplementationKind.Interface
            }
        }
    }

    private fun updateSealedKinds(nodes: Collection<NodeImpl>) {
        for (node in nodes) {
            val element = node.element
            if (element is AbstractElement<*, *, *>) {
                if (element.isSealed) {
                    element.kind = when (element.kind) {
                        ImplementationKind.AbstractClass -> ImplementationKind.SealedClass
                        ImplementationKind.Interface -> ImplementationKind.SealedInterface
                        else -> error("element $element with kind ${element.kind} can not be sealed")
                    }
                }
            }
        }
    }

    fun configureInterfacesAndAbstractClasses() {
        val nodes = this.elements.map(::NodeImpl)
        val solution = solveGraphForClassVsInterface(
            nodes,
            nodes.filter { it.element.kind?.typeKind == TypeKind.Interface },
            nodes.filter { it.element.kind?.typeKind == TypeKind.Class },
        )
        updateKinds(nodes, solution)
        updateSealedKinds(nodes)
    }
}