/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.elementVisitorType
import org.jetbrains.kotlin.ir.generator.irTypeType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.SingleField
import org.jetbrains.kotlin.utils.withIndent

internal open class TypeTransformerPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    private val rootElement: Element,
) : AbstractVisitorPrinter<Element, Field>(printer) {

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(elementVisitorType.withArgs(resultTypeVariable, dataTypeVariable))

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element): TypeRef = resultTypeVariable

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    protected fun Element.getFieldsWithIrTypeType(insideParent: Boolean = false): List<Field> {
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

    protected fun ImportCollectingPrinter.printTransformTypeMethod(hasDataParameter: Boolean, modality: Modality?, override: Boolean) {
        val typeTP = TypeVariable("Type", listOf(irTypeType.copy(nullable = true)))
        printFunctionDeclaration(
            name = "transformType",
            parameters = listOfNotNull(
                FunctionParameter("container", rootElement),
                FunctionParameter("type", typeTP),
                FunctionParameter("data", visitorDataType).takeIf { hasDataParameter },
            ),
            returnType = typeTP,
            typeParameters = listOf(typeTP),
            modality = modality,
            override = override,
        )
    }

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        printTransformTypeMethod(hasDataParameter = true, modality = null, override = false)
        println()
    }

    protected open fun ImportCollectingPrinter.printTypeRemappings(element: Element, irTypeFields: List<Field>, hasDataParameter: Boolean) {
        val visitorParam = element.visitorParameterName
        fun addVisitTypeStatement(field: Field) {
            val access = "$visitorParam.${field.name}"
            when (field) {
                is SingleField -> {
                    print(access, " = ", "transformType(", visitorParam, ", ", access)
                    if (hasDataParameter) {
                        print(", data")
                    }
                    println(")")
                }
                is ListField -> {
                    if (field.isMutable) {
                        print(access, " = ", access, ".map { transformType(", visitorParam, ", it")
                        if (hasDataParameter) {
                            print(", data")
                        }
                        println(") }")
                    } else {
                        print("for (i in 0 until ", access, ".size)")
                        printBlock {
                            print(access, "[i] = transformType(", visitorParam, ", ", access, "[i]")
                            if (hasDataParameter) {
                                print(", data")
                            }
                            println(")")
                        }
                    }
                }
            }
        }
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
                        print(
                            visitorParam,
                            ".putTypeArgument(it, transformType(",
                            visitorParam,
                            ", type"
                        )
                        if (hasDataParameter) {
                            print(", data")
                        }
                        println("))")
                    }
                    println("}")
                }
                println("}")
            }
            IrTree.`class` -> {
                println(visitorParam, ".valueClassRepresentation?.mapUnderlyingType {")
                withIndent {
                    print("transformType(", visitorParam, ", it")
                    if (hasDataParameter) {
                        print(", data")
                    }
                    println(")")
                }
                println("}")
                irTypeFields.forEach(::addVisitTypeStatement)
            }
            else -> {
                irTypeFields.forEach(::addVisitTypeStatement)
            }
        }
    }

    override fun printMethodsForElement(element: Element) {
        val irTypeFields = element.getFieldsWithIrTypeType()
        if (irTypeFields.isEmpty()) return
        if (element.parentInVisitor == null) return
        printer.run {
            println()
            printVisitMethodDeclaration(
                element = element,
                override = true,
            )
            printBlock {
                printTypeRemappings(element, irTypeFields, hasDataParameter = true)
                println(
                    "return super.",
                    element.visitFunctionName,
                    "(",
                    element.visitorParameterName,
                    ", data)"
                )
            }
        }
    }
}
