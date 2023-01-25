/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.components.ShortenOption
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedSingleModuleTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * A class for reference shortener test.
 *
 * Note that it tests shortening only a single expression between <expr> and </expr> in the first file.
 */
abstract class AbstractReferenceShortenerTest : AbstractAnalysisApiBasedSingleModuleTest() {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val element = testServices.expressionMarkerProvider.getSelectedElement(ktFiles.first())

        val shortenings = executeOnPooledThreadInReadAction {
            analyseForTest(element) {
                ShortenOption.values().map { option ->
                    Pair(option.name, collectPossibleReferenceShorteningsInElement(element, { option }, { option }))
                }
            }
        }

        val actual = buildString {
            appendLine("Before shortening: ${element.text}")
            shortenings.forEach { (name, shortening) ->
                appendLine("with ${name}:")
                if (shortening.isEmpty) return@forEach
                shortening.getTypesToShorten().forEach { userType ->
                    userType.element?.text?.let {
                        appendLine("[type] $it")
                    }
                }
                shortening.getQualifiersToShorten().forEach { qualifier ->
                    qualifier.element?.text?.let {
                        appendLine("[qualifier] $it")
                    }
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}