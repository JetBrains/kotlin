/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs

import org.jetbrains.kotlin.analysis.api.fir.generator.ConversionContext
import org.jetbrains.kotlin.analysis.api.fir.generator.HLDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.generator.HLDiagnosticList
import org.jetbrains.kotlin.analysis.api.fir.generator.HLDiagnosticParameter
import org.jetbrains.kotlin.fir.checkers.generator.inBracketsWithIndent
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

object FirDiagnosticToKtDiagnosticConverterRenderer : AbstractDiagnosticsDataClassRenderer() {
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticConverter(diagnosticList)
    }

    private fun SmartPrinter.printDiagnosticConverter(diagnosticList: HLDiagnosticList) {
        inBracketsWithIndent("internal val KT_DIAGNOSTIC_CONVERTER = KtDiagnosticConverterBuilder.buildConverter") {
            for (diagnostic in diagnosticList.diagnostics) {
                printConverter(diagnostic)
            }
        }
    }

    private fun SmartPrinter.printConverter(diagnostic: HLDiagnostic) {
        print("add(${diagnostic.original.containingObjectName}.${diagnostic.original.name}")
        if (diagnostic.severity != null) {
            print(".${diagnostic.severity.name.lowercase()}Factory")
        }
        println(") { firDiagnostic ->")
        withIndent {
            println("${diagnostic.implClassName}(")
            withIndent {
                printDiagnosticParameters(diagnostic)
            }
            println(")")
        }
        println("}")
    }

    private fun SmartPrinter.printDiagnosticParameters(diagnostic: HLDiagnostic) {
        printCustomParameters(diagnostic)
        println("firDiagnostic as KtPsiDiagnostic,")
        println("token,")
    }


    private fun SmartPrinter.printCustomParameters(diagnostic: HLDiagnostic) {
        diagnostic.parameters.forEach { parameter ->
            printParameter(parameter)
        }
    }

    private fun SmartPrinter.printParameter(parameter: HLDiagnosticParameter) {
        val expression = parameter.conversion.convertExpression(
            "firDiagnostic.${parameter.originalParameterName}",
            ConversionContext(currentIndentLengthInUnits, indentUnitLength)
        )
        println("$expression,")
    }

    override fun collectImportsForDiagnosticParameter(diagnosticParameter: HLDiagnosticParameter): Collection<String> =
        diagnosticParameter.importsToAdd

    override val defaultImports = listOf(
        "org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic",
        "org.jetbrains.kotlin.fir.builder.FirSyntaxErrors",
        "org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors",
        "org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors",
        "org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors",
        "org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors",
    )
}
