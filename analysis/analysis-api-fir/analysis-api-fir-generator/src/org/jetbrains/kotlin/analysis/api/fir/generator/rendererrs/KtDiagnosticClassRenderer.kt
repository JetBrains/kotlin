/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs

import org.jetbrains.kotlin.fir.checkers.generator.collectClassNamesTo
import org.jetbrains.kotlin.fir.checkers.generator.inBracketsWithIndent
import org.jetbrains.kotlin.analysis.api.fir.generator.*
import org.jetbrains.kotlin.analysis.api.fir.generator.printTypeWithShortNames
import org.jetbrains.kotlin.util.SmartPrinter

object KtDiagnosticClassRenderer : AbstractDiagnosticsDataClassRenderer() {
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticClasses(diagnosticList)
    }

    private fun SmartPrinter.printDiagnosticClasses(diagnosticList: HLDiagnosticList) {
        inBracketsWithIndent("sealed interface KtFirDiagnostic<PSI : PsiElement> : KtDiagnosticWithPsi<PSI>") {
            for (diagnostic in diagnosticList.diagnostics) {
                printDiagnosticClass(diagnostic, diagnosticList)
                println()
            }
        }
    }

    private fun SmartPrinter.printDiagnosticClass(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        print("interface ${diagnostic.className} : KtFirDiagnostic<")
        printTypeWithShortNames(diagnostic.original.psiType)
        print(">")
        inBracketsWithIndent {
            println("override val diagnosticClass get() = ${diagnostic.className}::class")
            printDiagnosticParameters(diagnostic, diagnosticList)
        }
    }

    private fun SmartPrinter.printDiagnosticParameters(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        diagnostic.parameters.forEach { parameter ->
            print("val ${parameter.name}: ")
            printTypeWithShortNames(parameter.type) { type ->
                diagnosticList.containsClashingBySimpleNameType(type)
            }
            println()
        }
    }

    override fun collectImportsForDiagnosticParameter(diagnosticParameter: HLDiagnosticParameter): Collection<String> = buildSet {
        diagnosticParameter.type.collectClassNamesTo(this)
    }

    override val defaultImports = listOf(
        "org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnosticWithPsi",
        "com.intellij.psi.PsiElement",
    )
}
