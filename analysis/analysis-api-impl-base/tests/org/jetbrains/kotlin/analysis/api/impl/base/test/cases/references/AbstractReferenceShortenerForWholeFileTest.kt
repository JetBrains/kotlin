/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.components.ShortenOption
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.ShorteningResultsRenderer.renderShorteningResults
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedSingleModuleTest
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractReferenceShortenerForWholeFileTest : AbstractAnalysisApiBasedSingleModuleTest() {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val file = ktFiles.first()

        val shortenings = executeOnPooledThreadInReadAction {
            analyseForTest(file) {
                ShortenOption.values().map { option ->
                    val shorteningsForOption = collectPossibleReferenceShortenings(file, file.textRange, { option }, { option })

                    Pair(option.name, shorteningsForOption)
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