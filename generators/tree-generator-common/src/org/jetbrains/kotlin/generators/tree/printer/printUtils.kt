/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.generators.tree.ImportCollector
import org.jetbrains.kotlin.generators.tree.render
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.joinToWithBuffer
import org.jetbrains.kotlin.utils.withIndent

/**
 * The angle bracket-delimited list of type parameters to print, or empty string if the list is empty.
 *
 * For type parameters that have a single upper bound, also prints that upper bound. If at least one type parameter has multiple upper
 * bounds, doesn't print any upper bounds at all. They are expected to be printed in the `where` clause (see [multipleUpperBoundsList]).
 *
 * @param end The string to add after the closing angle bracket of the type parameter list
 */
context(ImportCollector)
fun List<TypeVariable>.typeParameters(end: String = ""): String = buildString {
    if (this@typeParameters.isEmpty()) return@buildString
    joinToWithBuffer(this, prefix = "<", postfix = ">") { param ->
        if (param.variance != Variance.INVARIANT) {
            append(param.variance.label)
            append(" ")
        }
        append(param.name)
        param.bounds.singleOrNull()?.let {
            append(" : ")
            it.renderTo(this)
        }
    }
    append(end)
}

/**
 * The `where` clause to print after the class or function declaration if at least one of the element's tye parameters has multiple upper
 * bounds.
 *
 * Otherwise, an empty string.
 */
context(ImportCollector)
fun List<TypeVariable>.multipleUpperBoundsList(): String {
    val paramsWithMultipleUpperBounds = filter { it.bounds.size > 1 }.takeIf { it.isNotEmpty() } ?: return ""
    return buildString {
        append(" where ")
        paramsWithMultipleUpperBounds.joinToWithBuffer(this, separator = ", ") { param ->
            param.bounds.joinToWithBuffer(this) { bound ->
                append(param.name)
                append(" : ")
                bound.renderTo(this)
            }
        }
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

data class FunctionParameter(val name: String, val type: TypeRef, val defaultValue: String? = null) {

    context(ImportCollector)
    fun render(): String = buildString {
        append(name, ": ", type.render())
        defaultValue?.let {
            append(" = ", it)
        }
    }
}

context(ImportCollector)
fun SmartPrinter.printFunctionDeclaration(
    name: String,
    parameters: List<FunctionParameter>,
    returnType: TypeRef,
    typeParameters: List<TypeVariable> = emptyList(),
    modality: Modality? = null,
    override: Boolean = false,
    allParametersOnSeparateLines: Boolean = false,
) {
    when (modality) {
        null -> {}
        Modality.FINAL -> print("final ")
        Modality.OPEN -> print("open ")
        Modality.ABSTRACT -> print("abstract ")
        Modality.SEALED -> error("Function cannot be sealed")
    }
    if (override) {
        print("override ")
    }
    print("fun ")
    print(typeParameters.typeParameters(end = " "))
    print(name, "(")

    if (allParametersOnSeparateLines) {
        if (parameters.isNotEmpty()) {
            println()
            withIndent {
                for (parameter in parameters) {
                    print(parameter.render())
                    println(",")
                }
            }
        }
    } else {
        print(parameters.joinToString { it.render() })
    }
    print(")")
    if (returnType != StandardTypes.unit) {
        print(": ", returnType.render())
    }
    print(typeParameters.multipleUpperBoundsList())
}