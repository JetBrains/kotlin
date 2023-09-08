/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import com.squareup.kotlinpoet.*
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.Packages
import org.jetbrains.kotlin.ir.generator.config.UseFieldAsParameterInIrFactoryStrategy
import org.jetbrains.kotlin.ir.generator.model.Model
import org.jetbrains.kotlin.ir.generator.util.GeneratedFile
import org.jetbrains.kotlin.ir.generator.util.parameterizedByIfAny
import org.jetbrains.kotlin.ir.generator.util.tryParameterizedBy
import org.jetbrains.kotlin.generators.tree.type
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.io.File

internal val IR_FACTORY_TYPE = type(Packages.declarations, "IrFactory")

@Suppress("DuplicatedCode")
internal fun printFactory(generationPath: File, model: Model): GeneratedFile {
    val visitorType = TypeSpec.interfaceBuilder(IR_FACTORY_TYPE.toPoet() as ClassName).apply {
        addProperty("stageController", ClassName(Packages.declarations, "StageController"))
        model.elements
            .filter { it.isLeaf && it.generateIrFactoryMethod }
            .sortedWith(compareBy({ it.packageName }, { it.name }))
            .forEach { element ->
                val typeParams = element.params.map { it.toPoet() }
                addFunction(
                    FunSpec.builder("create${element.name.capitalizeAsciiOnly()}")
                        .addTypeVariables(typeParams)
                        .apply {
                            val fields = (element.allFieldsRecursively() + element.additionalFactoryMethodParameters)
                                .filterNot { it.name in element.fieldsToSkipInIrFactoryMethod }
                                .mapNotNull { field ->
                                    (field.useInIrFactoryStrategy as? UseFieldAsParameterInIrFactoryStrategy.Yes)?.let {
                                        field to it.defaultValue
                                    }
                                }
                                .sortedBy { (_, defaultValue) -> defaultValue != null } // All parameters with default values must go last
                            fields.forEach { (field, defaultValue) ->
                                addParameter(
                                    ParameterSpec.builder(field.name, field.type.toPoet().copy(nullable = field.nullable))
                                        .defaultValue(defaultValue)
                                        .build(),
                                )
                            }
                        }
                        .returns(element.toPoet().parameterizedByIfAny(typeParams))
                        .addModifiers(KModifier.ABSTRACT)
                        .build(),
                )
            }

        fun replacement(name: String) = funSpecs.find { it.name == name } ?: error("Method '$name' not found")

        addDeprecatedFunction(
            replacement("createBlockBody")
                .toBuilder()
                .addParameter(
                    "initializer",
                    LambdaTypeName.get(receiver = IrTree.blockBody.toPoet(), returnType = UNIT)
                )
                .build()
        ) {
            deprecationMessage = "This method was moved to an extension."
            parameter("startOffset")
            parameter("endOffset")
            parameter("initializer")
        }

        addDeprecatedFunction(
            replacement("createBlockBody")
                .toBuilder()
                .addParameter(
                    "statements",
                    LIST.tryParameterizedBy(IrTree.statement.toPoet())
                )
                .build()
        ) {
            deprecationMessage = "This method was moved to an extension."
            parameter("startOffset")
            parameter("endOffset")
            parameter("statements")
        }

        addDeprecatedFunction(replacement("createClass")) {
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("symbol")
            parameter("name")
            parameter("kind")
            parameter("visibility")
            parameter("modality")
            parameter("isCompanion")
            parameter("isInner")
            parameter("isData")
            parameter("isExternal")
            parameter("isValue")
            parameter("isExpect")
            parameter("isFun")
            parameter("source")
        }

        addDeprecatedFunction(replacement("createConstructor")) {
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("symbol")
            parameter("name")
            parameter("visibility")
            parameter("returnType")
            parameter("isInline")
            parameter("isExternal", removeDefaultValue = true)
            parameter("isPrimary")
            parameter("isExpect")
            parameter("containerSource")
        }

        addDeprecatedFunction(replacement("createEnumEntry")) {
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("symbol")
            parameter("name")
        }

        addDeprecatedFunction(replacement("createExpressionBody")) {
            deprecationMessage = "This method was moved to an extension."
            parameter("expression")
        }

        addDeprecatedFunction(replacement("createField")) {
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("symbol")
            parameter("name")
            parameter("type")
            parameter("visibility")
            parameter("isFinal")
            parameter("isExternal", removeDefaultValue = true)
            parameter("isStatic")
        }

        addDeprecatedFunction(replacement("createFunctionWithLateBinding")) {
            returnType = IrTree.simpleFunction.toPoet()
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("name")
            parameter("visibility")
            parameter("modality")
            parameter("returnType")
            parameter("isInline")
            parameter("isExternal", removeDefaultValue = true)
            parameter("isTailrec")
            parameter("isSuspend")
            parameter("isOperator")
            parameter("isInfix")
            parameter("isExpect")
        }

        addDeprecatedFunction(replacement("createLocalDelegatedProperty")) {
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("symbol")
            parameter("name")
            parameter("type")
            parameter("isVar")
        }

        addDeprecatedFunction(replacement("createProperty")) {
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("symbol")
            parameter("name")
            parameter("visibility")
            parameter("modality")
            parameter("isVar")
            parameter("isConst")
            parameter("isLateinit")
            parameter("isDelegated")
            parameter("isExternal", removeDefaultValue = true)
            parameter("isExpect")
            parameter("isFakeOverride")
            parameter("containerSource")
        }

        addDeprecatedFunction(replacement("createPropertyWithLateBinding")) {
            returnType = IrTree.property.toPoet()
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("name")
            parameter("visibility")
            parameter("modality")
            parameter("isVar")
            parameter("isConst")
            parameter("isLateinit")
            parameter("isDelegated")
            parameter("isExternal", removeDefaultValue = true)
            parameter("isExpect", removeDefaultValue = true)
        }

        addDeprecatedFunction(replacement("createSimpleFunction")) {
            oldName = "createFunction"
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("symbol")
            parameter("name")
            parameter("visibility")
            parameter("modality")
            parameter("returnType")
            parameter("isInline")
            parameter("isExternal", removeDefaultValue = true)
            parameter("isTailrec")
            parameter("isSuspend")
            parameter("isOperator")
            parameter("isInfix")
            parameter("isExpect")
            parameter("isFakeOverride")
            parameter("containerSource")
        }

        addDeprecatedFunction(replacement("createTypeAlias")) {
            parameter("startOffset")
            parameter("endOffset")
            parameter("symbol")
            parameter("name")
            parameter("visibility")
            parameter("expandedType")
            parameter("isActual")
            parameter("origin")
        }

        addDeprecatedFunction(replacement("createTypeParameter")) {
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("symbol")
            parameter("name")
            parameter("index")
            parameter("isReified")
            parameter("variance")
        }

        addDeprecatedFunction(replacement("createValueParameter")) {
            parameter("startOffset")
            parameter("endOffset")
            parameter("origin")
            parameter("symbol")
            parameter("name")
            parameter("index")
            parameter("type")
            parameter("varargElementType")
            parameter("isCrossinline")
            parameter("isNoinline")
            parameter("isHidden")
            parameter("isAssignable")
        }
    }.build()

    return printTypeCommon(generationPath, IR_FACTORY_TYPE.packageName, visitorType)
}

