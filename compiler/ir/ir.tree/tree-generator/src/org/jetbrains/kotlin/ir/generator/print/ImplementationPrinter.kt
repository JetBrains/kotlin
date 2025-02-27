/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.isSubclassOf
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.Packages
import org.jetbrains.kotlin.ir.generator.irElementConstructorIndicatorType
import org.jetbrains.kotlin.ir.generator.irImplementationDetailType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Implementation
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.withIndent

internal class ImplementationPrinter(
    private val printer: ImportCollectingPrinter,
) {
    fun getPureAbstractElementType(implementation: Implementation): ClassRef<*> =
        org.jetbrains.kotlin.ir.generator.elementBaseType

    val implementationOptInAnnotation: ClassRef<*>
        get() = irImplementationDetailType

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

            val additionalConstructorParameters = if (implementation.hasConstructorIndicator) {
                listOf(
                    FunctionParameter(
                        "constructorIndicator",
                        irElementConstructorIndicatorType.copy(nullable = true),
                        markAsUnused = true
                    )
                )
            } else {
                emptyList()
            }
            if (implementation.fieldsInConstructor.isNotEmpty() || additionalConstructorParameters.isNotEmpty()) {
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
                                printer.printPropertyDeclaration(
                                    name = field.name,
                                    type = field.typeRef,
                                    kind = VariableKind.PARAMETER,
                                    inConstructor = true,
                                    optInAnnotation = field.optInAnnotation
                                )
                                println()
                            }
                        }
                }
                print(")")
            }

            val parentRefs = listOfNotNull(getPureAbstractElementType(implementation).takeIf { implementation.needPureAbstractElement }) +
                    implementation.allParents.map { it.withSelfArgs() }
            printInheritanceClause(parentRefs)
            printBlock {
                for (field in implementation.allFields) {
                    printer.printPropertyDeclaration(
                        name = field.name,
                        field.typeRef,
                        kind = if (field.isMutable) VariableKind.VAR else VariableKind.VAL,
                        optInAnnotation = field.optInAnnotation,
                        override = true,
                    )

                    val defaultValue = field.implementationDefaultStrategy as? AbstractField.ImplementationDefaultStrategy.DefaultValue
                    if (defaultValue != null && defaultValue.withGetter) {
                        println()
                        withIndent {
                            println("get() = ${defaultValue.defaultValue}")
                            field.customSetter?.let {
                                print("set(value)")
                                printBlock {
                                    printlnMultiLine(it)
                                }
                            }
                        }
                    } else {
                        print(" by ${field.name}Attribute")
                    }
                    println()
                }

                println()
                printBlock("init") {
                    if (implementation.preallocateStorageSize != -1) {
                        println("preallocateStorage(${implementation.preallocateStorageSize})")

                        for (field in implementation.allFields.sortedBy { it.id }) {
                            val impl = field.implementationDefaultStrategy!!
                            if (impl is AbstractField.ImplementationDefaultStrategy.Required) {
                                if (field.typeRef == StandardTypes.boolean) {
                                    println("if (${field.name}) setFlagInternal(${field.name}Attribute, true)")
                                } else {
                                    println("initAttribute(${field.name}Attribute, ${field.name})")
                                }
                            } else if (
                                impl is AbstractField.ImplementationDefaultStrategy.DefaultValue &&
                                !impl.withGetter &&
                                !field.useSharedDefaultValues &&
                                field.typeRef != StandardTypes.boolean
                            ) {
                                println("initAttribute(${field.name}Attribute, ${impl.defaultValue})")
                            }
                        }
                    }

                    if (implementation.element.isSubclassOf<Element>(IrTree.symbolOwner) && implementation.bindOwnedSymbol) {
                        val symbolField = implementation.getOrNull("symbol")
                        if (symbolField != null) {
                            println()
                            println("${symbolField.name}.bind(this)")
                        }
                    }
                }

                printBlock("companion object") {
                    for (field in implementation.allFields) {
                        val defaultValue = field.implementationDefaultStrategy as? AbstractField.ImplementationDefaultStrategy.DefaultValue
                        if (defaultValue == null || !defaultValue.withGetter) {
                            print(
                                "@JvmStatic private val ${field.name}Attribute = ${
                                    type(
                                        Packages.tree,
                                        "IrIndexBasedAttributeRegistry"
                                    ).render()
                                }."
                            )
                            print(if (field.typeRef == StandardTypes.boolean) "createFlag" else "createAttr")
                            if (field.typeRef != StandardTypes.boolean) {
                                print("<${field.typeRef.render()}>")
                            }
                            print("(${implementation.typeName}::class.java, ${field.id}, \"${field.name}\"")
                            if (field.typeRef != StandardTypes.boolean) {
                                print(", ")
                                print(defaultValue?.defaultValue?.takeIf { field.useSharedDefaultValues } ?: "null")
                            }
                            println(")")
                        }
                    }

                    if (implementation.generationCallbackInCompanion) implementation.generationCallback?.invoke(this)
                }

                if (!implementation.generationCallbackInCompanion) implementation.generationCallback?.invoke(this)
            }

            addAllImports(implementation.additionalImports)
        }
    }
}
