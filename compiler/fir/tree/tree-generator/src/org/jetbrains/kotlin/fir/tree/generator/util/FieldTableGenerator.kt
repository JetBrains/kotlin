/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import java.io.File

fun printFieldUsageTable(builder: AbstractFirTreeBuilder) {
    val elements = builder.elements.filter { it.allImplementations.isNotEmpty() }
    val fields = elements.flatMapTo(mutableSetOf()) { it.allFields }

    val mapping = mutableMapOf<Element, Set<Field>>()
    val fieldsCount = mutableMapOf<Field, Int>()
    for (element in elements) {
        val containingFields = mutableSetOf<Field>()
        for (field in fields) {
            if (field in element.allFields) {
                containingFields += field
                fieldsCount[field] = fieldsCount.getOrDefault(field, 0) + 1
            }
        }
        mapping[element] = containingFields
    }

    val sortedFields = fields.sortedByDescending { fieldsCount[it] }
    File("compiler/fir/tree/table.csv").printWriter().use { printer ->
        with(printer) {
            val delim = ","
            print(delim)
            println(sortedFields.joinToString(delim) { "${it.name}:${fieldsCount.getValue(it)}" })
            for (element in elements) {
                print(element.name + delim)
                val containingFields = mapping.getValue(element)
                println(sortedFields.joinToString(delim) { if (it in containingFields) "+" else "-" })
            }
        }
    }
}