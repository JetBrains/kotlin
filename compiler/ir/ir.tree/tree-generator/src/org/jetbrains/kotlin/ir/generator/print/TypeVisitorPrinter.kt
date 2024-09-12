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
import org.jetbrains.kotlin.ir.generator.model.SimpleField
import org.jetbrains.kotlin.utils.withIndent

internal open class TypeVisitorPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    protected val rootElement: Element,
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
                    is SimpleField -> it.typeRef
                    is ListField -> it.baseType
                }
                type.toString() == irTypeType.toString()
            }

        return irTypeFields + parentsFields
    }

    protected fun ImportCollectingPrinter.printVisitTypeMethod(hasDataParameter: Boolean, modality: Modality?, override: Boolean) {
        printFunctionDeclaration(
            name = "visitType",
            parameters = listOfNotNull(
                FunctionParameter("container", rootElement),
                FunctionParameter("type", irTypeType),
                FunctionParameter("data", visitorDataType).takeIf { hasDataParameter },
            ),
            returnType = StandardTypes.unit,
            modality = modality,
            override = override
        )
    }

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        printVisitTypeMethod(hasDataParameter = true, modality = Modality.ABSTRACT, override = false)
        println()
    }

    protected fun ImportCollectingPrinter.printTypeRemappings(
        element: Element,
        irTypeFields: List<Field>,
        hasDataParameter: Boolean,
        replaceTypes: Boolean,
        visitTypeMethodName: String,
    ) {
        val visitorParam = element.visitorParameterName
        fun addVisitTypeStatement(field: Field) {
            val access = "$visitorParam.${field.name}"
            when (field) {
                is SimpleField -> {
                    val argumentToPassToVisitType = if (replaceTypes) {
                        print(access, " = ")
                        access
                    } else if (field.nullable) {
                        print(access, "?.let { ")
                        "it"
                    } else {
                        access
                    }
                    print(visitTypeMethodName, "(", visitorParam, ", ", argumentToPassToVisitType)
                    if (hasDataParameter) {
                        print(", data")
                    }
                    print(")")
                    if (!replaceTypes && field.nullable) {
                        print(" }")
                    }
                    println()
                }
                is ListField -> {
                    if (!replaceTypes) {
                        print(access, ".forEach { ", visitTypeMethodName, "(", visitorParam, ", it")
                        if (hasDataParameter) {
                            print(", data")
                        }
                        println(") }")
                    } else if (field.isMutable) {
                        print(access, " = ", access, ".map { ", visitTypeMethodName, "(", visitorParam, ", it")
                        if (hasDataParameter) {
                            print(", data")
                        }
                        println(") }")
                    } else {
                        print("for (i in 0 until ", access, ".size)")
                        printBlock {
                            print(access, "[i] = ", visitTypeMethodName, "(", visitorParam, ", ", access, "[i]")
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
                        if (replaceTypes) {
                            print(
                                visitorParam,
                                ".putTypeArgument(it, ",
                                visitTypeMethodName,
                                "(",
                                visitorParam,
                                ", type"
                            )
                            if (hasDataParameter) {
                                print(", data")
                            }
                            println("))")
                        } else {
                            print(visitTypeMethodName, "(", visitorParam, ", type")
                            if (hasDataParameter) {
                                print(", data")
                            }
                            println(")")
                        }
                    }
                    println("}")
                }
                println("}")
            }
            IrTree.`class` -> {
                println(visitorParam, ".valueClassRepresentation?.mapUnderlyingType {")
                withIndent {
                    print(visitTypeMethodName, "(", visitorParam, ", it")
                    if (hasDataParameter) {
                        print(", data")
                    }
                    println(")")
                    if (!replaceTypes) {
                        println("it")
                    }
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
                printTypeRemappings(element, irTypeFields, hasDataParameter = true, replaceTypes = false, visitTypeMethodName = "visitType")
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