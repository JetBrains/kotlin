/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.braces
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.typeParameters
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.withIndent

abstract class AbstractImplementationPrinter<Implementation, Element, ImplementationField>(
    private val printer: SmartPrinter,
)
        where Implementation : AbstractImplementation<Implementation, Element, ImplementationField>,
              Element : AbstractElement<Element, *, Implementation>,
              ImplementationField : AbstractField<*>,
              ImplementationField : AbstractFieldWithDefaultValue<*> {

    protected abstract val implementationOptInAnnotation: ClassRef<*>

    protected abstract val pureAbstractElementType: ClassRef<*>

    protected abstract fun makeFieldPrinter(printer: SmartPrinter): AbstractFieldPrinter<ImplementationField>

    context(ImportCollector)
    protected open fun SmartPrinter.printAdditionalMethods(implementation: Implementation) {}

    context(ImportCollector)
    fun printImplementation(implementation: Implementation) {
        addAllImports(implementation.additionalImports)
        printer.run {
            buildSet {
                if (implementation.requiresOptIn) {
                    add(implementationOptInAnnotation)
                }

                for (field in implementation.fieldsWithoutDefault) {
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

            if (!isInterface && !isAbstract && implementation.fieldsWithoutDefault.isNotEmpty()) {
                if (implementation.isPublic) {
                    print(" @", implementationOptInAnnotation.render(), " constructor")
                }
                println("(")
                withIndent {
                    implementation.fieldsWithoutDefault.forEachIndexed { _, field ->
                        if (field.isParameter) {
                            print(field.name, ": ", field.typeRef.render())
                            println(",")
                        } else if (!field.isFinal) {
                            fieldPrinter.printField(field, override = true, inConstructor = true)
                        }
                    }
                }
                print(")")
            }

            print(" : ")
            if (implementation.needPureAbstractElement) {
                print(pureAbstractElementType.render(), "(), ")
            }
            print(implementation.allParents.joinToString { "${it.render()}${it.kind.braces()}" })
            printBlock {
                if (isInterface || isAbstract) {
                    implementation.allFields.forEach {
                        fieldPrinter.printField(it, override = true, modality = Modality.ABSTRACT.takeIf { isAbstract })
                    }
                } else {
                    implementation.fieldsWithDefault.forEach {
                        fieldPrinter.printField(it, override = true)
                    }
                }

                printAdditionalMethods(implementation)
            }
        }
    }
}