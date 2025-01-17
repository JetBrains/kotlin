/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.AbstractElementPrinter
import org.jetbrains.kotlin.generators.tree.AbstractFieldPrinter
import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.generators.tree.TypeVariable
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.nullable
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.generators.tree.withArgs
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.BASE_PACKAGE
import org.jetbrains.kotlin.ir.generator.elementTransformerVoidType
import org.jetbrains.kotlin.ir.generator.irTransformerType
import org.jetbrains.kotlin.ir.generator.irVisitorType
import org.jetbrains.kotlin.ir.generator.irVisitorVoidType
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
                            val elementRef = child.typeRef as GenericElementRef<*>
                            if (!elementRef.element.hasTransformMethod) {
                                print(" as ", elementRef.render())
                            }
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
            printFunctionDeclaration(
                name = "transform",
                parameters = listOf(transformerParameter, dataParameter),
                returnType = element,
                typeParameters = listOf(dataTP),
                override = !element.isRootElement,
            )
            if (!element.isRootElement) {
                println(" =")
                withIndent {
                    print("accept(transformer, data)")
                    print(" as ", element.render())
                }
            }
            println()
            println()
            val transformerVoidParameter = FunctionParameter("transformer", elementTransformerVoidType)
            printFunctionDeclaration(
                name = "transformVoid",
                parameters = listOf(transformerVoidParameter),
                returnType = element,
                override = !element.isRootElement,
            )
            if (!element.isRootElement) {
                println(" =")
                withIndent {
                    print("transform(transformer, null)")
                    if (element.getTransformExplicitType() != element) {
                        print(" as ", element.render())
                    }
                }
            }
            println()
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
