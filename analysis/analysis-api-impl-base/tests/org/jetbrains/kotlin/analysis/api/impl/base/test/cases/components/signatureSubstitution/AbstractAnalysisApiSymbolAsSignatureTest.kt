/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForDebug
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiSymbolAsSignatureTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(mainFile)
        val actual = analyseForTest(declaration) {
            val symbol = declaration.getSymbol() as KaCallableSymbol
            val signature = symbol.asSignature()
            val renderedSymbol = symbol.render(KaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES)
            val renderedSignature = stringRepresentation(signature)
            prettyPrint {
                appendLine("${KtDeclaration::class.simpleName}: ${declaration::class.simpleName}")

                appendLine("Symbol:")
                appendLine(renderedSymbol)

                appendLine()

                appendLine("Signature:")
                appendLine(renderedSignature)
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
