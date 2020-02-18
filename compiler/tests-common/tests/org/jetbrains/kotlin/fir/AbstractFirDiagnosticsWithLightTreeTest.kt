/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirDiagnosticsWithLightTreeTest : AbstractFirDiagnosticsTest(), FirDiagnosticsTestLightTreeHelper {
    override fun doTest(filePath: String) {
        val file = createTestFileFromPath(filePath)
        val expectedText = KotlinTestUtils.doLoadFile(file)
        if (InTextDirectivesUtils.isDirectiveDefined(expectedText, "// IGNORE_LIGHT_TREE")) return

        super.doTest(filePath)
    }

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        super<FirDiagnosticsTestLightTreeHelper>.analyzeAndCheck(testDataFile, files)
    }
}