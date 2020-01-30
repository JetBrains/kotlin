/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation
import java.io.File

fun printHierarchyGraph(builder: AbstractFirTreeBuilder) {
    fun Implementation.Kind.toColor(): String = when (this) {
        Implementation.Kind.Interface -> "green"
        else -> "red"
    }

    val elements = builder.elements + builder.elements.flatMap { it.allParents }

    data class Edge(val from: String, val to: String) {
        override fun toString(): String {
            return "$from -> $to"
        }
    }

    File("FirTree.dot").printWriter().use { printer ->
        with(printer) {
            println("digraph FirTree {")
            elements.forEach {
                println("    ${it.type} [color=${it.kind!!.toColor()}]")
            }
            println()
            val edges = mutableSetOf<Edge>()
            elements.forEach { element ->
                element.allParents.forEach { parent ->
                    edges += Edge(parent.type, element.type)
                }
            }
            edges.forEach {
                print("    ")
                println(it)
            }

            println("}")
        }
    }
}
