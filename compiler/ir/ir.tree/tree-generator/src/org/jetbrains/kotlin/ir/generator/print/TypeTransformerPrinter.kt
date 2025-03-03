/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.TypeVariable
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.irTypeType
import org.jetbrains.kotlin.ir.generator.model.Element

internal open class TypeTransformerPrinter(
    printer: ImportCollectingPrinter,
    visitorType: ClassRef<*>,
    rootElement: Element,
) : TypeVisitorPrinter(printer, visitorType, rootElement) {

    protected fun ImportCollectingPrinter.printTransformTypeMethod(
        name: String,
        hasDataParameter: Boolean,
        modality: Modality?,
        override: Boolean,
    ) {
        val typeTP = TypeVariable("Type", listOf(irTypeType.copy(nullable = true)))
        printFunctionDeclaration(
            name = name,
            parameters = listOfNotNull(
                FunctionParameter("container", rootElement),
                FunctionParameter("type", typeTP),
                FunctionParameter("data", visitorDataType).takeIf { hasDataParameter },
            ),
            returnType = typeTP,
            typeParameters = listOf(typeTP),
            modality = modality,
            override = override,
        )
    }

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        printTransformTypeMethod(name = "transformTypeRecursively", hasDataParameter = true, modality = Modality.ABSTRACT, override = false)
        println()
    }

    override fun printMethodsForElement(element: Element) {
        val irTypeFields = element.getFieldsWithIrTypeType()
        if (irTypeFields.isEmpty()) return
        if (element.parentInVisitor == null) return
        printer.run {
            println()
            printVisitMethodDeclaration(
                element = element,
                override = true,
            )
            printBlock {
                printTypeRemappings(
                    element,
                    irTypeFields,
                    hasDataParameter = true,
                    transformTypes = true,
                )
                println(
                    "return super.",
                    element.visitFunctionName,
                    "(",
                    element.visitorParameterName,
                    ", data)"
                )
            }
        }
    }
}
