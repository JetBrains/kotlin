/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs

import org.jetbrains.kotlin.analysis.api.fir.generator.HLDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.generator.HLDiagnosticList
import org.jetbrains.kotlin.analysis.api.fir.generator.HLDiagnosticParameter
import org.jetbrains.kotlin.analysis.api.fir.generator.printTypeWithShortNames
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import kotlin.reflect.KType

object KaDiagnosticClassRenderer : AbstractDiagnosticsDataClassRenderer() {
    override fun SmartPrinter.render(diagnosticList: HLDiagnosticList, packageName: String) {
        printHeader(packageName, diagnosticList)
        printDiagnosticClasses(diagnosticList)
    }

    private fun SmartPrinter.printDiagnosticClasses(diagnosticList: HLDiagnosticList) {
        println(SUBCLASS_OPT_IN_REQUIRED)
        printBlock("public interface KaFirDiagnostic<PSI : PsiElement> : KaDiagnosticWithPsi<PSI>") {
            for (diagnostic in diagnosticList.diagnostics) {
                printDiagnosticClass(diagnostic, diagnosticList)
                println()
            }
        }
    }

    private fun SmartPrinter.printDiagnosticClass(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        println(SUBCLASS_OPT_IN_REQUIRED)
        print("public interface ${diagnostic.className} : KaFirDiagnostic<")
        printTypeWithShortNames(diagnostic.original.psiType)
        print(">")
        printBlock {
            println("override val diagnosticClass: KClass<${diagnostic.className}>")
            withIndent {
                println("get() = ${diagnostic.className}::class")
            }

            printDiagnosticParameters(diagnostic, diagnosticList)
        }
    }

    private fun SmartPrinter.printDiagnosticParameters(diagnostic: HLDiagnostic, diagnosticList: HLDiagnosticList) {
        if (diagnostic.parameters.isEmpty()) return

        println()

        diagnostic.parameters.forEach { parameter ->
            print("public val ${parameter.name}: ")
            printTypeWithShortNames(parameter.type) { type ->
                diagnosticList.containsClashingBySimpleNameType(type)
            }

            println()
        }
    }

    override fun collectImportsForDiagnosticParameterReflect(diagnosticParameter: HLDiagnosticParameter): Collection<KType> {
        return listOf(diagnosticParameter.type)
    }

    override fun collectImportsForDiagnosticParameterSimple(diagnosticParameter: HLDiagnosticParameter): Collection<String> {
        return emptyList()
    }

    override val defaultImports = listOf(
        "org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi",
        "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
        "com.intellij.psi.PsiElement",
        "kotlin.reflect.KClass",
    )
}

/**
 * Diagnostics are not `sealed`: the list is driven by the compiler and changes freely, so the hierarchy cannot be closed.
 */
private const val SUBCLASS_OPT_IN_REQUIRED = "@SubclassOptInRequired(KaImplementationDetail::class)"
