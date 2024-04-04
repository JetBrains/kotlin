/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

    protected open val separateFieldsWithBlankLine: Boolean
        get() = false

    protected open fun filterFields(element: Element): Collection<Field> = element.allFields

    context(ImportCollector)
    fun printElement(element: Element) {
        addAllImports(element.additionalImports)
        printer.run {
            val kind = element.kind ?: error("Expected non-null element kind")

            printKDoc(element.extendedKDoc())
            print(kind.title, " ", element.typeName)
            print(element.params.typeParameters())

            val fieldPrinter = makeFieldPrinter(this)

            if (kind.typeKind == TypeKind.Class) {
                val optInsForParameters = filterFields(element)
                    .filter { it.implementation is AbstractField.ImplementationStrategy.RegularField }
                    .mapNotNull { it.optInAnnotation }.toSet()
                if (optInsForParameters.isNotEmpty()) {
                    print(" @OptIn(${optInsForParameters.joinToString { it.render() + "::class" }}) constructor")
                }

                println("(")
                withIndent {
                    for (field in filterFields(element)) {
                        val fieldImplementation = field.implementation
                        if (fieldImplementation is AbstractField.ImplementationStrategy.ForwardValueToParent && fieldImplementation.defaultValue == null) {
                            printPropertyDeclaration(field.name, field.typeRef, VariableKind.PARAMETER, inConstructor = true)
                            println()
                        } else if (fieldImplementation is AbstractField.ImplementationStrategy.RegularField && fieldImplementation.defaultValue == null) {
                            fieldPrinter.printField(
                                field,
                                inImplementation = false,
                                inConstructor = true,
                                override = field.fromParent,
                            )
                        }
                    }
                }
                print(")")
            }

            val parentRefs = element.parentRefs
            if (parentRefs.isNotEmpty()) {
                print(" : ")
                parentRefs.sortedBy { it.typeKind }.forEachIndexed { index, parent ->
                    if (index != 0) print(", ")
                    print(parent.render())

                    if (parent is ElementOrRef<*> && parent.element.kind!!.typeKind == TypeKind.Class) {
                        print("(")
                        println()
                        withIndent {
                            for (field in filterFields(element)) {
                                val fieldImplementation = field.implementation
                                if (fieldImplementation is AbstractField.ImplementationStrategy.ForwardValueToParent) {
                                    print("${field.name} = ")
                                    if (fieldImplementation.defaultValue != null) {
                                        print(fieldImplementation.defaultValue)
                                    } else {
                                        print(field.name)
                                    }
                                    println(",")
                                }
                            }
                        }
                        print(")")
                    } else {
                        print(parent.inheritanceClauseParenthesis())
                    }
                }
            }
            print(element.params.multipleUpperBoundsList())

            printBlock {
                var index = 0
                for (field in filterFields(element)) {
                    val fieldImplementation = field.implementation
                    if (fieldImplementation is AbstractField.ImplementationStrategy.LateinitField
                        || fieldImplementation is AbstractField.ImplementationStrategy.Abstract
                        || fieldImplementation is AbstractField.ImplementationStrategy.RegularField && fieldImplementation.defaultValue != null
                    ) {
                        if (separateFieldsWithBlankLine && index++ > 0) println()
                        fieldPrinter.printField(
                            field,
                            inImplementation = false,
                            override = field.fromParent,
                            modality = Modality.ABSTRACT.takeIf { kind.typeKind == TypeKind.Class && fieldImplementation is AbstractField.ImplementationStrategy.Abstract },
                        )
                    }
                }
                printAdditionalMethods(element)
            }
        }
    }
}
