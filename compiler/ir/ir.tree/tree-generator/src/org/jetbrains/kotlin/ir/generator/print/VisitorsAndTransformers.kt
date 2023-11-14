/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.ir.generator.model.Model
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

fun printTransformer(generationPath: File, model: Model): GeneratedFile =
    printVisitorCommon(generationPath, model, elementTransformerType) { printer, visitorType ->
        TransformerPrinter(printer, visitorType, model.rootElement)
    }

private class TransformerVoidPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer, visitSuperTypeByDefault = false) {

    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = elementTransformerType.withArgs(visitorDataType)

    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    // IrPackageFragment is treated as transformByChildren in IrElementTransformerVoid for historical reasons.
    private val Element.isPackageFragment: Boolean
        get() = this == IrTree.packageFragment

    // Despite IrFile and IrExternalPackageFragment being transformByChildren, we treat them differently in IrElementTransformerVoid
    // than in IrElementTransformer for historical reasons. We want to preserve the historical semantics here.
    private val Element.isPackageFragmentChild: Boolean
        get() = elementParents.any { it.element.isPackageFragment }

    private val Element.transformByChildrenVoid: Boolean
        get() = element.transformByChildren || isPackageFragment

    override fun visitMethodReturnType(element: Element): Element =
        when {
            element.isPackageFragment -> element
            element.transformByChildren -> element.getTransformExplicitType()
            else -> element.parentInVisitor?.let(this::visitMethodReturnType) ?: element
        }

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    context(ImportCollector)
    override fun SmartPrinter.printAdditionalMethods() {
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

    context(ImportCollector)
    override fun printMethodsForElement(element: Element) {
        val parent = element.parentInVisitor
        if (!element.transformByChildrenVoid && parent == null) return
        printer.run {
            println()
            printVisitMethodDeclaration(element, hasDataParameter = false, modality = Modality.OPEN)
            if (element.transformByChildrenVoid && !element.isPackageFragmentChild) {
                printBlock {
                    println(element.visitorParameterName, ".transformChildren(this, null)")
                    println("return ", element.visitorParameterName)
                }
            } else {
                println(" =")
                withIndent {
                    print(parent!!.visitFunctionName, "(", element.visitorParameterName, ")")
                    if (element.isPackageFragmentChild) {
                        print(" as ", element.render())
                    }
                    println()
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
}

fun printTransformerVoid(generationPath: File, model: Model): GeneratedFile =
    printGeneratedType(
        generationPath,
        TREE_GENERATOR_README,
        elementTransformerVoidType.packageName,
        elementTransformerVoidType.simpleName,
    ) {
        TransformerVoidPrinter(this, elementTransformerVoidType).printVisitor(model.elements)
        println()
        val transformerParameter = FunctionParameter("transformer", elementTransformerVoidType)
        printFunctionWithBlockBody(
            name = "transformChildrenVoid",
            parameters = listOf(transformerParameter),
            returnType = StandardTypes.unit,
            extensionReceiver = IrTree.rootElement,
        ) {
            println("transformChildren(", transformerParameter.name, ", null)")
        }
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

fun printTypeVisitor(generationPath: File, model: Model): GeneratedFile =
    printVisitorCommon(generationPath, model, typeTransformerType) { printer, visitorType ->
        TypeTransformerPrinter(printer, visitorType, model.rootElement)
    }

private fun Element.getTransformExplicitType(): Element {
    return generateSequence(this) { it.parentInVisitor }
        .firstNotNullOfOrNull {
            when {
                it.transformByChildren -> it.transformerReturnType ?: it
                else -> it.transformerReturnType
            }
        } ?: this
}
