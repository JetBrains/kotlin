/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

private fun printVisitorCommon(
    generationPath: File,
    model: Model,
    visitorType: ClassRef<*>,
    makePrinter: (SmartPrinter, ClassRef<*>) -> AbstractVisitorPrinter<Element, Field>,
): GeneratedFile =
    printGeneratedType(generationPath, TREE_GENERATOR_README, visitorType.packageName, visitorType.simpleName) {
        println()
        makePrinter(this, visitorType).printVisitor(model.elements)
    }

private open class VisitorPrinter(printer: SmartPrinter, override val visitorType: ClassRef<*>) :
    AbstractVisitorPrinter<Element, Field>(printer, visitSuperTypeByDefault = false) {

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = resultTypeVariable

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>?
        get() = null

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false
}

fun printVisitor(generationPath: File, model: Model) = printVisitorCommon(generationPath, model, elementVisitorType, ::VisitorPrinter)

private class VisitorVoidPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorVoidPrinter<Element, Field>(printer, visitSuperTypeByDefault = false) {

    override val visitorSuperClass: ClassRef<PositionTypeParameterRef>
        get() = elementVisitorType

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    override val useAbstractMethodForRootElement: Boolean
        get() = false

    override val overriddenVisitMethodsAreFinal: Boolean
        get() = false
}

fun printVisitorVoid(generationPath: File, model: Model) =
    printVisitorCommon(generationPath, model, elementVisitorVoidType, ::VisitorVoidPrinter)

private class TransformerPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
    val rootElement: Element,
) : AbstractVisitorPrinter<Element, Field>(printer, visitSuperTypeByDefault = false) {

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = elementVisitorType.withArgs(rootElement, dataTypeVariable)

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = element.getTransformExplicitType()

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    context(ImportCollector)
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
                    println(" {")
                    withIndent {
                        println(element.visitorParameterName, ".transformChildren(this, data)")
                        println("return ", element.visitorParameterName)
                    }
                    println("}")
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

fun printTransformer(generationPath: File, model: Model): GeneratedFile =
    printVisitorCommon(generationPath, model, elementTransformerType) { printer, visitorType ->
        TransformerPrinter(printer, visitorType, model.rootElement)
    }

private class TypeTransformerPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
    val rootElement: Element,
) : AbstractVisitorPrinter<Element, Field>(printer, visitSuperTypeByDefault = false) {

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
                    is ListField -> it.elementType
                }
                type.toString() == irTypeType.toString()
            }

        return irTypeFields + parentsFields
    }

    context(ImportCollector)
    override fun SmartPrinter.printAdditionalMethods() {
        val typeTP = TypeVariable("Type", listOf(irTypeType.copy(nullable = true)), Variance.INVARIANT)
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

            println(" {")
            withIndent {
                when (element.name) {
                    IrTree.memberAccessExpression.name -> {
                        if (irTypeFields.singleOrNull()?.name != "typeArguments") {
                            error(
                                """`Ir${IrTree.memberAccessExpression.name.capitalizeAsciiOnly()}` has unexpected fields with `IrType` type. 
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
                    IrTree.`class`.name -> {
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
            println("}")
        }
    }
}

fun printTypeVisitor(generationPath: File, model: Model): GeneratedFile =
    printVisitorCommon(generationPath, model, typeTransformerType) { printer, visitorType ->
        TypeTransformerPrinter(printer, visitorType, model.rootElement)
    }

private fun Element.getTransformExplicitType(): Element {
    return generateSequence(this) { it.parentInVisitor?.element }
        .firstNotNullOfOrNull {
            when {
                it.transformByChildren -> it.transformerReturnType ?: it
                else -> it.transformerReturnType
            }
        } ?: this
}
