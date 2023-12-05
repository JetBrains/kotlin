/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.elementTransformerType
import org.jetbrains.kotlin.ir.generator.irTypeType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.SingleField
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

internal class TypeTransformerPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
    private val rootElement: Element,
) : AbstractVisitorPrinter<Element, Field>(printer) {

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = elementTransformerType.withArgs(dataTypeVariable)

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = element.getTransformExplicitType()

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    private fun Element.getFieldsWithIrTypeType(insideParent: Boolean = false): List<Field> {
        val parentsFields = elementParents.flatMap { it.element.getFieldsWithIrTypeType(insideParent = true) }
        if (insideParent && this.parentInVisitor != null) {
            return parentsFields
        }

        val irTypeFields = this.fields
            .filter {
                val type = when (it) {
                    is SingleField -> it.typeRef
                    is ListField -> it.baseType
                }
                type.toString() == irTypeType.toString()
            }

        return irTypeFields + parentsFields
    }

    context(ImportCollector)
    override fun SmartPrinter.printAdditionalMethods() {
        val typeTP = TypeVariable("Type", listOf(irTypeType.copy(nullable = true)))
        printFunctionDeclaration(
            name = "transformType",
            parameters = listOf(
                FunctionParameter("container", rootElement),
                FunctionParameter("type", typeTP),
                FunctionParameter("data", visitorDataType)
            ),
            returnType = typeTP,
            typeParameters = listOf(typeTP),
        )
        println()
    }

    context(ImportCollector)
    override fun printMethodsForElement(element: Element) {
        val irTypeFields = element.getFieldsWithIrTypeType()
        if (irTypeFields.isEmpty()) return
        if (element.parentInVisitor == null) return
        printer.run {
            println()
            val visitorParam = element.visitorParameterName
            printVisitMethodDeclaration(
                element = element,
                override = true,
            )

            fun addVisitTypeStatement(field: Field) {
                val access = "$visitorParam.${field.name}"
                when (field) {
                    is SingleField -> println(access, " = ", "transformType(", visitorParam, ", ", access, ", data)")
                    is ListField -> {
                        if (field.isMutable) {
                            println(access, " = ", access, ".map { transformType(", visitorParam, ", it, data) }")
                        } else {
                            println("for (i in 0 until ", access, ".size) {")
                            withIndent {
                                println(access, "[i] = transformType(", visitorParam, ", ", access, "[i], data)")
                            }
                            println("}")
                        }
                    }
                }
            }

            printBlock {
                when (element) {
                    IrTree.memberAccessExpression -> {
                        if (irTypeFields.singleOrNull()?.name != "typeArguments") {
                            error(
                                """`${IrTree.memberAccessExpression.typeName}` has unexpected fields with `IrType` type. 
                                        |Please adjust logic of `${visitorType.simpleName}`'s generation.""".trimMargin()
                            )
                        }
                        println("(0 until ", visitorParam, ".typeArgumentsCount).forEach {")
                        withIndent {
                            println(visitorParam, ".getTypeArgument(it)?.let { type ->")
                            withIndent {
                                println(
                                    visitorParam,
                                    ".putTypeArgument(it, transformType(",
                                    visitorParam,
                                    ", type, data))"
                                )
                            }
                            println("}")
                        }
                        println("}")
                    }
                    IrTree.`class` -> {
                        println(visitorParam, ".valueClassRepresentation?.mapUnderlyingType {")
                        withIndent {
                            println("transformType(", visitorParam, ", it, data)")
                        }
                        println("}")
                        irTypeFields.forEach(::addVisitTypeStatement)
                    }
                    else -> {
                        irTypeFields.forEach(::addVisitTypeStatement)
                    }
                }
                println(
                    "return super.",
                    element.visitFunctionName,
                    "(",
                    visitorParam,
                    ", data)"
                )
            }
        }
    }
}
