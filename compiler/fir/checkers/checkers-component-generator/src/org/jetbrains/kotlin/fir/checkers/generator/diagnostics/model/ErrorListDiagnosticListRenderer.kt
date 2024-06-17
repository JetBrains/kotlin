/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model

import org.jetbrains.kotlin.fir.checkers.generator.*
import org.jetbrains.kotlin.fir.checkers.generator.printCopyright
import org.jetbrains.kotlin.fir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.jetbrains.kotlin.generators.tree.printer.printKDoc
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance

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
        printKDoc(diagnosticList.extendedKDoc())
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
        val type = diagnostic.getProperType()
        val escapedName = "\"${diagnostic.name}\""
        val severityOrFeatureForError = when (diagnostic) {
            is RegularDiagnosticData -> diagnostic.severity.name
            is DeprecationDiagnosticData -> diagnostic.featureForError.name
        }
        val positioningStrategy = diagnostic.positioningStrategy.expressionToCreate
        val psiTypeClass = "${diagnostic.psiType.kClass.simpleName!!}::class"

        print("val ${diagnostic.name}: $type")
        diagnostic.parameters.map { it.type }.ifNotEmpty { printTypeArguments(this) }
        print(" = $type(")
        printSeparatedWithComma(listOf(escapedName, severityOrFeatureForError, positioningStrategy, psiTypeClass)) { print(it) }
        print(")")
        println()
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
            when (typeArgument.variance) {
                KVariance.INVARIANT, null -> {}
                KVariance.IN -> print("in ")
                KVariance.OUT -> print("out ")
            }
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
            diagnostic.psiType.collectClassNamesTo(this)
            for (parameter in diagnostic.parameters) {
                parameter.type.collectClassNamesTo(this)
            }
            add("org.jetbrains.kotlin.diagnostics." + diagnostic.getProperType())
            when (diagnostic) {
                is RegularDiagnosticData ->
                    add("org.jetbrains.kotlin.diagnostics.Severity.${diagnostic.severity.name}")
                is DeprecationDiagnosticData ->
                    add("org.jetbrains.kotlin.config.LanguageFeature.${diagnostic.featureForError.name}")
            }
        }
        add(PositioningStrategy.importToAdd)
        add("org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory")
    }


    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>

    private fun DiagnosticData.getProperType(): String = when (this) {
        is RegularDiagnosticData -> "KtDiagnosticFactory"
        is DeprecationDiagnosticData -> "KtDiagnosticFactoryForDeprecation"
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
