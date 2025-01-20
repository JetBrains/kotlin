/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.SimpleField
import org.jetbrains.kotlin.utils.withIndent
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

private val transformIfNeeded = ArbitraryImportable("$BASE_PACKAGE.util", "transformIfNeeded")
private val transformInPlace = ArbitraryImportable("$BASE_PACKAGE.util", "transformInPlace")

internal class ElementPrinter(printer: ImportCollectingPrinter) : AbstractElementPrinter<Element, Field>(printer) {

    override fun makeFieldPrinter(printer: ImportCollectingPrinter) = object : AbstractFieldPrinter<Field>(printer) {
        override fun forceMutable(field: Field) = field.isMutable
    }

    override val separateFieldsWithBlankLine: Boolean
        get() = true

    // In IR classes we only print fields that are either declared in this element, or refine the type of a parent field
    // and thus need an override.
    override fun filterFields(element: Element): Collection<Field> =
        element.fields

    private fun ImportCollectingPrinter.printAcceptChildrenMethodImplementation(element: Element, hasData: Boolean) {
        if (!element.isRootElement) {
            printBlock {
                for (child in element.walkableChildren) {
                    print(child.name, child.call())
                    when (child) {
                        is SimpleField -> if (hasData) {
                            println("accept(visitor, data)")
                        } else {
                            println("acceptVoid(visitor)")
                        }
                        is ListField -> {
                            print("forEach { it")
                            if (child.baseType.nullable) {
                                print("?")
                            }
                            if (hasData) {
                                println(".accept(visitor, data) }")
                            } else {
                                println(".acceptVoid(visitor) }")
                            }
                        }
                    }
                }
            }
        } else {
            println()
        }
    }

    private fun ImportCollectingPrinter.castIfNeeded(from: ElementOrRef<Element>, to: ElementOrRef<Element>) {
        if (!from.element.isSubclassOf(to.element)) {
            print(" as ", to.render())
        }
    }

    private fun ImportCollectingPrinter.printTransformChildrenMethodImplementation(element: Element, hasData: Boolean) {
        if (!element.isRootElement) {
            printBlock {
                for (child in element.transformableChildren) {
                    print(child.name)
                    when (child) {
                        is SimpleField -> {
                            print(" = ", child.name, child.call())
                            if (hasData) {
                                print("transform(transformer, data)")
                            } else {
                                print("transformVoid(transformer)")
                            }
                            @Suppress("UNCHECKED_CAST")
                            val elementRef = child.typeRef as GenericElementRef<Element>
                            castIfNeeded(elementRef.element.transformMethodReturnType, elementRef)
                            println()
                        }
                        is ListField -> {
                            if (child.isMutable) {
                                print(" = ", child.name, child.call())
                                addImport(transformIfNeeded)
                                print("transformIfNeeded(transformer")
                            } else {
                                addImport(transformInPlace)
                                print(child.call())
                                print("transformInPlace(transformer")
                            }
                            if (hasData) {
                                print(", data")
                            }
                            println(")")
                        }
                    }
                }
            }
        } else {
            println()
        }
    }

    override fun ImportCollectingPrinter.printAdditionalMethods(element: Element) {
        element.generationCallback?.invoke(this)

        printAcceptMethod(
            element = element,
            visitorClass = irVisitorType,
            hasImplementation = !element.isRootElement,
            treeName = "IR",
        )

        var acceptMethodHasBody = false
        if (element.hasAcceptMethod) {
            println()
            val visitorParameter = FunctionParameter("visitor", irVisitorVoidType)
            printFunctionDeclaration(
                name = "acceptVoid",
                parameters = listOf(visitorParameter),
                returnType = StandardTypes.unit,
                override = !element.isRootElement,
            )
            if (!element.isRootElement) {
                acceptMethodHasBody = true
                printBlock {
                    println(visitorParameter.name, ".", element.visitFunctionName, "(this)")
                }
            } else {
                println()
            }
        }

        if (element.hasTransformMethod) {
            println()
            val dataTP = TypeVariable("D")
            val dataParameter = FunctionParameter("data", dataTP)
            val transformerParameter = FunctionParameter("transformer", irTransformerType.withArgs(dataTP))
            if (element.isRootElement) {
                printKDoc(transformMethodKDoc(transformerParameter, dataParameter, "IR"))
            }
            val transformMethodModality = when {
                acceptMethodHasBody || element.kind!!.isInterface -> null
                else -> Modality.ABSTRACT
            }
            printFunctionDeclaration(
                name = "transform",
                parameters = listOf(transformerParameter, dataParameter),
                returnType = element.transformMethodReturnType,
                typeParameters = listOf(dataTP),
                modality = transformMethodModality,
                override = !element.isRootElement,
            )
            if (acceptMethodHasBody) {
                println(" =")
                withIndent {
                    print("transformer.", element.visitFunctionName, "(this, data)")
                    castIfNeeded(element.getTransformExplicitType(), element.transformMethodReturnType)
                    println()
                }
            } else {
                println()
            }

            println()
            val transformerVoidParameter = FunctionParameter("transformer", elementTransformerVoidType)
            printFunctionDeclaration(
                name = "transformVoid",
                parameters = listOf(transformerVoidParameter),
                returnType = element.transformMethodReturnType,
                modality = transformMethodModality,
                override = !element.isRootElement,
            )
            if (acceptMethodHasBody) {
                println(" =")
                withIndent {
                    print("transformer.", element.visitFunctionName, "(this)")
                    castIfNeeded(element.getTransformExplicitType(), element.transformMethodReturnType)
                    println()
                }
            } else {
                println()
            }
        }

        if (element.hasAcceptChildrenMethod) {
            printAcceptChildrenMethod(
                element = element,
                visitorClass = irVisitorType,
                visitorResultType = StandardTypes.unit,
                override = !element.isRootElement,
            )
            printAcceptChildrenMethodImplementation(element, hasData = true)

            println()
            val visitorParameter = FunctionParameter("visitor", irVisitorVoidType)
            printFunctionDeclaration(
                name = "acceptChildrenVoid",
                parameters = listOf(visitorParameter),
                returnType = StandardTypes.unit,
                typeParameters = emptyList(),
                override = !element.isRootElement,
            )
            printAcceptChildrenMethodImplementation(element, hasData = false)
        }

        if (element.hasTransformChildrenMethod) {
            printTransformChildrenMethod(
                element = element,
                transformerClass = irTransformerType,
                returnType = StandardTypes.unit,
                override = !element.isRootElement,
            )
            printTransformChildrenMethodImplementation(element, hasData = true)

            println()
            printFunctionDeclaration(
                name = "transformChildrenVoid",
                parameters = listOf(FunctionParameter("transformer", elementTransformerVoidType)),
                returnType = StandardTypes.unit,
                override = !element.isRootElement,
            )
            printTransformChildrenMethodImplementation(element, hasData = false)
        }
    }
}
