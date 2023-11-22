/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

/**
 * Runs [block] on this element and all its parents recursively.
 */
fun <Element : AbstractElement<Element, *, *>> Element.traverseParents(block: (Element) -> Unit) {
    traverseParentsUntil { block(it); false }
}

/**
 * Runs [block] on this element and all its parents recursively.
 *
 * If [block] returns `true` at any point, aborts iteration and returns `true`.
 *
 * If [block] always returns `false`, visits all the parents and returns `false`.
 */
fun <Element : AbstractElement<Element, *, *>> Element.traverseParentsUntil(block: (Element) -> Boolean): Boolean {
    if (block(this)) return true
    for (parent in elementParents) {
        if (parent.element.traverseParentsUntil(block)) return true
    }
    return false
}

/**
 * For each tree element, sets its [AbstractElement.baseTransformerType] to one of its parents if that parent type is used at least once as
 * a type of a field, except when that field is explicitly opted out of it via
 * [AbstractField.useInBaseTransformerDetection].
 */
fun <Element : AbstractElement<Element, *, *>> detectBaseTransformerTypes(model: Model<Element>) {
    val usedAsFieldType = hashSetOf<AbstractElement<*, *, *>>()
    for (element in model.elements) {
        for (field in element.allFields.filter { it.containsElement }) {
            if (!field.useInBaseTransformerDetection) continue
            val fieldElement = (field.typeRef as? ElementOrRef<*>)?.element
                ?: ((field as? ListField)?.baseType as? ElementOrRef<*>)?.element
                ?: continue
            if (fieldElement.isRootElement) continue
            usedAsFieldType.add(fieldElement)
        }
    }

    for (element in model.elements) {
        element.traverseParents {
            if (it in usedAsFieldType) {
                element.baseTransformerType = it
                return@traverseParents
            }
        }
    }
}

operator fun <K, V, U> MutableMap<K, MutableMap<V, U>>.set(k1: K, k2: V, value: U) {
    this.putIfAbsent(k1, mutableMapOf())
    val map = getValue(k1)
    map[k2] = value
}

operator fun <K, V, U> Map<K, Map<V, U>>.get(k1: K, k2: V): U {
    return getValue(k1).getValue(k2)
}

internal fun <Field : AbstractField<*>> List<Field>.reorderFieldsIfNecessary(order: List<String>?): List<Field> =
    if (order == null) {
        this
    } else {
        sortedBy {
            val position = order.indexOf(it.name)
            if (position < 0) order.size else position
        }
    }

val ImplementationKind.hasLeafBuilder: Boolean
    get() = this == ImplementationKind.FinalClass || this == ImplementationKind.OpenClass

val AbstractElement<*, *, *>.safeDecapitalizedName: String
    get() = when (name) {
        "Class" -> "klass"
        else -> name.replaceFirstChar(Char::lowercaseChar)
    }
