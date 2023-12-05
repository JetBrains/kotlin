/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

/**
 * A common class for printing FIR or IR tree elements.
 */
abstract class AbstractElementPrinter<Element : AbstractElement<Element, Field, *>, Field : AbstractField<Field>>(
    private val printer: SmartPrinter,
) {

    protected abstract fun makeFieldPrinter(printer: SmartPrinter): AbstractFieldPrinter<Field>

    context(ImportCollector)
    protected abstract fun SmartPrinter.printAdditionalMethods(element: Element)

    protected open fun defaultElementKDoc(element: Element): String? = null

    protected open val separateFieldsWithBlankLine: Boolean
        get() = false

    protected open fun filterFields(element: Element): Collection<Field> = element.allFields

    context(ImportCollector)
    fun printElement(element: Element) {
        addAllImports(element.additionalImports)
        printer.run {
            val kind = element.kind ?: error("Expected non-null element kind")

            printKDoc(element.extendedKDoc(defaultElementKDoc(element)))
            print(kind.title, " ", element.typeName)
            print(element.params.typeParameters())

            val parentRefs = element.parentRefs
            if (parentRefs.isNotEmpty()) {
                print(
                    parentRefs.sortedBy { it.typeKind }.joinToString(prefix = " : ") { parent ->
                        parent.render() + parent.inheritanceClauseParenthesis()
                    }
                )
            }
            print(element.params.multipleUpperBoundsList())

            val body = SmartPrinter(StringBuilder()).apply {
                val fieldPrinter = makeFieldPrinter(this)
                withIndent {
                    for (field in filterFields(element)) {
                        if (field.isParameter) continue
                        if (!field.withGetter && field.defaultValueInImplementation == null && field.isFinal && field.fromParent) {
                            continue
                        }
                        if (separateFieldsWithBlankLine) println()
                        fieldPrinter.printField(
                            field,
                            override = field.fromParent,
                            modality = Modality.ABSTRACT.takeIf { !field.isFinal && !kind.isInterface },
                        )
                    }
                    printAdditionalMethods(element)
                }
            }.toString()

            if (body.isNotEmpty()) {
                println(" {")
                print(body.trimStart('\n'))
                print("}")
            }
            println()
        }
    }
}
