/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.AbstractElement
import org.jetbrains.kotlin.fir.tree.generator.model.FieldList
import org.jetbrains.kotlin.fir.tree.generator.model.FirField

fun detectBaseTransformerTypes(builder: AbstractFirTreeBuilder) {
    val usedAsFieldType = mutableMapOf<AbstractElement, Boolean>().withDefault { false }
    for (element in builder.elements) {
        for (field in element.allFirFields) {
            val fieldElement = when (field) {
                is FirField -> field.element
                is FieldList -> field.baseType as AbstractElement
                else -> throw IllegalArgumentException()
            }
            usedAsFieldType[fieldElement] = true
        }
    }

    for (element in builder.elements) {
        element.traverseParents {
            if (usedAsFieldType.getValue(it)) {
                element.baseTransformerType = it
                return@traverseParents
            }
        }
    }
}
