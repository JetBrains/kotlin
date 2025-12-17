/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.AbstractElementPrinter
import org.jetbrains.kotlin.generators.tree.AbstractFieldPrinter
import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field

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
            visitorClass = irVisitorType,
            hasImplementation = !element.isRootElement,
            treeName = "IR",
        )

        printAcceptVoidMethod(
            element = element,
            visitorClass = irVisitorVoidType,
            hasImplementation = !element.isRootElement,
            treeName = "IR",
        )

        printTransformMethod(
            element = element,
            transformerClass = irTransformerType,
            implementation = "accept(transformer, data)".takeIf { !element.isRootElement },
            returnType = element,
            treeName = "IR",
        )

        printTransformVoidMethod(
            element = element,
            transformerType = elementTransformerVoidType,
            hasImplementation = !element.isRootElement,
            treeName = "IR",
        )

        if (element.hasAcceptChildrenMethod) {
            printAcceptChildrenMethod(
                element = element,
                visitorClass = irVisitorType,
                visitorResultType = StandardTypes.unit,
                override = !element.isRootElement,
            )
            printAcceptChildrenBody(element)

            printAcceptChildrenVoidMethod(
                element = element,
                visitorClass = irVisitorVoidType,
                override = !element.isRootElement,
            )
            printAcceptChildrenBody(element, isVoid = true)
        }

        if (element.hasTransformChildrenMethod) {
            printTransformChildrenMethod(
                element = element,
                transformerClass = irTransformerType,
                returnType = StandardTypes.unit,
                override = !element.isRootElement,
            )
            printTransformChildrenBody(element)

            printTransformChildrenVoidMethod(
                element = element,
                transformerClass = elementTransformerVoidType,
                override = !element.isRootElement,
            )
            printTransformChildrenBody(element, isVoid = true)
        }
    }
}
