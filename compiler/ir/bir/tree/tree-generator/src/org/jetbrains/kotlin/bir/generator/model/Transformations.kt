/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.model

import org.jetbrains.kotlin.bir.generator.BirTree
import org.jetbrains.kotlin.bir.generator.elementImplBaseType
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

fun transformModel(model: Model) {
    InterfaceAndAbstractClassConfigurator(model.elements)
        .configureInterfacesAndAbstractClasses()
    addPureAbstractElement(model.elements, elementImplBaseType)
    adjustSymbolOwners(model.elements)
    markLeaves(model.elements)
    setClassIds(model.elements)
    processFieldOverrides(model.elements)
    computeFieldProperties(model.elements)
    computeFieldFakeOverrides(model.elements)
    addWalkableChildren(model.elements)
}

private fun markLeaves(elements: List<Element>) {
    val leaves = elements.toMutableSet()
    leaves -= BirTree.rootElement

    for (el in elements) {
        for (parent in el.elementParents) {
            if (!parent.element.isLeaf) {
                leaves.remove(parent.element)
            }
        }
    }

    for (el in leaves) {
        el.isLeaf = true
    }
}

private fun adjustSymbolOwners(elements: List<Element>) {
    for (element in elements) {
        if (element.elementParentsRecursively().any { it.element.name == BirTree.symbolOwner.name }) {
            val symbolField = element.fields.singleOrNull { it.name == "symbol" } as SingleField?
            if (symbolField != null) {
                element.fields.remove(symbolField)

                val symbolType = when (val type = symbolField.typeRef) {
                    is ClassRef<*> -> type
                    is TypeVariable -> type.bounds.single() as ClassRef<*>
                    else -> error(type)
                }
                element.ownerSymbolType = symbolType
                element.otherParents += symbolType
            }
        }
    }
}

private fun setClassIds(elements: List<Element>) {
    var id = 0
    for (element in elements.sortedBy { it.name }) {
        element.classId = id++
    }
}

private fun processFieldOverrides(elements: List<Element>) {
    for (element in iterateElementsParentFirst(elements)) {
        for (field in element.fields) {
            fun visitParents(visited: Element) {
                for (parent in visited.elementParents) {
                    val overriddenField = parent.element.fields.singleOrNull { it.name == field.name }
                    if (overriddenField != null) {
                        field.isOverride = true
                        field.optInAnnotation = field.optInAnnotation ?: overriddenField.optInAnnotation

                        fun transformInferredType(type: TypeRef, overriddenType: TypeRef) =
                            type.takeUnless { it is InferredOverriddenType } ?: overriddenType
                        when (field) {
                            is SingleField -> {
                                field.typeRef =
                                    transformInferredType(field.typeRef, (overriddenField as SingleField).typeRef) as TypeRefWithNullability
                            }
                            is ListField -> {
                                field.baseType = transformInferredType(field.baseType, (overriddenField as ListField).baseType)
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

private fun computeFieldProperties(elements: List<Element>) {
    for (element in elements) {
        for (field in element.fields) {
            field.passViaConstructorParameter = !(field is ListField && field.isChild) && !field.initializeToThis
            field.isReadWriteTrackedProperty = field.isMutable && !(field is ListField && field.isChild)
        }
    }
}

private fun computeFieldFakeOverrides(elements: List<Element>) {
    for (element in elements.filter { it.isLeaf }) {
        val allFields = element.allFieldsRecursively()
        element.fieldFakeOverrides += allFields.associateWith { FieldFakeOverride(it) }

        var nextPropertyId = element.fieldFakeOverrides.count { it.key is ListField && it.key.isChild } + 1
        element.fieldFakeOverrides
            .filterKeys { it.isReadWriteTrackedProperty }
            .entries
            .sortedBy {
                val field = it.key
                // order by the most likely to change
                when {
                    field.isChild -> 1
                    field.typeRef is GenericElementRef<*> -> 2
                    field is ListField && field.baseType is GenericElementRef<*> -> 3
                    field.name == "sourceSpan" -> 11
                    field.name == "signature" -> 12
                    else -> 10
                }
            }
            .forEach {
                it.value.propertyId = (nextPropertyId++).coerceAtMost(14) // see [BirImplElementBase] for this maximum value
            }
    }
}

private fun addWalkableChildren(elements: List<Element>) {
    for (element in elements) {
        val walkableChildren = element.allFieldsRecursively().filter { it.isChild }
        element.walkableChildren = reorderIfNecessary(walkableChildren.toList(), element.childrenOrderOverride)
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

