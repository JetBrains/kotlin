/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.components.ShortenStrategy
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.ShorteningResultsRenderer.renderShorteningResults
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractReferenceShortenerForWholeFileTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val shortenings = executeOnPooledThreadInReadAction {
            analyseForTest(mainFile) {
                buildMap {
                    this += "default settings" to collectPossibleReferenceShortenings(mainFile, mainFile.textRange)

                    this += ShortenStrategy.entries.associateWith { option ->
                        val shorteningsForOption = collectPossibleReferenceShortenings(
                            mainFile,
                            mainFile.textRange,
                            classShortenStrategy = { option },
                            callableShortenStrategy = { option }
                        )

                        shorteningsForOption
                    }
                }
            }
        }

        val actual = buildString {
            shortenings.forEach { (name, shortening) ->
                appendLine("with ${name}:")
                if (shortening.isEmpty) return@forEach
                renderShorteningResults(shortening)
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}