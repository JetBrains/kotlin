/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.test.frontend.fir.handlers.AbstractAlternativeKtFileIdenticalChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.managedTestData
import org.jetbrains.kotlin.test.utils.isLLFirDivergenceTestData
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import java.io.File

/**
 * `.ll.kt` test data should not be identical to its base `.fir.kt`/`.kt` test data. If a base `.fir.kt` file does not exist, the base file
 * is the `.kt` file. The same holds for the Wasm variants of LL FIR test data (e.g., `.ll.wasmJs.kt`) of managed tests, whose base is the
 * test data they fall back to (e.g., `.ll.kt` or `.kt`).
 *
 * As the `LL_FIR_DIVERGENCE` directive only exists in LL FIR test data, [LLFirIdenticalChecker] ignores this directive when comparing the
 * LL FIR file's content to the base file's content. LL FIR test data should also differ from the base test data in more than
 * `METADATA` tags, which [LLKmpMetaInfoProcessor] takes from the base test data.
 *
 * `updateTestData` deletes such redundant LL FIR test data instead, in the test which writes it: the test data manager can't detect it,
 * as it doesn't know about the `LL_FIR_DIVERGENCE` directive.
 */
class LLFirIdenticalChecker(testServices: TestServices) : AbstractAlternativeKtFileIdenticalChecker(testServices) {
    override fun checkTestDataFile(testDataFile: File) {
        val managedTestData = testServices.managedTestData

        // A managed test checks the variant it writes, which `updateTestData` might have created from the actual output of this run, or
        // deleted as redundant.
        val llFile = managedTestData?.writeTargetFile ?: testDataFile
        if (!llFile.isLLFirDivergenceTestData || !llFile.exists()) return

        val baseFile = managedTestData?.fallbackFile ?: llFile.originalTestDataFile

        // `removeLlFirDivergenceDirective` trims whitespace after the `LL_FIR_DIVERGENCE` directive to allow blank lines after the
        // directive. Hence, the base content's starting whitespace needs to be trimmed as well, otherwise file contents might differ in their
        // starting whitespace. The base might be LL FIR test data itself, e.g., `.ll.kt` for `.ll.wasmJs.kt`.
        val baseContent = readContent(baseFile, trimLines = true).removeLlFirDivergenceDirective(trimLines = true)
        val llContent = readContent(llFile, trimLines = false).removeLlFirDivergenceDirective(trimLines = true)
        val redundancyMessage = when {
            baseContent == llContent -> "`${llFile.name}` and `${baseFile.name}` are identical. Remove `$llFile`."

            baseContent.withoutMetadataCompilationTags() == llContent ->
                "`${llFile.name}` only differs from `${baseFile.name}` in `METADATA` tags, which LL tests take from" +
                        " `${baseFile.name}` (see `LLKmpMetaInfoProcessor`). Remove `$llFile`."

            else -> null
        }

        if (redundancyMessage == null) {
            assertPreprocessedTestDataAreEqual(testServices, baseFile, baseContent, llFile, llContent) {
                "When ignoring diagnostics, the contents of `${baseFile.name}` (expected) and `${llFile.name}` (actual) are not" +
                        " identical. LL FIR test data may only differ from its base `.fir.kt` or `.kt` test data in the reported" +
                        " diagnostics and the `LL_FIR_DIVERGENCE` directive. Update one of these test data files."
            }
            return
        }

        if (managedTestData?.updatesTestData == true) {
            if (managedTestData.isWriteTarget(llFile)) {
                managedTestData.deleteWriteTargetFile()
            }
            return
        }

        testServices.assertions.fail { redundancyMessage }
    }
}
