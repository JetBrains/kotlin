/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.generators.tree.ImplementationKindOwner
import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.util.Node
import org.jetbrains.kotlin.generators.util.solveGraphForClassVsInterface

private class NodeImpl(val element: ImplementationKindOwner) : Node {
    override val parents: List<Node>
        get() = element.allParents.map(::NodeImpl)

    override val origin: NodeImpl
        get() = if (element.origin == element) this else NodeImpl(element.origin)

    override fun equals(other: Any?): Boolean =
        other is NodeImpl && element == other.element

    override fun hashCode(): Int =
        element.hashCode()
}

fun configureInterfacesAndAbstractClasses(builder: AbstractFirTreeBuilder) {
    val elements = collectElements(builder)
    val solution = solveGraphForClassVsInterface(
        elements,
        elements.filter { it.element.kind?.isInterface == true },
        elements.filter { it.element.kind?.isInterface == false },
    )
    updateKinds(elements, solution)
    updateSealedKinds(elements)
}

private fun collectElements(builder: AbstractFirTreeBuilder): List<NodeImpl> {
    return (builder.elements + builder.elements.flatMap { it.allImplementations }).map { NodeImpl(it.origin) }
}

private fun updateKinds(nodes: List<NodeImpl>, solution: List<Boolean>) {
    val allParents = nodes.flatMapTo(mutableSetOf()) { element -> element.parents.map { it.origin } }

    for (index in solution.indices) {
        val isClass = solution[index]
        val node = nodes[index].origin
        val element = node.element
        val existingKind = element.kind
        if (isClass) {
            if (existingKind == ImplementationKind.Interface)
                throw IllegalStateException(element.toString())

            if (existingKind == null) {
                element.kind = when (element) {
                    is Implementation -> {
                        if (node in allParents)
                            ImplementationKind.AbstractClass
                        else
                            ImplementationKind.FinalClass
                    }
                    is Element -> ImplementationKind.AbstractClass
                    else -> throw IllegalStateException()
                }
            }
        } else {
            element.kind = ImplementationKind.Interface
        }
    }
}

private fun updateSealedKinds(nodes: Collection<NodeImpl>) {
    for (node in nodes) {
        val element = node.element
        if (element is Element) {
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

private val ImplementationKindOwner.origin: ImplementationKindOwner get() = if (this is ImplementationWithArg) implementation else this
