/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.typeTransformerType
import org.jetbrains.kotlin.utils.withIndent

internal class TypeTransformerVoidPrinter(
    printer: ImportCollectingPrinter,
    visitorType: ClassRef<*>,
    rootElement: Element,
) : TypeTransformerPrinter(printer, visitorType, rootElement) {

    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(typeTransformerType.withArgs(StandardTypes.unit, visitorDataType))

    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override fun visitMethodReturnType(element: Element): TypeRef = StandardTypes.unit

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        printTransformTypeMethod("transformTypeRecursively", hasDataParameter = false, modality = Modality.ABSTRACT, override = false)
        println()
        println()
        printTransformTypeMethod("transformTypeRecursively", hasDataParameter = true, modality = Modality.FINAL, override = true)
        println(" =")
        withIndent {
            println("transformTypeRecursively(container, type)")
        }
    }

    override fun printMethodsForElement(element: Element) {
        if (element.parentInVisitor == null && !element.isRootElement) return
        val irTypeFields = element.getFieldsWithIrTypeType()
        printer.run {
            println()
            printVisitMethodDeclaration(element, hasDataParameter = true, modality = Modality.FINAL, override = true)
            printBlock {
                println(element.visitFunctionName, "(", element.visitorParameterName, ")")
            }
            println()
            printVisitMethodDeclaration(element, hasDataParameter = false, modality = Modality.OPEN, override = false)
            printBlock {
                printTypeRemappings(
                    element,
                    irTypeFields,
                    hasDataParameter = false,
                    transformTypes = true,
                )
                element.parentInVisitor?.let {
                    println(it.visitFunctionName, "(", element.visitorParameterName, ")")
                }
                if (element.isRootElement) {
                    println(element.visitorParameterName, ".acceptChildrenVoid(this)")
                }
            }
        }
    }
}
