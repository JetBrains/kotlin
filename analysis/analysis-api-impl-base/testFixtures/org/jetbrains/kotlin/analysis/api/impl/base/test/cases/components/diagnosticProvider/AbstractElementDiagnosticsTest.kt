/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticCheckerKind
import org.jetbrains.kotlin.analysis.api.diagnostics.directDiagnostics
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/** @see AbstractCollectDiagnosticsTest */
abstract class AbstractElementDiagnosticsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val targetDeclaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeByDirective(
            mainFile,
            mainModule.testModule,
            defaultType = KtElement::class,
        ) as KtElement

        analyzeForTest(mainFile) {
            val diagnostics = targetDeclaration.directDiagnostics()
                .withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED)
                .toList()

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

            testServices.assertions.assertEqualsToTestOutputFile(actualText)
        }
    }
}
