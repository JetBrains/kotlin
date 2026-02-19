/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.Packages
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
        get() = listOf(
            irVisitorType.withArgs(rootElement, dataTypeVariable),
            ClassRef<PositionTypeParameterRef>(
                TypeKind.Interface,
                Packages.visitors,
                "IrElementTransformer",
                typeAnnotations = listOf("Suppress(\"DEPRECATION_ERROR\")")
            ).withArgs(dataTypeVariable), // See KT-75353
        )

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = element.getTransformExplicitType()

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
                    printBlock {
                        println(element.visitorParameterName, ".transformChildren(this, data)")
                        println("return ", element.visitorParameterName)
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