private class DeprecatedFunctionBuilder(private val replacement: FunSpec) {

    val deprecatedFunctionParameterSpecs = mutableListOf<ParameterSpec>()

    var oldName = replacement.name

    var returnType = replacement.returnType

    var deprecationMessage: String? = null

    fun parameter(name: String, removeDefaultValue: Boolean = false) {
        val replacementParameter =
            replacement.parameters.find { it.name == name } ?: error("Parameter '$name' not found in $replacement")
        deprecatedFunctionParameterSpecs.add(
            replacementParameter
                .toBuilder()
                .applyIf(removeDefaultValue) {
                    defaultValue(null)
                }
                .build()
        )
    }
}

private fun TypeSpec.Builder.addDeprecatedFunction(
    replacement: FunSpec,
    build: DeprecatedFunctionBuilder.() -> Unit
): TypeSpec.Builder {

    val builder = DeprecatedFunctionBuilder(replacement)
    builder.build()

    val message = builder.deprecationMessage ?: if (builder.oldName != replacement.name) {
        "The method has been renamed, and its parameters were reordered."
    } else {
        "The method's parameters were reordered."
    }

    return addFunction(
        FunSpec.builder(builder.oldName)
            .addTypeVariables(replacement.typeVariables)
            .addCode(
                CodeBlock.builder()
                    .add("return ")
                    .add("%N(\n", replacement)
                    .apply {
                        for (parameter in replacement.parameters) {
                            if (builder.deprecatedFunctionParameterSpecs.any { it.name == parameter.name }) {
                                indent()
                                add("%N,\n", parameter)
                                unindent()
                            }
                        }
                    }
                    .add(")")
                    .build()
            )
            .addAnnotation(
                AnnotationSpec.builder(Deprecated::class)
                    .addMember(
                        "message = %S + %S",
                        message,
                        " This variant of the method will be removed when the 2024.2 IntelliJ platform is shipped (see KTIJ-26314).",
                    )
                    .addMember("level = %T.%N", DeprecationLevel::class, DeprecationLevel.HIDDEN.name)
                    .build(),
            )
            .addParameters(builder.deprecatedFunctionParameterSpecs)
            .apply {
                builder.returnType?.let { returns(it) }
            }
            .build(),
    )
}