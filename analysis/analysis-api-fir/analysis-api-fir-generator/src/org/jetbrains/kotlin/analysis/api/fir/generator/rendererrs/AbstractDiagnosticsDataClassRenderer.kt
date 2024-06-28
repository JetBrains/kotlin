/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs

import org.jetbrains.kotlin.analysis.api.fir.generator.*
import org.jetbrains.kotlin.fir.checkers.generator.collectClassNamesTo
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticListRenderer
import org.jetbrains.kotlin.fir.checkers.generator.printImports
import org.jetbrains.kotlin.fir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File
import kotlin.reflect.KType

abstract class AbstractDiagnosticsDataClassRenderer : DiagnosticListRenderer() {
    override fun render(file: File, diagnosticList: DiagnosticList, packageName: String, starImportsToAdd: Set<String>) {
        val hlDiagnosticsList = HLDiagnosticConverter.convert(diagnosticList)
        file.writeToFileUsingSmartPrinterIfFileContentChanged { render(hlDiagnosticsList, packageName) }
    }

    private fun SmartPrinter.collectAndPrintImports(diagnosticList: HLDiagnosticList) {
        val imports = collectImports(diagnosticList)
        printImports(imports)
    }

    protected fun SmartPrinter.printHeader(packageName: String, diagnosticList: HLDiagnosticList) {
        printCopyright()
        println("package $packageName")
        println()
        collectAndPrintImports(diagnosticList)
        println()
        printGeneratedMessage()
    }

    protected fun collectImports(diagnosticList: HLDiagnosticList): Collection<String> = buildSet {
        addAll(defaultImports)
        for (diagnostic in diagnosticList.diagnostics) {
            diagnostic.original.psiType.collectClassNamesTo(this)
            diagnostic.parameters.forEach { diagnosticParameter ->
                addAll(collectImportsForDiagnosticParameter(diagnosticParameter))
            }
        }
    }

    protected fun HLDiagnosticList.containsClashingBySimpleNameType(type: KType): Boolean {
        return diagnostics.any { it.className == type.simpleName }
    }

    protected abstract fun collectImportsForDiagnosticParameter(diagnosticParameter: HLDiagnosticParameter): Collection<String>

    protected abstract fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String)

    protected abstract val defaultImports: Collection<String>
}
