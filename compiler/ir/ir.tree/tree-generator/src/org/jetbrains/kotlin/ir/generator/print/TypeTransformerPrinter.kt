/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.elementVisitorType
import org.jetbrains.kotlin.ir.generator.irTypeType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.SimpleField
import org.jetbrains.kotlin.utils.withIndent

internal open class TypeTransformerPrinter(
    printer: ImportCollectingPrinter,
    visitorType: ClassRef<*>,
    rootElement: Element,
) : TypeVisitorPrinter(printer, visitorType, rootElement) {

    protected fun ImportCollectingPrinter.printTransformTypeMethod(hasDataParameter: Boolean, modality: Modality?, override: Boolean) {
        val typeTP = TypeVariable("Type", listOf(irTypeType.copy(nullable = true)))
        printFunctionDeclaration(
            name = "transformType",
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
        printTransformTypeMethod(hasDataParameter = true, modality = null, override = false)
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
                    replaceTypes = true,
                    visitTypeMethodName = "transformType",
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
