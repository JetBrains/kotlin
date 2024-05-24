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

abstract class AbstractImplementationPrinter<Implementation, Element, ImplementationField>(
    private val printer: ImportCollectingPrinter,
)
        where Implementation : AbstractImplementation<Implementation, Element, ImplementationField>,
              Element : AbstractElement<Element, *, Implementation>,
              ImplementationField : AbstractField<*> {

    protected abstract val implementationOptInAnnotation: ClassRef<*>

    protected abstract fun getPureAbstractElementType(implementation: Implementation): ClassRef<*>

    protected open val separateFieldsWithBlankLine: Boolean
        get() = false

    protected open fun ImportCollecting.parentConstructorArguments(implementation: Implementation): List<String> =
        emptyList()

    protected abstract fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<ImplementationField>

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
            print(implementation.element.params.typeParameters(end = " "))

            val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
            val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass

            val fieldPrinter = makeFieldPrinter(this)

            val additionalConstructorParameters = additionalConstructorParameters(implementation)
            if (!isInterface &&
                !isAbstract &&
                (implementation.fieldsInConstructor.isNotEmpty() || additionalConstructorParameters.isNotEmpty())
            ) {
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
                    implementation.fieldsInConstructor
                        .reorderFieldsIfNecessary(implementation.constructorParameterOrderOverride)
                        .forEachIndexed { _, field ->
                            if (field.isParameter) {
                                print(field.name, ": ", field.typeRef.render())
                                println(",")
                            } else if (!field.isFinal) {
                                fieldPrinter.printField(field, inImplementation = true, override = true, inConstructor = true)
                            }
                        }
                }
                print(")")
            }

            val parentRefs = listOfNotNull(getPureAbstractElementType(implementation).takeIf { implementation.needPureAbstractElement }) +
                    implementation.allParents.map { it.withSelfArgs() }
            printInheritanceClause(parentRefs, parentConstructorArguments(implementation))
            val printer = SmartPrinter(StringBuilder())
            withNewPrinter(printer) {
                val bodyFieldPrinter = makeFieldPrinter(this)
                withIndent {
                    val fields = if (isInterface || isAbstract) implementation.allFields
                    else implementation.fieldsInBody
                    fields.forEachIndexed { index, field ->
                        if (index > 0 && separateFieldsWithBlankLine) {
                            println()
                        }
                        bodyFieldPrinter.printField(
                            field,
                            inImplementation = true,
                            override = true,
                            modality = Modality.ABSTRACT.takeIf { isAbstract }
                        )
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