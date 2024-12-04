/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.elementVisitorVoidType
import org.jetbrains.kotlin.ir.generator.irSimpleTypeType
import org.jetbrains.kotlin.ir.generator.irTypeProjectionType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.typeVisitorType

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
        printVisitTypeKDoc()
        printVisitTypeMethod(name = "visitType", hasDataParameter = false, modality = Modality.ABSTRACT, override = false)
        println()
        println()
        printVisitTypeMethod(name = "visitType", hasDataParameter = true, modality = Modality.FINAL, override = true)
        printBlock {
            println("visitType(container, type)")
        }
        println()
        printVisitTypeRecursivelyKDoc()
        printVisitTypeMethod(name = "visitTypeRecursively", hasDataParameter = false, modality = Modality.OPEN, override = false)
        printBlock {
            printlnMultiLine(
                """
                visitType(container, type)
                if (type is ${irSimpleTypeType.render()}) {
                    type.arguments.forEach {
                        if (it is ${irTypeProjectionType.render()}) {
                            visitTypeRecursively(container, it.type)
                        }
                    }
                }
                """
            )
        }
        println()
        printVisitTypeMethod(name = "visitTypeRecursively", hasDataParameter = true, modality = Modality.FINAL, override = true)
        printBlock {
            println("visitTypeRecursively(container, type)")
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
                )
                println("super<", elementVisitorVoidType.render(), ">.", element.visitFunctionName, "(", element.visitorParameterName, ")")
            }
        }
    }
}
