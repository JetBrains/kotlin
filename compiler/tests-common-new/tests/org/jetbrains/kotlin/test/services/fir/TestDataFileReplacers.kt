/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.fir

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.utils.firTestDataFile
import org.jetbrains.kotlin.test.utils.isCustomTestData
import org.jetbrains.kotlin.test.utils.latestLVTestDataFile
import java.io.File

abstract class TestDataFileReplacer(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun transformTestDataPath(testDataFileName: String): String {
        val originalFile = File(testDataFileName)

        // If the original file is already not just `.kt`, then it was processed by another replacer
        if (originalFile.isCustomTestData) return testDataFileName
        // configured with `forTestsMatching`, it'll be executed after `LLFirMetaTestConfigurator`, which is configured generally.
        if (".reversed." in originalFile.path) return testDataFileName

        if (!shouldReplaceFile(originalFile)) return testDataFileName

        val newFile = originalFile.newFile
        if (!newFile.exists()) {
            originalFile.copyTo(newFile)
        }
        return newFile.absolutePath
    }

    protected abstract fun shouldReplaceFile(originalFile: File): Boolean

    protected abstract val File.newFile: File
}

class FirOldFrontendMetaConfigurator(testServices: TestServices) : TestDataFileReplacer(testServices) {
    override fun shouldReplaceFile(originalFile: File): Boolean {
        return originalFile.useLines { lines ->
            lines.none { it == "// ${FirDiagnosticsDirectives.FIR_IDENTICAL.name}" }
        }
    }

    override val File.newFile: File
        get() = this.firTestDataFile
}

class LatestLanguageVersionMetaConfigurator(testServices: TestServices) : TestDataFileReplacer(testServices) {
    override fun shouldReplaceFile(originalFile: File): Boolean {
        return originalFile.useLines { lines ->
            lines.any { it == "// ${FirDiagnosticsDirectives.LATEST_LV_DIFFERENCE.name}" }
        }
    }

    override val File.newFile: File
        get() = this.latestLVTestDataFile
}
