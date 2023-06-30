/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model

import org.jetbrains.kotlin.fir.checkers.generator.collectClassNamesTo
import org.jetbrains.kotlin.fir.checkers.generator.inBracketsWithIndent
import org.jetbrains.kotlin.fir.checkers.generator.printImports
import org.jetbrains.kotlin.fir.tree.generator.printer.printCopyright
import org.jetbrains.kotlin.fir.tree.generator.printer.printGeneratedMessage
import org.jetbrains.kotlin.fir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

object ErrorListDiagnosticListRenderer : DiagnosticListRenderer() {
    const val BASE_PACKAGE = "org.jetbrains.kotlin.fir.analysis.diagnostics"
    const val DIAGNOSTICS_PACKAGE = "org.jetbrains.kotlin.diagnostics"

    override fun render(
        file: File,
        diagnosticList: DiagnosticList,
        packageName: String,
        starImportsToAdd: Set<String>
    ) {
        file.writeToFileUsingSmartPrinterIfFileContentChanged {
            render(diagnosticList, packageName, starImportsToAdd)
        }
    }

    private fun SmartPrinter.render(
        diagnosticList: DiagnosticList,
        packageName: String,
        starImportsToAdd: Set<String>,
    ) {
        printCopyright()
        println("package $packageName")
        println()
        collectAndPrintImports(diagnosticList, packageName, starImportsToAdd)
        printGeneratedMessage()
        printErrorsObject(diagnosticList)
    }

    private fun SmartPrinter.printErrorsObject(diagnosticList: DiagnosticList) {
        inBracketsWithIndent("object ${diagnosticList.objectName}") {
            for (group in diagnosticList.groups) {
                printDiagnosticGroup(group.name, group.diagnostics)
                println()
            }
            inBracketsWithIndent("init") {
                println("RootDiagnosticRendererFactory.registerFactory(${diagnosticList.objectName}DefaultMessages)")
            }
        }
    }

    private fun SmartPrinter.printDiagnosticGroup(
        group: String,
        diagnostics: List<DiagnosticData>
    ) {
        println("// $group")
        for (it in diagnostics) {
            printDiagnostic(it)
        }
    }

    private fun SmartPrinter.printDiagnostic(diagnostic: DiagnosticData) {
        print("val ${diagnostic.name} by ${diagnostic.getFactoryFunction()}")
        printTypeArguments(diagnostic.getAllTypeArguments())
        printPositioningStrategyAndLanguageFeature(diagnostic)
        println()
    }

    private fun SmartPrinter.printPositioningStrategyAndLanguageFeature(diagnostic: DiagnosticData) {
        val argumentsList = buildList {
            if (diagnostic is DeprecationDiagnosticData) {
                add(diagnostic.featureForError.name)
            }
            if (!diagnostic.hasDefaultPositioningStrategy()) {
                add(diagnostic.positioningStrategy.expressionToCreate)
            }
        }
        print(argumentsList.joinToString(", ", prefix = "(", postfix = ")"))
    }


    private fun DiagnosticData.getAllTypeArguments(): List<KType> = buildList {
        add(psiType)
        parameters.mapTo(this) { it.type }
    }

    private fun SmartPrinter.printTypeArguments(typeArguments: List<KType>) {
        print("<")
        printSeparatedWithComma(typeArguments) { typeArgument ->
            printType(typeArgument)
        }
        print(">")
    }

    private fun SmartPrinter.printType(type: KType) {
        print(type.kClass.simpleName!!)
        if (type.arguments.isNotEmpty()) {
            print("<")
            printSeparatedWithComma(type.arguments) { typeArgument ->
                printTypeArgument(typeArgument)
            }
            print(">")
        }
        if (type.isMarkedNullable) {
            print("?")
        }
    }

    private fun SmartPrinter.printTypeArgument(typeArgument: KTypeProjection) {
        val typeArgumentType = typeArgument.type
        if (typeArgumentType == null) {
            print("*")
        } else {
            printType(typeArgumentType)
        }
    }

    private fun SmartPrinter.collectAndPrintImports(diagnosticList: DiagnosticList, packageName: String, starImportsToAdd: Set<String>) {
        val imports = collectImports(diagnosticList, packageName, starImportsToAdd)
        printImports(imports)
        println()
    }

    private fun collectImports(
        diagnosticList: DiagnosticList,
        packageName: String,
        starImportsToAdd: Set<String>
    ): Collection<String> = buildSet {
        for (starImport in starImportsToAdd) {
            if (starImport != packageName) {
                add("$starImport.*")
            }
        }
        diagnosticList.allDiagnostics.forEach { diagnostic ->
            for (typeArgument in diagnostic.getAllTypeArguments()) {
                typeArgument.collectClassNamesTo(this)
            }
            if (!diagnostic.hasDefaultPositioningStrategy()) {
                add(PositioningStrategy.importToAdd)
            }
        }
        for (deprecationDiagnostic in diagnosticList.allDiagnostics.filterIsInstance<DeprecationDiagnosticData>()) {
            add("org.jetbrains.kotlin.config.LanguageFeature.${deprecationDiagnostic.featureForError.name}")
        }
        add("org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory")
    }


    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>

    private fun DiagnosticData.getFactoryFunction(): String = when (this) {
        is RegularDiagnosticData -> severity.name.lowercase()
        is DeprecationDiagnosticData -> "deprecationError"
    } + parameters.size
}

private inline fun <T> SmartPrinter.printSeparatedWithComma(list: List<T>, printItem: (T) -> Unit) {
    list.forEachIndexed { index, element ->
        printItem(element)
        if (index != list.lastIndex) {
            print(", ")
        }
    }
}
