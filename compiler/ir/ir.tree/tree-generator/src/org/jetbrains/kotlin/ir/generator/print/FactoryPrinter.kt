/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.ImportCollector
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.GeneratedFile
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.generators.tree.printer.printGeneratedType
import org.jetbrains.kotlin.generators.tree.render
import org.jetbrains.kotlin.ir.generator.TREE_GENERATOR_README
import org.jetbrains.kotlin.ir.generator.irFactoryType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.Model
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
            .filter { it.implementations.isNotEmpty() && it.generateIrFactoryMethod }
            .sortedWith(compareBy({ it.packageName }, { it.name }))
            .map(::FactoryMethod)

        factoryMethods.forEach { printFactoryMethod(it) }
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
