/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import org.jetbrains.kotlin.fir.checkers.generator.*
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import org.jetbrains.kotlin.fir.tree.generator.printer.printCopyright
import org.jetbrains.kotlin.fir.tree.generator.printer.printGeneratedMessage
import org.jetbrains.kotlin.fir.tree.generator.printer.useSmartPrinter
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

object ErrorListDiagnosticListRenderer : DiagnosticListRenderer() {
    override fun render(file: File, diagnosticList: DiagnosticList, packageName: String) {
        file.useSmartPrinter {
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
            for (it in diagnosticList.diagnostics) {
                printDiagnostic(it)
            }
        }
    }

    private fun SmartPrinter.printDiagnostic(diagnostic: Diagnostic) {
        print("val ${diagnostic.name} by ${diagnostic.getFactoryFunction()}")
        printTypeArguments(diagnostic.getAllTypeArguments())
        printPositioningStrategy(diagnostic)
        println()
    }

    private fun SmartPrinter.printPositioningStrategy(diagnostic: Diagnostic) {
        print("(")
        if (!diagnostic.hasDefaultPositioningStrategy()) {
            print(diagnostic.positioningStrategy.expressionToCreate)
        }
        print(")")
    }


    @OptIn(ExperimentalStdlibApi::class)
    private fun Diagnostic.getAllTypeArguments(): List<KType> = buildList {
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
        diagnosticList.diagnostics.forEach { diagnostic ->
            diagnostic.getAllTypeArguments().mapTo(this) { it.kClass.qualifiedName!! }
            if (!diagnostic.hasDefaultPositioningStrategy()) {
                add(diagnostic.positioningStrategy.import)
            }
        }
    }


    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>

    private fun Diagnostic.getFactoryFunction(): String =
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