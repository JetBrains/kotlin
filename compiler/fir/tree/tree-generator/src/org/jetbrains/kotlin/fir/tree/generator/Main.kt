/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.*
import java.io.File


fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("compiler/fir/tree/gen").absoluteFile

    NodeConfigurator.configureFields()
    detectBaseTransformerTypes(FirTreeBuilder)
    ImplementationConfigurator.configureImplementations()
    configureInterfacesAndAbstractClasses(FirTreeBuilder)
    removePreviousGeneratedFiles(generationPath)
    printElements(FirTreeBuilder, generationPath)
//    printTable(FirTreeBuilder)
//    printInterfaceClassGraph(FirTreeBuilder)
}

fun Element.traverseParents(block: (Element) -> Unit) {
    block(this)
    parents.forEach { it.traverseParents(block) }
}

private fun printInterfaceClassGraph(builder: AbstractFirTreeBuilder) {
    fun Implementation.Kind.toColor(): String = when (this) {
        Implementation.Kind.Interface -> "green"
        else -> "red"
    }
    val elements = builder.elements + builder.elements.flatMap { it.allParents }

    File("FirTree.dot").printWriter().use { printer ->
        with(printer) {
            println("digraph FirTree {")
            elements.forEach {
                println("    ${it.type} [color=${it.kind!!.toColor()}]")
            }
            println()
            elements.forEach { element ->
                element.allParents.forEach { parent ->
                    println("    ${parent.type} -> ${element.type}")
                }
            }
            println("}")
        }
    }
}

private fun detectBaseTransformerTypes(builder: AbstractFirTreeBuilder) {
    val usedAsFieldType = mutableMapOf<AbstractElement, Boolean>().withDefault { false }
    for (element in builder.elements) {
        for (field in element.allFirFields) {
            val fieldElement = when (field) {
                is FirField -> field.element
                is FieldList -> field.baseType as Element
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

private fun removePreviousGeneratedFiles(generationPath: File) {
    generationPath.walkTopDown().forEach {
        if (it.isFile && it.readText().contains(GENERATED_MESSAGE)) {
            it.delete()
        }
    }
}

private fun printTable(builder: AbstractFirTreeBuilder) {
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