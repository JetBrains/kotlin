/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.substututorFactory

import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.components.buildSubstitutor
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.getSymbolOfType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.services.getSymbolByName
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSubstitutorBuilderTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile)
        val actual = analyseForTest(declaration) {
            val symbol = declaration.getSymbolOfType<KtCallableSymbol>()

            val substitutor = buildSubstitutor {
                substitution(getSymbolByName(ktFile, "A"), builtinTypes.INT)
                substitution(getSymbolByName(ktFile, "B"), builtinTypes.LONG)
                substitution(
                    getSymbolByName(ktFile, "C"),
                    buildClassType(StandardClassIds.List) {
                        argument(builtinTypes.STRING)
                    }
                )
            }

            val signatureAfterSubstitution = symbol.substitute(substitutor)
            prettyPrint {
                appendLine("KtDeclaration: ${declaration::class.simpleName}")

                appendLine("Symbol:")
                appendLine(symbol.render())

                appendLine()

                appendLine("Substitutor:")
                appendLine(stringRepresentation(substitutor))

                appendLine()

                appendLine("Signature after substitution:")
                appendLine(stringRepresentation(signatureAfterSubstitution))
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
