/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.firVisitorType
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.generators.tree.printer.printFunctionWithBlockBody
import org.jetbrains.kotlin.utils.SmartPrinter

internal class TransformerPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer) {

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = firVisitorType.withArgs(AbstractFirTreeBuilder.baseFirElement, visitorDataType)

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = element.transformerClass

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    context(ImportCollector)
    override fun printMethodsForElement(element: Element) {
        printer.run {
            println()
            val elementParameterName = element.safeDecapitalizedName
            if (element.isRootElement) {
                val elementTP = TypeVariable("E", listOf(element))
                printFunctionDeclaration(
                    name = "transformElement",
                    parameters = listOf(
                        FunctionParameter(elementParameterName, elementTP),
                        FunctionParameter("data", dataTypeVariable)
                    ),
                    returnType = elementTP,
                    typeParameters = listOf(elementTP),
                    modality = Modality.ABSTRACT,
                )
                println()
            } else {
                printFunctionWithBlockBody(
                    name = "transform" + element.name,
                    parameters = listOf(
                        FunctionParameter(elementParameterName, element),
                        FunctionParameter("data", dataTypeVariable)
                    ),
                    returnType = visitMethodReturnType(element),
                    typeParameters = element.params,
                    modality = Modality.OPEN,
                ) {
                    println("return transformElement(", elementParameterName, ", data)")
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
                    element.safeDecapitalizedName,
                    ", ",
                    "data)"
                )
            }
        }
    }
}
