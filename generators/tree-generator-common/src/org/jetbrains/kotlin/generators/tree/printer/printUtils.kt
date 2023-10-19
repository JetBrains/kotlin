/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.printer

import org.jetbrains.kotlin.generators.tree.AbstractElement
import org.jetbrains.kotlin.generators.tree.ImplementationKind

/**
 * The angle bracket-delimited list of type parameters to print, or empty string if the element has no type parameters.
 *
 * For type parameters that have a single upper bound, also prints that upper bound. If at least one type parameter has multiple upper
 * bounds, doesn't print any upper bounds at all. They are expected to be printed in the `where` clause (see [multipleUpperBoundsList]).
 *
 * @param end The string to add after the closing angle bracket of the type parameter list
 */
fun AbstractElement<*, *>.typeParameters(end: String = ""): String = params.takeIf { it.isNotEmpty() }
    ?.joinToString(", ", "<", ">$end") { param ->
        param.name + (param.bounds.singleOrNull()?.let { " : ${it.typeWithArguments}" } ?: "")
    } ?: ""

/**
 * The `where` clause to print after the class or function declaration if at least one of the element's tye parameters has multiple upper
 * bounds.
 *
 * Otherwise, an empty string.
 */
fun AbstractElement<*, *>.multipleUpperBoundsList(): String {
    val paramsWithMultipleUpperBounds = params.filter { it.bounds.size > 1 }.takeIf { it.isNotEmpty() } ?: return ""
    return buildString {
        append(" where ")
        paramsWithMultipleUpperBounds.joinTo(this, separator = ", ") { param ->
            param.bounds.joinToString(", ") { bound -> "$param : ${bound.typeWithArguments}" }
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

val AbstractElement<*, *>.generics: String
    get() = params.takeIf { it.isNotEmpty() }
        ?.let { it.joinToString(", ", "<", ">") { it.name } }
        ?: ""
