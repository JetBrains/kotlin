/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.test.frontend.fir.handlers.AbstractFirIdenticalChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.utils.isLLFirTestData
import java.io.File

/**
 * `.ll.kt` test data should not be identical to its base `.fir.kt`/`.kt` test data. If a base `.fir.kt` file does not exist, the base file
 * is the `.kt` file.
 *
 * As the `LL_FIR_DIVERGENCE` directive only exists in `.ll.kt` files, [LLFirIdenticalChecker] ignores this directive when comparing the
 * LL FIR file's content to the base file's content.
 */
class LLFirIdenticalChecker(testServices: TestServices) : AbstractFirIdenticalChecker(testServices) {
    override fun checkTestDataFile(testDataFile: File) {
        if (!testDataFile.isLLFirTestData) return

        val originalFile = helper.getClassicFileToCompare(testDataFile)
        val baseFile = helper.getFirFileToCompare(originalFile).takeIf { it.exists() } ?: originalFile

        // `readContentIgnoringLlFirDivergenceDirective` trims whitespace after the `LL_FIR_DIVERGENCE` directive to allow blank lines
        // after the directive. Hence, the base content's starting whitespace needs to be trimmed as well, otherwise file contents might
        // differ in their starting whitespace.
        val baseContent = helper.readContent(baseFile, trimLines = true).trimStart()
        val llContent = helper.readContent(testDataFile, trimLines = false).removeLlFirDivergenceDirective(trimLines = true)
        if (baseContent == llContent) {
            testServices.assertions.fail {
                "`${testDataFile.name}` and `${baseFile.name}` are identical. Remove `$testDataFile`."
            }
        } else {
            assertPreprocessedTestDataAreEqual(testServices, baseFile, baseContent, testDataFile, llContent) {
                "When ignoring diagnostics, the contents of `${baseFile.name}` (expected) and `${testDataFile.name}` (actual) are not" +
                        " identical. `.ll.kt` test data may only differ from its base `.fir.kt` or `.kt` test data in the reported" +
                        " diagnostics and the `LL_FIR_DIVERGENCE` directive. Update one of these test data files."
            }
        }
    }
}