/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

fun <Element : AbstractElement<Element, *>> Element.traverseParents(block: (Element) -> Unit) {
    block(this)
    elementParents.forEach { it.element.traverseParents(block) }
}

/**
 * For each tree element, sets its [AbstractElement.baseTransformerType] to one of its parents if that parent type is used at least once as
 * a type of a field, except when that field is explicitly opted out of it via
 * [AbstractField.useInBaseTransformerDetection].
 */
fun <Element : AbstractElement<Element, *>> detectBaseTransformerTypes(model: Model<Element>) {
    val usedAsFieldType = hashSetOf<AbstractElement<*, *>>()
    for (element in model.elements) {
        for (field in element.allFields.filter { it.containsElement }) {
            if (!field.useInBaseTransformerDetection) continue
            val fieldElement = (field.typeRef as? ElementOrRef<*, *>)?.element
                ?: ((field as? ListField)?.baseType as? ElementOrRef<*, *>)?.element
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