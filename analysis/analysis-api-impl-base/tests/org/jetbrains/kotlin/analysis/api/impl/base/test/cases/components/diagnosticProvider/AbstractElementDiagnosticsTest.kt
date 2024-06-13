/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractElementDiagnosticsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val targetDeclaration = testServices.expressionMarkerProvider.getSelectedElementOfType<KtElement>(mainFile)

        analyze(mainFile) {
            val diagnostics = targetDeclaration.getDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)

            val actualText = buildString {
                if (diagnostics.isNotEmpty()) {
                    for (diagnostic in diagnostics) {
                        append(diagnostic.factoryName).append(": ")
                        diagnostic.textRanges.joinTo(this)
                        appendLine()
                    }
                } else {
                    appendLine("No diagnostics found")
                }
            }

            testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
        }
    }
}