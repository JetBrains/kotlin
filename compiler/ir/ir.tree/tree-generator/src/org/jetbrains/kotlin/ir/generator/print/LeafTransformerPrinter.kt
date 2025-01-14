/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printAnnotation
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.irLeafVisitorType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.utils.withIndent

internal class LeafTransformerPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    private val rootElement: Element,
) : AbstractTransformerPrinter<Element, Field>(printer) {

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(irLeafVisitorType.withArgs(rootElement, dataTypeVariable))

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = element.getTransformExplicitType()

    override fun skipElement(element: Element): Boolean =
        !element.hasAcceptMethod

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        printRootTransformMethodDeclaration(rootElement, Modality.OPEN, hasDataParameter = true)
        printBlock {
            println(rootElement.visitorParameterName, ".transformChildren(this, data)")
            println("return ", rootElement.visitorParameterName)
        }
    }

    override fun parentInVisitor(element: Element): Element? =
        element.parentInLeafOnlyVisitor(rootElement)

    override fun printMethodsForElement(element: Element) {
        printer.run {
            val parent = parentInVisitor(element)
            if (element.transformByChildren || parent != null) {
                println()
                if (element.isRootElement) {
                    printAnnotation(
                        Deprecated(
                            message = "Call transformElement instead",
                            replaceWith = ReplaceWith("transformElement(${element.visitorParameterName}, data)"),
                            level = DeprecationLevel.ERROR,
                        )
                    )
                }
                printVisitMethodDeclaration(
                    element = element,
                    // visitElement is final because it's not called from anywhere and thus is not supposed to be overridden.
                    modality = Modality.FINAL.takeIf { element.isRootElement },
                    override = true,
                )
                println(" =")
                withIndent {
                    if (element.transformByChildren || parent?.isRootElement == true) {
                        println("transformElement(", element.visitorParameterName, ", data)")
                    } else {
                        println(parent!!.visitFunctionName, "(", element.visitorParameterName, ", data)")
                    }
                }
            }
        }
    }
}
