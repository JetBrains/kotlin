/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.getSymbolOfType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.SubstitutionParser
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractAnalysisApiSubstitutorsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtCallableDeclaration>(mainFile)
        val actual = analyseForTest(declaration) {
            val substitutor = SubstitutionParser.parseSubstitutor(mainFile, declaration)
            val symbol = declaration.getSymbolOfType<KtCallableSymbol>()
            val type = symbol.returnType
            val substituted = substitutor.substitute(type)
            val substitutedOrNull = substitutor.substituteOrNull(type)

            prettyPrint {
                appendLine("PSI type: ${declaration.typeReference?.text}")
                appendLine("KtType: ${type.render(position = Variance.INVARIANT)}")
                appendLine("substitutor.substitute: ${substituted.render(position = Variance.INVARIANT)}")
                appendLine("substitutor.substituteOrNull: ${substitutedOrNull?.render(position = Variance.INVARIANT)}")
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
