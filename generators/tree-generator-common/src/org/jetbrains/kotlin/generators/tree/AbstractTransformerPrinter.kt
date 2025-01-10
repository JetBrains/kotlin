/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.generators.tree.printer.printFunctionWithBlockBody
import org.jetbrains.kotlin.generators.util.printBlock

abstract class AbstractTransformerPrinter<Element : AbstractElement<Element, Field, *>, Field : AbstractField<Field>>(
    printer: ImportCollectingPrinter,
) : AbstractVisitorPrinter<Element, Field>(printer) {

    override fun visitMethodReturnType(element: Element) = element.transformerClass

    protected fun ImportCollectingPrinter.printRootTransformMethodDeclaration(
        element: Element,
        modality: Modality?,
        hasDataParameter: Boolean,
        override: Boolean = false,
    ) {
        val elementTP = TypeVariable("E", listOf(element))
        printFunctionDeclaration(
            name = "transformElement",
            parameters = listOfNotNull(
                FunctionParameter(element.visitorParameterName, elementTP),
                FunctionParameter("data", visitorDataType).takeIf { hasDataParameter },
            ),
            returnType = elementTP,
            typeParameters = listOf(elementTP),
            modality = modality,
            override = override,
        )
    }

    override fun printMethodsForElement(element: Element) {
        printer.run {
            println()
            val elementParameterName = element.visitorParameterName
            if (element.isRootElement) {
                this.printRootTransformMethodDeclaration(element, Modality.ABSTRACT, hasDataParameter = true)
                println()
            } else {
                val parentInVisitor = parentInVisitor(element) ?: return
                printFunctionWithBlockBody(
                    name = "transform" + element.name,
                    parameters = listOf(
                        FunctionParameter(elementParameterName, element.withSelfArgs()),
                        FunctionParameter("data", dataTypeVariable)
                    ),
                    returnType = visitMethodReturnType(element),
                    typeParameters = element.params,
                    modality = Modality.OPEN,
                ) {
                    println("return transform", parentInVisitor.name, "(", elementParameterName, ", data)")
                }
            }
            println()
            printVisitMethodDeclaration(
                element = element,
                modality = Modality.FINAL,
                override = true,
            )
            printBlock {
                println(
                    "return transform",
                    element.name,
                    "(",
                    element.visitorParameterName,
                    ", ",
                    "data)"
                )
            }
        }
    }
}
