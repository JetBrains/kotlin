/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

abstract class AbstractTransformerVoidPrinter<Element : AbstractElement<Element, Field, *>, Field : AbstractField<Field>>(
    printer: SmartPrinter
) : AbstractTransformerPrinter<Element, Field>(printer) {

    final override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    final override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    abstract val transformerSuperClass: ClassRef<PositionTypeParameterRef>

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>?
        get() = transformerSuperClass.withArgs(visitorDataType)

    context(ImportCollector)
    override fun printMethodsForElement(element: Element) {
        printer.run {
            val elementParameterName = element.visitorParameterName
            val dataParameter = FunctionParameter("data", visitorDataType)
            val methodName = "transform" + element.name
            if (element.isRootElement) {
                println()
                val elementTP = TypeVariable("E", listOf(element))
                printFunctionDeclaration(
                    name = methodName,
                    parameters = listOf(FunctionParameter(elementParameterName, elementTP)),
                    returnType = elementTP,
                    typeParameters = listOf(elementTP),
                    modality = Modality.ABSTRACT,
                )
                println()
                println()
                printFunctionDeclaration(
                    name = methodName,
                    parameters = listOf(FunctionParameter(elementParameterName, elementTP), dataParameter),
                    returnType = elementTP,
                    typeParameters = listOf(elementTP),
                    modality = Modality.FINAL,
                    override = true,
                )
            } else {
                val parentInVisitor = parentInVisitor(element) ?: return
                val returnType = visitMethodReturnType(element)
                println()
                printFunctionDeclaration(
                    name = methodName,
                    parameters = listOf(FunctionParameter(elementParameterName, element)),
                    returnType = returnType,
                    modality = Modality.OPEN,
                )
                println(" =")
                withIndent {
                    println("transform", parentInVisitor.name, "(", elementParameterName, ")")
                }
                println()
                printFunctionDeclaration(
                    name = methodName,
                    parameters = listOf(FunctionParameter(elementParameterName, element), dataParameter),
                    returnType = returnType,
                    modality = Modality.FINAL,
                    override = true,
                )
            }
            println(" =")
            withIndent {
                println(methodName, "(", elementParameterName, ")")
            }
        }
    }
}