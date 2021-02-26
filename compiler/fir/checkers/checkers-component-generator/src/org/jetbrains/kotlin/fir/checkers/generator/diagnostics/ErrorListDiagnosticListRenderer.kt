/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import org.jetbrains.kotlin.fir.checkers.generator.*
import org.jetbrains.kotlin.fir.tree.generator.printer.printCopyright
import org.jetbrains.kotlin.fir.tree.generator.printer.printGeneratedMessage
import org.jetbrains.kotlin.fir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.jetbrains.kotlin.util.SmartPrinter
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

object ErrorListDiagnosticListRenderer : DiagnosticListRenderer() {
    override fun render(file: File, diagnosticList: DiagnosticList, packageName: String) {
        file.writeToFileUsingSmartPrinterIfFileContentChanged {
            render(diagnosticList, packageName)
        }
    }

    private fun SmartPrinter.render(diagnosticList: DiagnosticList, packageName: String) {
        printCopyright()
        println("package $packageName")
        println()
        collectAndPrintImports(diagnosticList)
        printGeneratedMessage()
        printErrorsObject(diagnosticList)
    }

    private fun SmartPrinter.printErrorsObject(diagnosticList: DiagnosticList) {
        inBracketsWithIndent("object FirErrors") {
            for (group in diagnosticList.groups) {
                printDiagnosticGroup(group.name, group.diagnostics)
                println()
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
        printPositioningStrategy(diagnostic)
        println()
    }

    private fun SmartPrinter.printPositioningStrategy(diagnostic: DiagnosticData) {
        print("(")
        if (!diagnostic.hasDefaultPositioningStrategy()) {
            print(diagnostic.positioningStrategy.expressionToCreate)
        }
        print(")")
    }


    @OptIn(ExperimentalStdlibApi::class)
    private fun DiagnosticData.getAllTypeArguments(): List<KType> = buildList {
        add(sourceElementType)
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
    }

    private fun SmartPrinter.printTypeArgument(typeArgument: KTypeProjection) {
        val typeArgumentType = typeArgument.type
        if (typeArgumentType == null) {
            print("*")
        } else {
            printType(typeArgumentType)
        }
    }

    private fun SmartPrinter.collectAndPrintImports(diagnosticList: DiagnosticList) {
        val imports = collectImports(diagnosticList)
        printImports(imports)
        println()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun collectImports(diagnosticList: DiagnosticList): Collection<String> = buildSet {
        diagnosticList.allDiagnostics.forEach { diagnostic ->
            for (typeArgument in diagnostic.getAllTypeArguments()) {
                typeArgument.collectClassNamesTo(this)
            }
            if (!diagnostic.hasDefaultPositioningStrategy()) {
                add(PositioningStrategy.importToAdd)
            }
        }
    }


    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>

    private fun DiagnosticData.getFactoryFunction(): String =
        severity.name.toLowerCase() + parameters.size
}

private inline fun <T> SmartPrinter.printSeparatedWithComma(list: List<T>, printItem: (T) -> Unit) {
    list.forEachIndexed { index, element ->
        printItem(element)
        if (index != list.lastIndex) {
            print(", ")
        }
    }
}
