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
import org.jetbrains.kotlin.test.utils.isLLFirSpecializedTestData
import org.jetbrains.kotlin.test.utils.latestLVTestDataFile
import java.nio.file.Path
import kotlin.io.path.*

abstract class TestDataFileReplacer(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun transformTestDataPath(testDataFileName: String): String {
        val originalFile = Path(testDataFileName)

        // If the original file is already not just `.kt`, then it was processed by another replacer
        if (originalFile.isCustomTestData) return testDataFileName
        // configured with `forTestsMatching`, it'll be executed after `LLFirMetaTestConfigurator`, which is configured generally.
        if (originalFile.isLLFirSpecializedTestData) return testDataFileName

        if (!shouldReplaceFile(originalFile)) return testDataFileName

        val newFile = originalFile.newFile
        if (!newFile.exists()) {
            originalFile.copyTo(newFile)
        }
        return newFile.toAbsolutePath().pathString
    }

    protected abstract fun shouldReplaceFile(originalFile: Path): Boolean

    protected abstract val Path.newFile: Path
}

class FirOldFrontendMetaConfigurator(testServices: TestServices) : TestDataFileReplacer(testServices) {
    override fun shouldReplaceFile(originalFile: Path): Boolean {
        return originalFile.useLines { lines ->
            lines.none { it == "// ${FirDiagnosticsDirectives.FIR_IDENTICAL.name}" }
        }
    }

    override val Path.newFile: Path
        get() = this.firTestDataFile
}

class LatestLanguageVersionMetaConfigurator(testServices: TestServices) : TestDataFileReplacer(testServices) {
    override fun shouldReplaceFile(originalFile: Path): Boolean {
        return originalFile.useLines { lines ->
            lines.any { it == "// ${FirDiagnosticsDirectives.LATEST_LV_DIFFERENCE.name}" }
        }
    }

    override val Path.newFile: Path
        get() = this.latestLVTestDataFile
}
