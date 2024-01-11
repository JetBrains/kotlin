/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd
import java.io.File

abstract class AbstractRawFirBuilderLazyBodiesTestCase : AbstractRawFirBuilderTestCase() {
    override fun doRawFirTest(filePath: String) {
        val file = createKtFile(filePath)
        val firFile = file.toFirFile(BodyBuildingMode.LAZY_BODIES)
        val firFileDump = FirRenderer().renderElementAsString(firFile)
        val originalExpectedFile = File(expectedPath(filePath, ".lazyBodies.txt"))
        val alternativeExpectedFile = expectedAlternativeFileIfExists(filePath)
        KotlinTestUtils.assertEqualsToFile(alternativeExpectedFile ?: originalExpectedFile, firFileDump)

        val alternativeFileContent = alternativeExpectedFile?.readText()?.trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd()
            ?: return

        val originalFileContent = originalExpectedFile.readText().trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd()
        if (alternativeFileContent == originalFileContent) {
            error("'${alternativeExpectedFile.name}' has the same content as '${originalExpectedFile.name}'")
        }
    }

    protected fun expectedAlternativeFileIfExists(filePath: String): File? {
        val alternativeFilePath = alternativeTestPrefix?.let {
            expectedPath(filePath, ".$it.lazyBodies.txt")
        } ?: return null

        return File(alternativeFilePath).takeIf(File::exists)
    }

    protected open val alternativeTestPrefix: String? get() = null
}
