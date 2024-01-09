/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.importOptimizer

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiImportOptimizerTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val importsAnalysis = analyseForTest(mainFile) {
            analyseImports(mainFile)
        }

        val importAnalysisRendered = buildString {
            val sortedUsedDeclarations = importsAnalysis.usedDeclarations
                .toSortedMap(compareBy { importPath -> importPath.toString() })
                .mapValues { (_, importedNames) -> importedNames.sorted() }

            appendLine("USED DECLARATIONS:")
            for ((path, elements) in sortedUsedDeclarations) {
                appendLine()
                appendLine("Declaration: $path")
                appendLine("By names: $elements")
            }

            appendLine()

            val sortedUnresolvedNames = importsAnalysis.unresolvedNames.sorted()

            appendLine("UNRESOLVED NAMES:")
            sortedUnresolvedNames.forEach(::appendLine)
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(importAnalysisRendered, extension = ".importsAnalysis")
    }
}