/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.components.ShortenOptions
import org.jetbrains.kotlin.analysis.api.components.ShortenStrategy
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.ShorteningResultsRenderer.renderShorteningResults
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * A class for reference shortener test.
 *
 * Note that it tests shortening only a single expression between <expr> and </expr> in the first file.
 */
abstract class AbstractReferenceShortenerTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val element = testServices.expressionMarkerProvider.getSelectedElementOfType<KtElement>(mainFile)

        val shortenings = executeOnPooledThreadInReadAction {
            analyseForTest(element) {
                buildMap {
                    this += "default settings" to collectPossibleReferenceShorteningsInElement(element)

                    this += ShortenStrategy.entries.associate { option ->
                        val shorteningsForOption = collectPossibleReferenceShorteningsInElement(
                            element,
                            shortenOptions = ShortenOptions.ALL_ENABLED,
                            classShortenStrategy = { option },
                            callableShortenStrategy = { option }
                        )

                        option.toString() to shorteningsForOption
                    }
                }
            }
        }

        val actual = buildString {
            appendLine("Before shortening: ${element.text}")
            shortenings.forEach { (name, shortening) ->
                appendLine("with ${name}:")
                if (shortening.isEmpty) return@forEach
                renderShorteningResults(shortening)
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}