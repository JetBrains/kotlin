/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator.rendererrs

import org.jetbrains.kotlin.fir.checkers.generator.inBracketsWithIndent
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.*
import org.jetbrains.kotlin.idea.frontend.api.fir.generator.printTypeWithShortNames
import org.jetbrains.kotlin.fir.checkers.generator.collectClassNamesTo
import org.jetbrains.kotlin.util.SmartPrinter
import org.jetbrains.kotlin.util.withIndent

object KtDiagnosticClassImplementationRenderer : AbstractDiagnosticsDataClassRenderer() {
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticClassesImplementation(diagnosticList)
    }

    private fun SmartPrinter.printDiagnosticClassesImplementation(diagnosticList: HLDiagnosticList) {
        for (diagnostic in diagnosticList.diagnostics) {
            printDiagnosticImplementation(diagnostic)
            println()
        }
    }

    private fun SmartPrinter.printDiagnosticImplementation(diagnostic: HLDiagnostic) {
        println("internal class ${diagnostic.implClassName}(")
        withIndent {
            printParameters(diagnostic)
        }
        print(") : KtFirDiagnostic.${diagnostic.className}(), KtAbstractFirDiagnostic<")
        printTypeWithShortNames(diagnostic.original.psiType)
        print(">")
        inBracketsWithIndent {
            println("override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)")
        }
    }

    private fun SmartPrinter.printParameters(diagnostic: HLDiagnostic) {
        for (parameter in diagnostic.parameters) {
            printParameter(parameter)
        }
        println("firDiagnostic: FirPsiDiagnostic<*>,")
        println("override val token: ValidityToken,")
    }

    private fun SmartPrinter.printParameter(parameter: HLDiagnosticParameter) {
        print("override val ${parameter.name}: ")
        printTypeWithShortNames(parameter.type)
        println(",")
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun collectImportsForDiagnosticParameter(diagnosticParameter: HLDiagnosticParameter): Collection<String> = buildSet {
        diagnosticParameter.type.collectClassNamesTo(this)
    }

    override val defaultImports = listOf(
        "org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef",
        "org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic",
        "org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken",
    )
}
