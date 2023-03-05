/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.ir.generator.DSL_PACKAGE
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.Packages
import org.jetbrains.kotlin.ir.generator.config.ElementConfig
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.ElementRef
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.Model
import org.jetbrains.kotlin.ir.generator.util.ClassRef
import org.jetbrains.kotlin.ir.generator.util.GeneratedFile
import org.jetbrains.kotlin.ir.generator.util.parameterizedByIfAny
import org.jetbrains.kotlin.name.Name
import java.io.File

private val buildingContextTypeName = ClassName(DSL_PACKAGE, "IrBuildingContext")
private val irNodeBuilderDslAnnotationTypeName = ClassName(DSL_PACKAGE, "IrNodeBuilderDsl")

fun printDslDeclarationContainerBuilders(generationPath: File, model: Model): GeneratedFile =
    printDslContainerBuilders(
        "IrDeclarationContainerBuilder",
        "__internal_addDeclarationBuilder",
        { it.isChildOf(IrTree.declaration) },
        generationPath,
        model
    )

fun printDslStatementContainerBuilders(generationPath: File, model: Model): GeneratedFile =
    printDslContainerBuilders(
        "IrStatementContainerBuilder",
        "__internal_addStatementBuilder",
        { it.isChildOf(IrTree.statement) && !it.isChildOf(IrTree.declaration) },
        generationPath,
        model
    )

fun printDslExpressionContainerBuilders(generationPath: File, model: Model): GeneratedFile =
    printDslContainerBuilders(
        "IrExpressionContainerBuilder",
        "__internal_addExpressionBuilder",
        { it.isChildOf(IrTree.expression) },
        generationPath,
        model
    )

private fun printDslContainerBuilders(
    interfaceName: String,
    addMethodName: String,
    elementPredicate: (Element) -> Boolean,
    generationPath: File,
    model: Model
): GeneratedFile = printFile(generationPath, DSL_PACKAGE, interfaceName) {
    for (element in model.elements) {
        if (!element.isLeaf || !elementPredicate(element)) continue
        buildIrNodeBuilderFunctions(
            element,
            name = element.typeName.replaceFirstChar(Char::lowercaseChar),
            additionalParameters = emptyList(),
            returnType = UNIT,
            customize = {
                receiver(ClassName(DSL_PACKAGE, interfaceName))
            },
            bodyBuilder = {
                addStatement(
                    buildString {
                        append(addMethodName)
                        append("(")
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
        ).forEach(::addFunction)
    }
}

private fun Element.isChildOf(elementConfig: ElementConfig): Boolean {
    fun recurse(parents: List<ElementRef>): Boolean {
        if (parents.any { it.element.name == elementConfig.name }) return true
        return parents.any { recurse(it.element.elementParents) }
    }
    return recurse(elementParents)
}

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

        // IrFile and IrModuleFragment builders are special, we'll implement them by hand
        if (element.name == IrTree.file.name || element.name == IrTree.moduleFragment.name) continue

        buildTopLevelBuilderFunctions(element).forEach(::addFunction)
    }
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

private fun buildIrNodeBuilderFunctions(
    element: Element,
    name: String,
    additionalParameters: Iterable<ParameterSpec>,
    returnType: TypeName,
    customize: FunSpec.Builder.() -> Unit = {},
    bodyBuilder: FunSpec.Builder.() -> Unit
): List<FunSpec> {
    var hasNameParameter = false

    val nameClass = Name::class.asTypeName()

    val mandatoryFields = element.allFields.mapNotNull { field ->
        if (field.isMandatoryInDSL) {
            val poetFieldType = field.type.toPoet()
            if (poetFieldType == nameClass) {
                hasNameParameter = true
            }
            field.name to poetFieldType
        } else null
    }

    val parameterSpecs = mandatoryFields.map { (name, type) -> ParameterSpec.builder(name, type).build() }

    val parameterSpecsWithStringsInsteadOfNames = if (hasNameParameter)
        mandatoryFields.map { (name, type) -> ParameterSpec.builder(name, if (type == nameClass) STRING else type).build() }
    else
        null

    fun build(parameters: List<ParameterSpec>) =
        FunSpec.builder(name)
            .addModifiers(KModifier.INLINE)
            .addAnnotation(irNodeBuilderDslAnnotationTypeName)
            .addTypeVariables(element.poetTypeVariables)
            .addParameters(parameters)
            .addParameters(additionalParameters)
            .addParameter(buildLambdaParameter(element))
            .returns(returnType)
            .apply(customize)

    return listOfNotNull(
        build(parameterSpecs).apply(bodyBuilder).build(),
        parameterSpecsWithStringsInsteadOfNames
            ?.let(::build)
            ?.run {
                addStatement(
                    buildString {
                        append("return ")
                        append(name)
                        parameters.withIndex().joinTo(this, prefix = "(", postfix = ")") { (i, parameter) ->
                            if (i in parameterSpecs.indices && parameterSpecs[i].type == nameClass) {
                                "${nameClass.simpleName}.${Name::guessByFirstCharacter.name}(${parameter.name})"
                            } else {
                                parameter.name
                            }
                        }
                    }
                )
            }
            ?.build()
    )
}

private fun buildTopLevelBuilderFunctions(element: Element): List<FunSpec> {
    val buildingContextParameter = buildBuildingContextParameter()
    return buildIrNodeBuilderFunctions(
        element,
        name = "build${element.typeName}",
        additionalParameters = listOf(buildingContextParameter),
        element.toPoetSelfParameterized(),
        bodyBuilder = {
            addStatement(
                buildString {
                    append("return ")
                    appendCallToBuilder(
                        element,
                        buildingContextParameter.name,
                        parameters.dropLast(2).map { it.name },
                        parameters.last().name
                    )
                    append(".build()")
                }
            )
        }
    )
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
