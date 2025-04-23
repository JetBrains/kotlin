/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.irSimpleTypeType
import org.jetbrains.kotlin.ir.generator.irTypeProjectionType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.typeVisitorType

internal open class TypeVisitorVoidPrinter(
    printer: ImportCollectingPrinter,
    visitorType: ClassRef<*>,
    rootElement: Element,
) : TypeVisitorPrinter(printer, visitorType, rootElement) {
    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(typeVisitorType.withArgs(StandardTypes.unit, visitorDataType))

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

        println()
        printlnMultiLine(
            """
            final override fun visitAnnotationUsage(annotation: IrConstructorCall, data: Nothing?) {
                visitAnnotationUsage(annotation)
            }

            open fun visitAnnotationUsage(annotation: IrConstructorCall) {
                visitElement(annotation)
            }
            """
        )
    }

    override fun printMethodsForElement(element: Element) {
        if (element.parentInVisitor == null && !element.isRootElement) return
        val irTypeFields = element.getFieldsWithIrTypeType()
        printer.run {
            if (shouldPrintVisitWithDataMethod()) {
                println()
                printVisitMethodDeclaration(element, hasDataParameter = true, modality = Modality.FINAL, override = true)
                printBlock {
                    println(element.visitFunctionName, "(", element.visitorParameterName, ")")
                }
            }
            println()
            printVisitVoidMethodDeclaration(element)
            printBlock {
                printTypeRemappings(
                    element,
                    irTypeFields,
                    hasDataParameter = false,
                )
                element.parentInVisitor?.let {
                    println(it.visitFunctionName, "(", element.visitorParameterName, ")")
                }
                if (element.isRootElement) {
                    println(element.visitorParameterName, ".acceptChildrenVoid(this)")
                    addImport(ArbitraryImportable("org.jetbrains.kotlin.ir.visitors", "acceptChildrenVoid"))
                }
            }
        }
    }

    protected open fun shouldPrintVisitWithDataMethod(): Boolean = true
    protected open fun printVisitVoidMethodDeclaration(element: Element): Unit = with(printer) {
        printVisitMethodDeclaration(element, hasDataParameter = false, modality = Modality.OPEN, override = false)
    }
}
