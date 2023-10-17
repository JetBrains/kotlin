/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.model.FieldList
import org.jetbrains.kotlin.fir.tree.generator.model.FirField
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef

/**
 * For each FIR element, sets its [Element.baseTransformerType] to one of it parents if that parent FIR type is used at least once as
 * a type of [Field], except when that [Field] is explicitly opted out of it via [Field.useInBaseTransformerDetection].
 */
fun detectBaseTransformerTypes(builder: AbstractFirTreeBuilder) {
    val usedAsFieldType = hashSetOf<Element>()
    for (element in builder.elements) {
        for (field in element.allFirFields) {
            if (!field.useInBaseTransformerDetection) continue
            val fieldElement = when (field) {
                is FirField -> field.element.element
                is FieldList -> (field.baseType as GenericElementOrRef<*, *>).element as Element
                else -> error("Invalid field type: $field")
            }
            if (fieldElement == AbstractFirTreeBuilder.baseFirElement) continue
            usedAsFieldType.add(fieldElement)
        }
    }

    for (element in builder.elements) {
        element.traverseParents {
            if (it in usedAsFieldType) {
                element.baseTransformerType = it
                return@traverseParents
            }
        }
    }
}
