/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.printer

import org.jetbrains.kotlin.generators.tree.AbstractElement
import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.ImportCollector
import org.jetbrains.kotlin.generators.tree.render
import org.jetbrains.kotlin.utils.SmartPrinter

/**
 * The angle bracket-delimited list of type parameters to print, or empty string if the element has no type parameters.
 *
 * For type parameters that have a single upper bound, also prints that upper bound. If at least one type parameter has multiple upper
 * bounds, doesn't print any upper bounds at all. They are expected to be printed in the `where` clause (see [multipleUpperBoundsList]).
 *
 * @param end The string to add after the closing angle bracket of the type parameter list
 */
context(ImportCollector)
fun AbstractElement<*, *>.typeParameters(end: String = ""): String = params.takeIf { it.isNotEmpty() }
    ?.joinToString(", ", "<", ">$end") { param ->
        param.name + (param.bounds.singleOrNull()?.let { " : ${it.render()}" } ?: "")
    } ?: ""

/**
 * The `where` clause to print after the class or function declaration if at least one of the element's tye parameters has multiple upper
 * bounds.
 *
 * Otherwise, an empty string.
 */
context(ImportCollector)
fun AbstractElement<*, *>.multipleUpperBoundsList(): String {
    val paramsWithMultipleUpperBounds = params.filter { it.bounds.size > 1 }.takeIf { it.isNotEmpty() } ?: return ""
    return buildString {
        append(" where ")
        paramsWithMultipleUpperBounds.joinTo(this, separator = ", ") { param ->
            param.bounds.joinToString(", ") { bound -> "$param : ${bound.render()}" }
        }
        append("")
    }
}

/**
 * The braces to print in the inheritance clause if this is a class, or empty string if this is an interface.
 */
fun ImplementationKind?.braces(): String = when (this) {
    ImplementationKind.Interface, ImplementationKind.SealedInterface -> ""
    ImplementationKind.OpenClass, ImplementationKind.AbstractClass, ImplementationKind.SealedClass -> "()"
    else -> throw IllegalStateException(this.toString())
}

fun SmartPrinter.printKDoc(kDoc: String?) {
    if (kDoc == null) return
    println("/**")
    for (line in kDoc.lineSequence()) {
        print(" *")
        if (line.isBlank()) {
            println()
        } else {
            print(" ")
            println(line)
        }
    }
    println(" */")
}

fun AbstractElement<*, *>.extendedKDoc(defaultKDoc: String? = null): String = buildString {
    val doc = kDoc ?: defaultKDoc
    if (doc != null) {
        appendLine(doc)
        appendLine()
    }
    append("Generated from: [${element.propertyName}]")
}
