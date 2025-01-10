/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.irVisitorType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.utils.withIndent

internal class TransformerPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    private val rootElement: Element,
) : AbstractTransformerPrinter<Element, Field>(printer) {

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(irVisitorType.withArgs(rootElement, dataTypeVariable))

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = element.getTransformExplicitType()

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        printRootTransformMethodDeclaration(rootElement, Modality.OPEN, hasDataParameter = true)
        printBlock {
            println(rootElement.visitorParameterName, ".transformChildren(this, data)")
            println("return ", rootElement.visitorParameterName)
        }
    }

    override fun printMethodsForElement(element: Element) {
        printer.run {
            val parent = element.parentInVisitor
            if (element.transformByChildren || parent != null) {
                println()
                printVisitMethodDeclaration(
                    element = element,
                    override = true,
                )
                if (element.transformByChildren) {
                    println(" =")
                    withIndent {
                        println("transformElement(", element.visitorParameterName, ", data)")
                    }
                } else {
                    println(" =")
                    withIndent {
                        println(parent!!.visitFunctionName, "(", element.visitorParameterName, ", data)")
                    }
                }
            }
        }
    }
}
