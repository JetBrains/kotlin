/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs

import org.jetbrains.kotlin.analysis.api.fir.generator.qualifiedName
import org.jetbrains.kotlin.analysis.api.fir.generator.simpleName
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DeprecationDiagnosticData
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticData
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticListRenderer
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.RegularDiagnosticData
import org.jetbrains.kotlin.fir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.jetbrains.kotlin.generators.util.printCopyright
import org.jetbrains.kotlin.generators.util.printGeneratedMessage
import org.jetbrains.kotlin.generators.util.printImports
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File


object DiagnosticFactoryToPsiElementMapRenderer : DiagnosticListRenderer() {
    private val containerFqNames: Map<String, String> = linkedMapOf(
        "FirErrors" to "org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors",
        "FirJvmErrors" to "org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors",
        "FirJsErrors" to "org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors",
        "FirNativeErrors" to "org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors",
        "FirWasmErrors" to "org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors",
        "FirWebCommonErrors" to "org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors",
        "FirSyntaxErrors" to "org.jetbrains.kotlin.fir.builder.FirSyntaxErrors",
    )

    override fun render(file: File, diagnosticList: DiagnosticList, packageName: String, starImportsToAdd: Set<String>) {
        val diagnostics = diagnosticList.allDiagnostics
        val diagnosticsByContainer = diagnostics.groupBy { it.containingObjectName }

        require(diagnosticsByContainer.keys == containerFqNames.keys) {
            val unknownContainers = diagnosticsByContainer.keys - containerFqNames.keys
            val redundantContainers = containerFqNames.keys - diagnosticsByContainer.keys
            buildString {
                appendLine("Unregistered diagnostic containers: $unknownContainers")
                appendLine("Redundant diagnostic containers: $redundantContainers")
                appendLine("Align container names with their fully qualified names in `${DiagnosticFactoryToPsiElementMapRenderer::containerFqNames.name}`")
            }
        }

        val psiTypesWithClashingNames = diagnostics
            .map { it.psiType.qualifiedName }
            .distinct()
            .groupBy { it.substringAfterLast('.') }
            .filterValues { it.size > 1 }

        require(psiTypesWithClashingNames.isEmpty()) {
            "PSI types with the same simple name cannot be imported together: ${psiTypesWithClashingNames.values}"
        }

        file.writeToFileUsingSmartPrinterIfFileContentChanged {
            printCopyright()
            println("package $packageName")
            println()
            printImports(
                packageName,
                importableTypes = diagnostics.map { it.psiType },
                simpleImports = listOf("com.intellij.psi.PsiElement", "org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryN") +
                        containerFqNames.values,
                starImports = starImportsToAdd,
            )
            printGeneratedMessage()
            println("/**")
            println(" * Maps each compiler diagnostic factory to the type of PSI element its diagnostics are reported on.")
            println(" * Factories declared outside of the compiler diagnostic lists, e.g., in compiler plugins, are absent.")
            println(" */")
            println("internal val diagnosticFactoryToPsiElementMap: Map<KtDiagnosticFactoryN, Class<out PsiElement>> = mapOf(")
            withIndent {
                for ([container, diagnostics] in diagnosticsByContainer) {
                    for (diagnostic in diagnostics) {
                        printEntries(container, diagnostic)
                    }
                }
            }
            println(")")
        }
    }

    private fun SmartPrinter.printEntries(container: String, diagnostic: DiagnosticData) {
        val factory = "$container.${diagnostic.name}"
        val psiType = "${diagnostic.psiType.simpleName}::class.java"
        when (diagnostic) {
            is RegularDiagnosticData -> println("$factory to $psiType,")
            is DeprecationDiagnosticData -> {
                println("$factory.warningFactory to $psiType,")
                println("$factory.errorFactory to $psiType,")
            }
        }
    }
}
