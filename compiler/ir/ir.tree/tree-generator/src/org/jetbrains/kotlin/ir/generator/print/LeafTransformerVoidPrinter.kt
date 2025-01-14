/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printFunctionWithBlockBody
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.irLeafTransformerType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.utils.withIndent

internal class LeafTransformerVoidPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    private val rootElement: Element,
) : AbstractTransformerVoidPrinter<Element, Field>(printer) {

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(irLeafTransformerType.withArgs(visitorDataType))

    override fun visitMethodReturnType(element: Element): Element =
        when {
            element.transformByChildren -> element.getTransformExplicitType()
            else -> element.parentInVisitor?.let(this::visitMethodReturnType) ?: element
        }

    override fun skipElement(element: Element): Boolean = element.isRootElement || !element.hasAcceptMethod

    override fun parentInVisitor(element: Element): Element? =
        element.parentInLeafOnlyVisitor(rootElement)

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        println()
        printRootTransformMethodDeclaration(rootElement, Modality.OPEN, hasDataParameter = false)
        printBlock {
            println(rootElement.visitorParameterName, ".transformChildren(this, null)")
            println("return ", rootElement.visitorParameterName)
        }
        println()
        printRootTransformMethodDeclaration(rootElement, Modality.FINAL, hasDataParameter = true, override = true)
        println(" =")
        withIndent {
            println("transformElement(", rootElement.visitorParameterName, ")")
        }
        println()
        val typeParameter = TypeVariable("T", listOf(IrTree.rootElement))
        printFunctionWithBlockBody(
            name = "transformPostfix",
            parameters = listOf(FunctionParameter("body", Lambda(receiver = typeParameter, returnType = StandardTypes.unit))),
            returnType = typeParameter,
            typeParameters = listOf(typeParameter),
            extensionReceiver = typeParameter,
            visibility = Visibility.PROTECTED,
            isInline = true,
        ) {
            println("transformChildrenVoid()")
            println("this.body()")
            println("return this")
        }
        println()
        printFunctionWithBlockBody(
            name = "transformChildrenVoid",
            parameters = emptyList(),
            returnType = StandardTypes.unit,
            extensionReceiver = IrTree.rootElement,
            visibility = Visibility.PROTECTED,
        ) {
            println("transformChildrenVoid(this@", visitorType.simpleName, ")")
        }
    }

    override fun printMethodsForElement(element: Element) {
        printer.run {
            println()
            printVisitMethodDeclaration(element, hasDataParameter = false, modality = Modality.OPEN)
            println(" =")
            val parent = parentInVisitor(element)
            withIndent {
                if (element.transformByChildren || parent?.isRootElement == true) {
                    println("transformElement(", element.visitorParameterName, ")")
                } else {
                    println(parent!!.visitFunctionName, "(", element.visitorParameterName, ")")
                }
            }
            println()
            printVisitMethodDeclaration(
                element = element,
                modality = Modality.FINAL,
                override = true,
                returnType = element.getTransformExplicitType(),
            )
            println(" =")
            withIndent {
                println(element.visitFunctionName, "(", element.visitorParameterName, ")")
            }
        }
    }

    override fun printVisitor(elements: List<Element>) {
        super.printVisitor(elements)
        printer.run {
            println()
            val transformerParameter = FunctionParameter("transformer", visitorType)
            printFunctionWithBlockBody(
                name = "transformChildrenVoid",
                parameters = listOf(transformerParameter),
                returnType = StandardTypes.unit,
                extensionReceiver = IrTree.rootElement,
            ) {
                println("transformChildren(", transformerParameter.name, ", null)")
            }
        }
    }

}