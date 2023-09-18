/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

/**
 * A common class for printing FIR or IR tree elements.
 */
abstract class AbstractElementPrinter<Element : AbstractElement<*, Field>, Field : AbstractField>(
    private val printer: SmartPrinter,
) {
    protected abstract val fieldPrinter: AbstractFieldPrinter<Field>

    protected open fun pureAbstractElementType(element: Element): String? = null

    protected abstract fun SmartPrinter.printAdditionalMethods(element: Element)

    fun printElement(element: Element) {
        printer.run {
            val kind = element.kind ?: error("Expected non-null element kind")
            fun abstract() {
                if (!kind.isInterface) {
                    print("abstract ")
                }
            }

            print("${kind.title} ${element.type}")
            print(element.typeParameters())

            val pureAbstractElementType = pureAbstractElementType(element)
            if (element.elementParents.isNotEmpty() || pureAbstractElementType != null) {
                print(" : ")
                if (pureAbstractElementType != null) {
                    print("$pureAbstractElementType()")
                    if (element.elementParents.isNotEmpty()) {
                        print(", ")
                    }
                }
                print(
                    element.elementParents.joinToString(", ") {
                        // TODO: Factor out
                        var result = it.element.type
                        if (it.args.isNotEmpty()) {
                            result += it.args.values.joinToString(", ", "<", ">") { it.typeWithArguments }
                        }
                        result + it.element.kind.braces()
                    },
                )
            }
            print(element.multipleUpperBoundsList())
            println(" {")
            withIndent {
                for (field in element.allFields) {
                    if (field.isFinal && field.fromParent || field.isParameter) continue
                    fieldPrinter.printField(field, isImplementation = false, override = field.fromParent) {
                        if (!field.isFinal) {
                            abstract()
                        }
                    }
                }
                if (element.allFields.isNotEmpty()) {
                    println()
                }
                printAdditionalMethods(element)
            }
            println("}")
        }
    }
}

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