/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.ir.generator.DSL_PACKAGE
import org.jetbrains.kotlin.ir.generator.config.ElementConfig
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.Model
import org.jetbrains.kotlin.ir.generator.util.GeneratedFile
import org.jetbrains.kotlin.ir.generator.util.parameterizedByIfAny
import org.jetbrains.kotlin.name.Name
import java.io.File

private val buildingContextTypeName = ClassName(DSL_PACKAGE, "IrBuildingContext")
private val prettyIrDslAnnotationTypeName = ClassName(DSL_PACKAGE, "PrettyIrDsl")
private val irNodeBuilderDslAnnotationTypeName = ClassName(DSL_PACKAGE, "IrNodeBuilderDsl")

fun printDslDeclarationContainerBuilder(generationPath: File, model: Model): GeneratedFile {
    val interfaceName = "IrDeclarationContainerBuilder"
    return printFile(generationPath, DSL_PACKAGE, interfaceName) {
        for (element in model.elements) {
            if (!element.isLeaf || element.packageName != ElementConfig.Category.Declaration.packageName) continue
            addFunction(
                buildChildDeclarationBuilderFunction(element)
                    .receiver(ClassName(DSL_PACKAGE, interfaceName))
                    .addAnnotation(irNodeBuilderDslAnnotationTypeName)
                    .run {
                        addStatement(
                            buildString {
                                append("declarationBuilders")
                                append(".add(")
                                appendCallToBuilder(
                                    element,
                                    buildingContextPropertyName = "buildingContext",
                                    mandatoryParameterNames = parameters.dropLast(1).map { it.name },
                                    blockParameterName = parameters.last().name
                                )
                                append(")")
                            }
                        )
                    }
                    .build()
            )
        }
    }
}

private val Element.isDeclaration: Boolean
    get() = TODO()

private fun StringBuilder.appendCallToBuilder(
    element: Element,
    buildingContextPropertyName: String,
    mandatoryParameterNames: Iterable<String>,
    blockParameterName: String,
) {
    append(element.builderClassName.simpleName)
    if (element.poetTypeVariables.isNotEmpty()) {
        element.poetTypeVariables.joinTo(this, prefix = "<", postfix = ">") { it.name }
    }
    append("(")
    for (mandatoryParameterName in mandatoryParameterNames) {
        append(mandatoryParameterName)
        append(", ")
    }
    append(buildingContextPropertyName)
    append(").apply(")
    append(blockParameterName)
    append(")")
}

fun printTopLevelBuilderFunctions(generationPath: File, model: Model) = printFile(generationPath, DSL_PACKAGE, "topLevelBuilders") {
    for (element in model.elements) {
        if (!element.isLeaf) continue
        addFunction(buildTopLevelBuilderFunction(element).build())
    }
}

private fun buildMandatoryElementSpecificParameters(element: Element) = element.allFields.mapNotNull { field ->
    if (field.isMandatoryInDSL) {
        val poetFieldType = field.type.toPoet()
        val type = if (poetFieldType == Name::class.asTypeName()) {
            STRING
        } else {
            poetFieldType
        }
        ParameterSpec.builder(field.name, type).build()
    } else null
}

private fun buildBuildingContextParameter() =
    ParameterSpec.builder("buildingContext", buildingContextTypeName)
        .defaultValue("${buildingContextTypeName.simpleName}()")
        .build()

private val Element.builderClassName: ClassName
    get() = ClassName(DSL_PACKAGE, "${typeName}Builder")

private fun buildLambdaParameter(element: Element) =
    ParameterSpec.builder(
        "block",
        ClassName(DSL_PACKAGE, "IrElementBuilderClosure").parameterizedBy(
            element.builderClassName.parameterizedByIfAny(element.poetTypeVariables)
        )
    ).build()

private fun buildBuilderFunction(element: Element, name: String, additionalParameters: Iterable<ParameterSpec>, returnType: TypeName) =
    FunSpec.builder(name)
        .addModifiers(KModifier.INLINE)
        .addTypeVariables(element.poetTypeVariables)
        .addParameters(buildMandatoryElementSpecificParameters(element))
        .addParameters(additionalParameters)
        .addParameter(buildLambdaParameter(element))
        .returns(returnType)

private fun buildChildDeclarationBuilderFunction(element: Element) =
    buildBuilderFunction(
        element,
        name = element.typeName.replaceFirstChar(Char::lowercaseChar),
        additionalParameters = emptyList(),
        returnType = UNIT
    )

private fun buildTopLevelBuilderFunction(element: Element): FunSpec.Builder {
    val buildingContextParameter = buildBuildingContextParameter()
    return buildBuilderFunction(
        element,
        name = "build${element.typeName}",
        additionalParameters = listOf(buildingContextParameter),
        element.toPoetSelfParameterized()
    ).run {
        addStatement(
            buildString {
                append("return ")
                appendCallToBuilder(element, buildingContextParameter.name, parameters.dropLast(2).map { it.name }, parameters.last().name)
                append(".build()")
            }
        )
    }
}

private val Element.allFields: List<Field>
    get() {
        val seen = mutableSetOf<Field>()
        val result = mutableListOf<Field>()
        fun collectFields(element: Element) {
            for (parent in element.elementParents) {
                collectFields(parent.element)
            }
            for (field in element.fields) {
                if (field in seen) continue
                result.add(field)
                seen.add(field)
            }
        }
        collectFields(this)
        return result
    }
