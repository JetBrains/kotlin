/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.GeneratedFile
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.generators.tree.printer.printGeneratedType
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.TREE_GENERATOR_README
import org.jetbrains.kotlin.ir.generator.irFactoryType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.Model
import org.jetbrains.kotlin.ir.generator.stageControllerType
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

@Suppress("DuplicatedCode")
internal fun printFactory(generationPath: File, model: Model): GeneratedFile = printGeneratedType(
    generationPath,
    TREE_GENERATOR_README,
    irFactoryType.packageName,
    irFactoryType.simpleName,
) {
    println("interface ", irFactoryType.simpleName, " {")
    withIndent {
        println("val stageController: ", stageControllerType.render())
        val factoryMethods = model.elements
            .filter { it.isLeaf && it.generateIrFactoryMethod }
            .sortedWith(compareBy({ it.packageName }, { it.name }))
            .map(::FactoryMethod)

        factoryMethods.forEach { printFactoryMethod(it) }

        fun replacement(name: String) = factoryMethods.find { it.name == name } ?: error("Method '$name' not found")

        addDeprecatedFunction(
            replacement("createBlockBody")
                .addParameter(
                    "initializer",
                    Lambda(receiver = IrTree.blockBody, returnType = StandardTypes.unit),
                ),
        ) {
            deprecationMessage = "This method was moved to an extension."
            parameter("startOffset")
            parameter("endOffset")
            parameter("initializer")
        }

        addDeprecatedFunction(
            replacement("createBlockBody")
                .addParameter(
                    "statements",
                    StandardTypes.list.withArgs(IrTree.statement),
                ),
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
            defaultValue["hasEnumEntries"] = "false"
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
            returnType = IrTree.simpleFunction
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
            returnType = IrTree.property
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
    }
    println("}")
}

private class FactoryMethod(val element: Element) {

    val name = "create" + element.name.capitalizeAsciiOnly()

    val parameters = (element.allFields + element.additionalIrFactoryMethodParameters)
        .filterNot { it.name in element.fieldsToSkipInIrFactoryMethod }
        .mapNotNull { field ->
            (field.useInIrFactoryStrategy as? Field.UseFieldAsParameterInIrFactoryStrategy.Yes)?.let {
                FunctionParameter(field.name, field.typeRef, it.defaultValue)
            }
        }
        .sortedBy { it.defaultValue != null } // All parameters with default values must go last
        .toMutableList()

    fun addParameter(name: String, type: TypeRef): FactoryMethod {
        parameters.add(FunctionParameter(name, type))
        return this
    }
}

private const val MAX_FUNCTION_PARAMETERS_ON_ONE_LINE = 2

context(ImportCollector)
private fun SmartPrinter.printFactoryMethod(factoryMethod: FactoryMethod) {
    println()
    printFunctionDeclaration(
        name = factoryMethod.name,
        parameters = factoryMethod.parameters,
        returnType = factoryMethod.element,
        typeParameters = factoryMethod.element.params,
        allParametersOnSeparateLines = factoryMethod.parameters.size > MAX_FUNCTION_PARAMETERS_ON_ONE_LINE,
    )
    println()
}

private class DeprecatedFunctionBuilder(private val replacement: FactoryMethod) {
    val deprecatedFunctionParameters = mutableListOf<FunctionParameter>()
    var oldName = replacement.name
    var returnType: TypeRef = replacement.element
    var deprecationMessage: String? = null
    val defaultValue = mutableMapOf<String, String>()

    fun parameter(name: String, removeDefaultValue: Boolean = false) {
        val replacementParameter =
            replacement.parameters.find { it.name == name } ?: error("Parameter '$name' not found in $replacement")
        deprecatedFunctionParameters.add(if (removeDefaultValue) replacementParameter.copy(defaultValue = null) else replacementParameter)
    }
}

context(ImportCollector)
private fun SmartPrinter.addDeprecatedFunction(
    replacement: FactoryMethod,
    build: DeprecatedFunctionBuilder.() -> Unit
) {
    val builder = DeprecatedFunctionBuilder(replacement)
    builder.build()

    val message = builder.deprecationMessage ?: if (builder.oldName != replacement.name) {
        "The method has been renamed, and its parameters were reordered."
    } else {
        "The method's parameters were reordered."
    }

    println()
    println("@Deprecated(")
    withIndent {
        println("message = \"", message, "\" +")
        println("        \" This variant of the method will be removed when the 2024.2 IntelliJ platform is shipped (see KTIJ-26314).\",")
        println("level = DeprecationLevel.HIDDEN,")
    }
    println(")")
    val allParametersOnSeparateLines = builder.deprecatedFunctionParameters.size > MAX_FUNCTION_PARAMETERS_ON_ONE_LINE
    printFunctionDeclaration(
        name = builder.oldName,
        parameters = builder.deprecatedFunctionParameters,
        returnType = builder.returnType,
        typeParameters = replacement.element.params,
        allParametersOnSeparateLines = allParametersOnSeparateLines,
    )
    print(" = ", replacement.name, "(")

    val renderedParameters = replacement.parameters.mapNotNull { parameter ->
        if (builder.deprecatedFunctionParameters.any { it.name == parameter.name }) {
            parameter.name
        } else {
            builder.defaultValue[parameter.name]
        }
    }

    if (allParametersOnSeparateLines) {
        println()
        withIndent {
            renderedParameters.forEach { println(it, ",") }
        }
    } else {
        print(renderedParameters.joinToString())
    }
    println(")")
}
