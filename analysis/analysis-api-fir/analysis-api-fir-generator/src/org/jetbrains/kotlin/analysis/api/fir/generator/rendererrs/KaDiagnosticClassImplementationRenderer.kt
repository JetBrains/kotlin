/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs

import org.jetbrains.kotlin.analysis.api.fir.generator.HLDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.generator.HLDiagnosticList
import org.jetbrains.kotlin.analysis.api.fir.generator.HLDiagnosticParameter
import org.jetbrains.kotlin.analysis.api.fir.generator.printTypeWithShortNames
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import kotlin.reflect.KType

object KaDiagnosticClassImplementationRenderer : AbstractDiagnosticsDataClassRenderer() {
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticClassesImplementation(diagnosticList)
    }

    private fun SmartPrinter.printDiagnosticClassesImplementation(diagnosticList: HLDiagnosticList) {
        for (diagnostic in diagnosticList.diagnostics) {
            printDiagnosticImplementation(diagnostic, diagnosticList)
            println()
        }
    }

    private fun SmartPrinter.printDiagnosticImplementation(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        println("internal class ${diagnostic.implClassName}(")
        withIndent {
            printParameters(diagnostic, diagnosticList)
        }
        print(") : KaAbstractFirDiagnostic<")
        printTypeWithShortNames(diagnostic.original.psiType)
        println(">(firDiagnostic, token), KaFirDiagnostic.${diagnostic.className}")
    }

    private fun SmartPrinter.printParameters(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        for (parameter in diagnostic.parameters) {
            printParameter(parameter, diagnosticList)
        }
        println("firDiagnostic: KtPsiDiagnostic,")
        println("token: KaLifetimeToken,")
    }

    private fun SmartPrinter.printParameter(parameter: HLDiagnosticParameter, diagnosticList: HLDiagnosticList) {
        print("override val ${parameter.name}: ")
        printTypeWithShortNames(parameter.type) {
            diagnosticList.containsClashingBySimpleNameType(it)
        }
        println(",")
    }

    override fun collectImportsForDiagnosticParameterReflect(diagnosticParameter: HLDiagnosticParameter): Collection<KType> {
        return listOf(diagnosticParameter.type)
    }

    override fun collectImportsForDiagnosticParameterSimple(diagnosticParameter: HLDiagnosticParameter): Collection<String> {
        return emptyList()
    }

    override val defaultImports = listOf(
        "org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic",
        "org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken",
    )
}
