/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.AbstractElementPrinter
import org.jetbrains.kotlin.generators.tree.AbstractFieldPrinter
import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.nullable
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.BASE_PACKAGE
import org.jetbrains.kotlin.ir.generator.elementTransformerType
import org.jetbrains.kotlin.ir.generator.elementVisitorType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.SimpleField
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

    override fun ImportCollectingPrinter.printAdditionalMethods(element: Element) {
        element.generationCallback?.invoke(this)

        printAcceptMethod(
            element = element,
            visitorClass = elementVisitorType,
            hasImplementation = !element.isRootElement,
            treeName = "IR",
        )

        printTransformMethod(
            element = element,
            transformerClass = elementTransformerType,
            implementation = "accept(transformer, data)".takeIf { !element.isRootElement },
            returnType = element,
            treeName = "IR",
        )

        if (element.hasAcceptChildrenMethod) {
            printAcceptChildrenMethod(
                element = element,
                visitorClass = elementVisitorType,
                visitorResultType = StandardTypes.unit,
                override = !element.isRootElement,
            )

            if (!element.isRootElement) {
                printBlock {
                    for (child in element.walkableChildren) {
                        print(child.name, child.call())
                        when (child) {
                            is SimpleField -> println("accept(visitor, data)")
                            is ListField -> {
                                print("forEach { it")
                                if (child.baseType.nullable) {
                                    print("?")
                                }
                                println(".accept(visitor, data) }")
                            }
                        }
                    }
                }
            } else {
                println()
            }
        }

        if (element.hasTransformChildrenMethod) {
            printTransformChildrenMethod(
                element = element,
                transformerClass = elementTransformerType,
                returnType = StandardTypes.unit,
                override = !element.isRootElement,
            )
            if (!element.isRootElement) {
                printBlock {
                    for (child in element.transformableChildren) {
                        print(child.name)
                        when (child) {
                            is SimpleField -> {
                                print(" = ", child.name, child.call())
                                print("transform(transformer, data)")
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
                                    println("transformIfNeeded(transformer, data)")
                                } else {
                                    addImport(transformInPlace)
                                    print(child.call())
                                    println("transformInPlace(transformer, data)")
                                }
                            }
                        }
                    }
                }
            } else {
                println()
            }
        }
    }
}
