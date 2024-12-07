/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.imports.ImportCollector
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

/**
 * A common class for printing FIR or IR tree elements.
 */
abstract class AbstractElementPrinter<Element : AbstractElement<Element, Field, *>, Field : AbstractField<Field>>(
    private val printer: ImportCollectingPrinter,
) {

    protected abstract fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field>

    protected abstract fun ImportCollectingPrinter.printAdditionalMethods(element: Element)

    protected open val separateFieldsWithBlankLine: Boolean
        get() = false

    protected open fun filterFields(element: Element): Collection<Field> = element.allFields

    protected open fun ImportCollecting.elementKDoc(element: Element): String = element.extendedKDoc()

    fun printElement(element: Element) {
        printer.run {
            val kind = element.kind ?: error("Expected non-null element kind")

            printKDoc(elementKDoc(element))
            print(kind.title, " ", element.typeName)
            print(element.params.typeParameters())
            printInheritanceClause(element.parentRefs)
            print(element.params.multipleUpperBoundsList())

            val printer = SmartPrinter(StringBuilder())
            this@AbstractElementPrinter.printer.withNewPrinter(printer) {
                val fieldPrinter = makeFieldPrinter(this)
                withIndent {
                    for (field in filterFields(element)) {
                        if (field.isParameter) continue
                        if (field.isFinal && field.isOverride) {
                            continue
                        }
                        if (separateFieldsWithBlankLine) println()
                        fieldPrinter.printField(
                            field,
                            inImplementation = false,
                            override = field.isOverride,
                            modality = Modality.ABSTRACT.takeIf { !field.isFinal && !kind.isInterface },
                        )
                    }
                    printAdditionalMethods(element)
                }
            }
            val body = printer.toString()

            if (body.isNotEmpty()) {
                println(" {")
                print(body.trimStart('\n'))
                print("}")
            }
            println()
            addAllImports(element.additionalImports)
        }
    }
}
