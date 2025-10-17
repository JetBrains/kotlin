/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.junit.jupiter.api.fail
import java.nio.file.Path
import java.util.function.Consumer
import kotlin.io.path.readText

abstract class AbstractSurfaceDumpConsistencyTest {
    private companion object {
        private val CLASS_NAME_REGEX = Regex("class ([\\w/$]+)\\s*[{:]")
    }

    protected fun validateApiDump(apiDumpPath: Path): Unit = runTest {
        val text = apiDumpPath.readText()
        val classNames = CLASS_NAME_REGEX
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
