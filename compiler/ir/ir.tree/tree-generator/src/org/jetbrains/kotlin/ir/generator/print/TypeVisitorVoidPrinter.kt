/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.ir.generator.elementVisitorVoidType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.typeVisitorType
import org.jetbrains.kotlin.utils.withIndent

internal class TypeVisitorVoidPrinter(
    printer: ImportCollectingPrinter,
    visitorType: ClassRef<*>,
    rootElement: Element,
) : TypeVisitorPrinter(printer, visitorType, rootElement) {
    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(
            typeVisitorType.withArgs(StandardTypes.unit, visitorDataType),
            elementVisitorVoidType
        )

    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override fun visitMethodReturnType(element: Element): TypeRef = StandardTypes.unit

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        printVisitTypeMethod(hasDataParameter = false, modality = Modality.ABSTRACT, override = false)
        println()
        println()
        printVisitTypeMethod(hasDataParameter = true, modality = Modality.FINAL, override = true)
        printBlock {
            println("visitType(container, type)")
        }
    }

    override fun printMethodsForElement(element: Element) {
        val irTypeFields = element.getFieldsWithIrTypeType()
        if (irTypeFields.isEmpty()) return
        if (element.parentInVisitor == null) return
        printer.run {
            println()
            printVisitMethodDeclaration(element, hasDataParameter = true, modality = Modality.FINAL, override = true)
            printBlock {
                println(element.visitFunctionName, "(", element.visitorParameterName, ")")
            }
            println()
            printVisitMethodDeclaration(element, hasDataParameter = false, override = true)
            printBlock {
                printTypeRemappings(
                    element,
                    irTypeFields,
                    hasDataParameter = false,
                    replaceTypes = false,
                    visitTypeMethodName = "visitType"
                )
                println("super<", elementVisitorVoidType.render(), ">.", element.visitFunctionName, "(", element.visitorParameterName, ")")
            }
        }
    }
}