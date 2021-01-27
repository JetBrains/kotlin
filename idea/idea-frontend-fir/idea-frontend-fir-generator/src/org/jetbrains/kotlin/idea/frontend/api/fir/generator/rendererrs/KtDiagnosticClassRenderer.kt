/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator.rendererrs

import org.jetbrains.kotlin.fir.checkers.generator.inBracketsWithIndent
import org.jetbrains.kotlin.fir.tree.generator.printer.SmartPrinter
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.*
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.collectClassNamesTo
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.printTypeWithShortNames

object KtDiagnosticClassRenderer : AbstractDiagnosticsDataClassRenderer() {
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticClasses(diagnosticList)
    }

    private fun SmartPrinter.printDiagnosticClasses(diagnosticList: HLDiagnosticList) {
        inBracketsWithIndent("sealed class KtFirDiagnostic : KtDiagnosticWithPsi") {
            for (diagnostic in diagnosticList.diagnostics) {
                printDiagnosticClass(diagnostic)
                println()
            }
        }
    }

    private fun SmartPrinter.printDiagnosticClass(diagnostic: HLDiagnostic) {
        inBracketsWithIndent("abstract class ${diagnostic.className} : KtFirDiagnostic()") {
            println("override val diagnosticClass get() = ${diagnostic.className}::class")
            printPsiParameter(diagnostic)
            printDiagnosticParameters(diagnostic)
        }
    }

    private fun SmartPrinter.printPsiParameter(diagnostic: HLDiagnostic) {
        if (diagnostic.original.psiType.classifier != PsiElement::class) {
            print("abstract override val psi: ")
            printTypeWithShortNames(diagnostic.original.psiType)
            println()
        }
    }

    private fun SmartPrinter.printDiagnosticParameters(diagnostic: HLDiagnostic) {
        diagnostic.parameters.forEach { parameter ->
            print("abstract val ${parameter.name}: ")
            printTypeWithShortNames(parameter.type)
            println()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun collectImportsForDiagnosticParameter(diagnosticParameter: HLDiagnosticParameter): Collection<String> = buildSet {
        diagnosticParameter.type.collectClassNamesTo(this)
    }

    override val defaultImports = listOf(
        "org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi",
    )
}