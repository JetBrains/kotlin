/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.withIndent

abstract class AbstractImplementationPrinter<Implementation, Element, ImplementationField>(
    private val printer: SmartPrinter,
)
        where Implementation : AbstractImplementation<Implementation, Element, ImplementationField>,
              Element : AbstractElement<Element, *, Implementation>,
              ImplementationField : AbstractField<*> {

    protected abstract val implementationOptInAnnotation: ClassRef<*>

    protected abstract val pureAbstractElementType: ClassRef<*>

    protected open val separateFieldsWithBlankLine: Boolean
        get() = false

    protected abstract fun makeFieldPrinter(printer: SmartPrinter): AbstractFieldPrinter<ImplementationField>

    context(ImportCollector)
    protected open fun SmartPrinter.printAdditionalMethods(implementation: Implementation) {
    }

    context(ImportCollector)
    protected open fun SmartPrinter.printAdditionalConstructorParameters(implementation: Implementation) {
    }

    context(ImportCollector)
    fun printImplementation(implementation: Implementation) {
        addAllImports(implementation.additionalImports)
        printer.run {
            printKDoc(implementation.kDoc)
            buildSet {
                if (implementation.requiresOptIn) {
                    add(implementationOptInAnnotation)
                }

                for (field in implementation.fieldsInConstructor) {
                    field.optInAnnotation?.let {
                        add(it)
                    }
                }
            }.ifNotEmpty {
                println("@OptIn(", joinToString { "${it.render()}::class" }, ")")
            }

            if (!implementation.isPublic) {
                print("internal ")
            }

            val kind = implementation.kind ?: error("Expected non-null element kind")
            print("${kind.title} ${implementation.typeName}")
            print(implementation.element.params.typeParameters(end = " "))

            val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
            val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass

            val fieldPrinter = makeFieldPrinter(this)

            if (!isInterface && !isAbstract) {
                var printConstructor = false
                if (implementation.isPublic && implementation.isConstructorPublic && implementation.putImplementationOptInInConstructor) {
                    print(" @", implementationOptInAnnotation.render())
                    printConstructor = true
                }
                if (implementation.isPublic && !implementation.isConstructorPublic) {
                    print(" internal")
                    printConstructor = true
                }

                if (printConstructor) {
                    print(" constructor")
                }

                println("(")
                withIndent {
                    printAdditionalConstructorParameters(implementation)
                    for (field in implementation.allFields.reorderFieldsIfNecessary(implementation.constructorParameterOrderOverride)) {
                        val fieldImplementation = field.implementation
                        if (field.isParameter || fieldImplementation is AbstractField.ImplementationStrategy.ForwardValueToParent && fieldImplementation.defaultValue == null) {
                            printPropertyDeclaration(field.name, field.typeRef, VariableKind.PARAMETER, inConstructor = true)
                            println()
                        } else if (fieldImplementation is AbstractField.ImplementationStrategy.RegularField && fieldImplementation.defaultValue == null) {
                            fieldPrinter.printField(
                                field,
                                inImplementation = false,
                                inConstructor = true,
                                override = true,
                            )
                        }
                    }
                }
                print(")")
            }

            print(" : ")
            if (implementation.needPureAbstractElement) {
                print(pureAbstractElementType.render(), "(), ")
            } else {
                val element = implementation.element
                print(element.render())

                if (element.element.kind!!.typeKind == TypeKind.Class) {
                    print("(")
                    println()
                    withIndent {
                        for (field in implementation.allFields) {
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
                }
            }

            printBlock {
                var index = 0
                for (field in implementation.allFields) {
                    val fieldImplementation = field.implementation
                    if (fieldImplementation is AbstractField.ImplementationStrategy.LateinitField
                        || fieldImplementation is AbstractField.ImplementationStrategy.ComputedProperty
                        || fieldImplementation is AbstractField.ImplementationStrategy.RegularField && fieldImplementation.defaultValue != null
                    ) {
                        if (separateFieldsWithBlankLine && index++ > 0) println()
                        fieldPrinter.printField(
                            field,
                            inImplementation = false,
                            override = true,
                        )
                    }
                }

                printAdditionalMethods(implementation)
            }
        }
    }
}