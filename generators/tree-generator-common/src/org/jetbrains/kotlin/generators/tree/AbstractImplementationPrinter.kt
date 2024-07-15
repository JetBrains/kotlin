/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.withIndent

abstract class AbstractImplementationPrinter<Implementation, Element, Field>(
    private val printer: ImportCollectingPrinter,
)
        where Implementation : AbstractImplementation<Implementation, Element, Field>,
              Element : AbstractElement<Element, Field, Implementation>,
              Field : AbstractField<Field> {


    protected abstract val implementationOptInAnnotation: ClassRef<*>

    protected abstract fun getPureAbstractElementType(implementation: Implementation): ClassRef<*>

    protected open val separateFieldsWithBlankLine: Boolean
        get() = false

    protected open fun ImportCollecting.parentConstructorArguments(implementation: Implementation): List<String> =
        emptyList()

    protected abstract fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field>

    protected open fun ImportCollectingPrinter.printAdditionalMethods(implementation: Implementation) {
    }

    protected open fun additionalConstructorParameters(implementation: Implementation): List<FunctionParameter> = emptyList()

    fun printImplementation(implementation: Implementation) {
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
            print(implementation.element.params.typeParameters())

            val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
            val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass

            val fieldPrinter = makeFieldPrinter(this)

            val additionalConstructorParameters = additionalConstructorParameters(implementation)
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
                    for (parameter in additionalConstructorParameters) {
                        println(parameter.render(this), ",")
                    }

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
                print(getPureAbstractElementType(implementation).render(), "(), ")
            } else {
                val element = implementation.element
                print(element.withSelfArgs().render())

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

            val printer = SmartPrinter(StringBuilder())
            withNewPrinter(printer) {
                val bodyFieldPrinter = makeFieldPrinter(this)
                withIndent {
                    var index = 0
                    for (field in implementation.allFields) {
                        val fieldImplementation = field.implementation
                        if (fieldImplementation is AbstractField.ImplementationStrategy.LateinitField
                            || fieldImplementation is AbstractField.ImplementationStrategy.ComputedProperty
                            || fieldImplementation is AbstractField.ImplementationStrategy.RegularField && fieldImplementation.defaultValue != null
                        ) {
                            if (separateFieldsWithBlankLine && index++ > 0) println()
                            bodyFieldPrinter.printField(
                                field,
                                inImplementation = false,
                                override = true,
                            )
                        }
                    }

                    printAdditionalMethods(implementation)
                }
            }
            val body = printer.toString()
            if (body.isNotEmpty()) {
                println(" {")
                print(body)
                println("}")
            } else {
                println()
            }
            addAllImports(implementation.additionalImports)
        }
    }
}