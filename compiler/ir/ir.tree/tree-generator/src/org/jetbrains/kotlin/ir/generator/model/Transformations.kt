/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.*

internal object InferredOverriddenType : TypeRefWithNullability {

    context(ImportCollector)
    override fun renderTo(appendable: Appendable) {
        renderingIsNotSupported()
    }

    override fun substitute(map: TypeParameterSubstitutionMap) = this

    override val nullable: Boolean
        get() = false

    override fun copy(nullable: Boolean) = this
}

typealias Model = org.jetbrains.kotlin.generators.tree.Model<Element>

internal fun markLeaves(elements: List<Element>) {
    val leaves = elements.toMutableSet()

    for (el in elements) {
        for (parent in el.elementParents) {
            if (!parent.element.isLeaf) {
                leaves.remove(parent.element)
            }
        }
    }

    for (el in leaves) {
        el.isLeaf = true
        if (el.parentInVisitor != null) {
            el.hasAcceptMethod = true
        }
    }
}

internal fun processFieldOverrides(elements: List<Element>) {
    for (element in iterateElementsParentFirst(elements)) {
        for (field in element.fields) {
            fun visitParents(visited: Element) {
                for (parent in visited.elementParents) {
                    val overriddenField = parent.element.fields.singleOrNull { it.name == field.name }
                    if (overriddenField != null) {
                        field.fromParent = true
                        field.optInAnnotation = field.optInAnnotation ?: overriddenField.optInAnnotation

                        fun transformInferredType(type: TypeRef, overriddenType: TypeRef) =
                            type.takeUnless { it is InferredOverriddenType } ?: overriddenType
                        when (field) {
                            is SingleField -> {
                                field.typeRef =
                                    transformInferredType(field.typeRef, (overriddenField as SingleField).typeRef) as TypeRefWithNullability
                            }
                            is ListField -> {
                                field.elementType = transformInferredType(field.elementType, (overriddenField as ListField).elementType)
                            }
                        }

                        break
                    }

                    visitParents(parent.element)
                }
            }

            visitParents(element)
        }
    }
}

internal fun addWalkableChildren(elements: List<Element>) {
    for (element in elements) {
        val walkableChildren = mutableMapOf<String, Field>()

        fun visitParents(visited: Element) {
            for (parent in visited.elementParents) {
                if (!parent.element.ownsChildren) {
                    for (field in parent.element.fields) {
                        if (field.isChild) {
                            walkableChildren[field.name] = field
                        }
                    }

                    visitParents(parent.element)
                }
            }
        }

        visitParents(element)

        element.fields.filter { it.isChild }.associateByTo(walkableChildren) { it.name }

        element.walkableChildren = reorderIfNecessary(walkableChildren.values.toList(), element.childrenOrderOverride)
    }
}

private fun reorderIfNecessary(fields: List<Field>, order: List<String>?): List<Field> =
    if (order == null) fields else fields.sortedBy {
        val position = order.indexOf(it.name)
        if (position < 0) order.size else position
    }

private fun iterateElementsParentFirst(elements: List<Element>) = sequence {
    val pending = elements.sortedBy { it.elementParents.size }.toMutableSet()
    pendingLoop@ while (pending.isNotEmpty()) {
        val iter = pending.iterator()
        while (iter.hasNext()) {
            val element = iter.next()
            if (element.elementParents.none { it.element in pending }) {
                yield(element)
                iter.remove()
                continue@pendingLoop
            }
        }

        error("Cannot find next element to process")
    }
}
