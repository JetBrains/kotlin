/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.nio.file.Paths
import java.util.function.Consumer
import kotlin.io.path.readText

class AnalysisApiSurfaceTest {
    private companion object {
        private val API_SURFACE_PATH = Paths.get("/Users/ilya.goncharov/repos/kotlin-amper/analysis/analysis-api/api/analysis-api.api")
    }

    @Test
    fun testNestedClassCoverage() = runTest {
        val text = API_SURFACE_PATH.readText()
        val classNames = Regex("class ([\\w/$]+)\\s*[{:]")
            .findAll(text)
            .map { it.groupValues[1] }
            .toSet()

        for (className in classNames) {
            val containingClassNames = generateSequence(className) {
                it.substringBeforeLast('$', missingDelimiterValue = "").takeUnless { it.isEmpty() }
            }

            for (containingClassName in containingClassNames) {
                if (containingClassName !in classNames) {
                    accept("Containing class $containingClassName for $className is missing")
                }
            }
        }
    }

    private fun runTest(block: Consumer<String>.() -> Unit) {
        val result = mutableListOf<String>()
        block(result::add)
        if (result.isNotEmpty()) {
            fail("Problems are found:\n" + result.joinToString("\n"))
        }
    }
}