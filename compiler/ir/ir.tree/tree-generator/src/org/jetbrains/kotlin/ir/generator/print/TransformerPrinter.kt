/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.legacyVisitorType
import org.jetbrains.kotlin.ir.generator.irTransformerType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.utils.withIndent

internal class TransformerPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    private val rootElement: Element,
) : AbstractTransformerPrinter<Element, Field>(printer) {

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(legacyVisitorType.withArgs(rootElement, dataTypeVariable))

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

    override val ImportCollecting.classKDoc: String
        get() = deprecatedVisitorInterface(irTransformerType)

    override val annotations: List<Annotation>
        get() = listOf(DeprecatedCompilerApi(CompilerVersionOfApiDeprecation._2_1_20, replaceWith = "IrTransformer"))
}
