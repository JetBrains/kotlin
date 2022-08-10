/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.getSymbolOfType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.services.SubstitutionParser
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiSignatureSubstitutionTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtCallableDeclaration>(ktFile)
        val actual = analyseForTest(declaration) {
            val symbol = declaration.getSymbolOfType<KtCallableSymbol>()

            val substitutor = SubstitutionParser.parseSubstitutor(ktFile, declaration)

            val signatureBeforeSubstitution = symbol.asSignature()
            val signatureAfterSubstitution = signatureBeforeSubstitution.substitute(substitutor)
            prettyPrint {
                appendLine("KtDeclaration: ${declaration::class.simpleName}")

                appendLine("Symbol:")
                appendLine(symbol.render())

                appendLine()

                appendLine("Signature before substitution:")
                appendLine(stringRepresentation(signatureBeforeSubstitution))

                appendLine()

                appendLine("Signature after substitution:")
                appendLine(stringRepresentation(signatureAfterSubstitution))
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
