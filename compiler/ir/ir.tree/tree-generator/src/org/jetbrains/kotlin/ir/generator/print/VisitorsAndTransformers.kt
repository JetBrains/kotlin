/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

private val visitorTypeName = ClassName(VISITOR_PACKAGE, "IrElementVisitor")
private val transformerTypeName = ClassName(VISITOR_PACKAGE, "IrElementTransformer")
private val typeTransformerTypeName = ClassName(VISITOR_PACKAGE, "IrTypeTransformer")

private fun printVisitorCommon(
    generationPath: File,
    model: Model,
    printerCreator: (SmartPrinter) -> AbstractVisitorPrinter<Element, Field>,
): GeneratedFile {
    val elements = model.elements
    val stringBuilder = StringBuilder()
    val smartPrinter = SmartPrinter(stringBuilder)
    val printer = printerCreator(smartPrinter)
    val dir = File(generationPath, printer.visitorType.packageName.replace(".", "/"))
    val file = File(dir, "${printer.visitorType.simpleName}.kt")
    smartPrinter.run {
        printCopyright()
        println(GENERATED_MESSAGE)
        println()
        println("package $VISITOR_PACKAGE")
        println()
        println("import ", Packages.tree, ".*")
        println("import ", Packages.declarations, ".*")
        println("import ", Packages.exprs, ".*")
        println()
        printer.printVisitor(elements)
    }
    return GeneratedFile(file, stringBuilder.toString())
}

private open class VisitorPrinter(printer: SmartPrinter) :
    AbstractVisitorPrinter<Element, Field>(printer, visitSuperTypeByDefault = false) {

    override val visitorType: ClassRef<*>
        get() = elementVisitorType

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>?
        get() = null

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false
}

fun printVisitor(generationPath: File, model: Model) = printVisitorCommon(generationPath, model, ::VisitorPrinter)

private class VisitorVoidPrinter(
    printer: SmartPrinter,
) : AbstractVisitorVoidPrinter<Element, Field>(printer, visitSuperTypeByDefault = false) {
    override val visitorType: ClassRef<*>
        get() = elementVisitorVoidType

    override val visitorSuperClass: ClassRef<PositionTypeParameterRef>
        get() = elementVisitorType

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    override val useAbstractMethodForRootElement: Boolean
        get() = false

    override val overriddenVisitMethodsAreFinal: Boolean
        get() = false
}

fun printVisitorVoid(generationPath: File, model: Model) = printVisitorCommon(generationPath, model, ::VisitorVoidPrinter)

fun printTransformer(generationPath: File, model: Model): GeneratedFile {
    val visitorType = TypeSpec.interfaceBuilder(transformerTypeName).apply {
        val d = TypeVariableName("D", KModifier.IN)
        addTypeVariable(d)

        addSuperinterface(visitorTypeName.parameterizedBy(model.rootElement.toPoetStarParameterized(), d))

        fun buildVisitFun(element: Element) = FunSpec.builder(element.visitFunctionName).apply {
            addModifiers(KModifier.OVERRIDE)
            addParameter(element.visitorParameterName, element.toPoetStarParameterized())
            addParameter("data", d)
        }

        for (element in model.elements) {
            val returnType = element.getTransformExplicitType()
            if (element.transformByChildren) {
                addFunction(buildVisitFun(element).apply {
                    addStatement("${element.visitorParameterName}.transformChildren(this, data)")
                    addStatement("return ${element.visitorParameterName}")
                    returns(returnType.toPoetStarParameterized())
                }.build())
            } else {
                element.parentInVisitor?.let { parent ->
                    addFunction(buildVisitFun(element).apply {
                        addStatement("return ${parent.element.visitFunctionName}(${element.visitorParameterName}, data)")
                        returns(returnType.toPoetStarParameterized())
                    }.build())
                }
            }
        }
    }.build()

    return printTypeCommon(generationPath, transformerTypeName.packageName, visitorType)
}

fun printTypeVisitor(generationPath: File, model: Model): GeneratedFile {
    val transformTypeFunName = "transformType"

    fun FunSpec.Builder.addVisitTypeStatement(element: Element, field: Field) {
        val visitorParam = element.visitorParameterName
        val access = "$visitorParam.${field.name}"
        when (field) {
            is SingleField -> addStatement("$access = $transformTypeFunName($visitorParam, $access, data)")
            is ListField -> {
                if (field.isMutable) {
                    addStatement("$access = $access.map { $transformTypeFunName($visitorParam, it, data) }")
                } else {
                    beginControlFlow("for (i in 0 until $access.size)")
                    addStatement("$access[i] = $transformTypeFunName($visitorParam, $access[i], data)")
                    endControlFlow()
                }
            }
        }
    }

    fun Element.getFieldsWithIrTypeType(insideParent: Boolean = false): List<Field> {
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

    val visitorType = TypeSpec.interfaceBuilder(typeTransformerTypeName).apply {
        val d = TypeVariableName("D", KModifier.IN)
        addTypeVariable(d)
        addSuperinterface(transformerTypeName.parameterizedBy(d))

        val abstractVisitFun = FunSpec.builder(transformTypeFunName).apply {
            val poetNullableIrType = irTypeType.toPoet().copy(nullable = true)
            val typeVariable = TypeVariableName("Type", poetNullableIrType)
            addTypeVariable(typeVariable)
            addParameter("container", model.rootElement.toPoet())
            addParameter("type", typeVariable)
            addParameter("data", d)
            returns(typeVariable)
        }
        addFunction(abstractVisitFun.addModifiers(KModifier.ABSTRACT).build())

        fun buildVisitFun(element: Element) = FunSpec.builder(element.visitFunctionName).apply {
            addModifiers(KModifier.OVERRIDE)
            addParameter(element.visitorParameterName, element.toPoetStarParameterized())
            addParameter("data", d)
        }

        for (element in model.elements) {
            val irTypeFields = element.getFieldsWithIrTypeType()
            if (irTypeFields.isEmpty()) continue

            val returnType = element.getTransformExplicitType()
            element.parentInVisitor?.let { _ ->
                addFunction(buildVisitFun(element).apply {
                    returns(returnType.toPoetStarParameterized())

                    val visitorParam = element.visitorParameterName
                    when (element.name) {
                        IrTree.memberAccessExpression.name -> {
                            if (irTypeFields.singleOrNull()?.name != "typeArguments") {
                                error(
                                    """`Ir${IrTree.memberAccessExpression.name.capitalizeAsciiOnly()}` has unexpected fields with `IrType` type. 
                                        |Please adjust logic of `${typeTransformerTypeName.simpleName}`'s generation.""".trimMargin()
                                )
                            }
                            beginControlFlow("(0 until $visitorParam.typeArgumentsCount).forEach {")
                            beginControlFlow("$visitorParam.getTypeArgument(it)?.let { type ->")
                            addStatement("expression.putTypeArgument(it, $transformTypeFunName($visitorParam, type, data))")
                            endControlFlow()
                            endControlFlow()
                        }
                        IrTree.`class`.name -> {
                            beginControlFlow("$visitorParam.valueClassRepresentation?.mapUnderlyingType {")
                            addStatement("$transformTypeFunName($visitorParam, it, data)")
                            endControlFlow()
                            irTypeFields.forEach { addVisitTypeStatement(element, it) }
                        }
                        else -> irTypeFields.forEach { addVisitTypeStatement(element, it) }
                    }
                    addStatement("return super.${element.visitFunctionName}($visitorParam, data)")
                }.build())
            }
        }
    }.build()

    return printTypeCommon(generationPath, typeTransformerTypeName.packageName, visitorType)
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
