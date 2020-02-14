/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.idea.fir.FirResolution
import org.jetbrains.kotlin.idea.test.configureCompilerOptions
import org.jetbrains.kotlin.idea.test.rollbackCompilerOptions
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractFirPsiCheckerTest : AbstractPsiCheckerTest() {
    override fun setUp() {
        super.setUp()
        FirResolution.enabled = true
    }

    override fun doTest(filePath: String) {
        myFixture.configureByFile(fileName())
        checkHighlighting(checkWarnings = false, checkInfos = false, checkWeakWarnings = false)
    }

    override fun checkHighlighting(
        checkWarnings: Boolean,
        checkInfos: Boolean,
        checkWeakWarnings: Boolean
    ): Long {
        val file = file
        val configured = configureCompilerOptions(file.text, project, module)
        val doComparison = InTextDirectivesUtils.isDirectiveDefined(myFixture.file.text, "FIR_COMPARISON")
        return try {
            myFixture.checkHighlighting(checkWarnings, checkInfos, checkWeakWarnings)
        } catch (e: FileComparisonFailure) {
            if (doComparison) {
                // Even this is very partial check (only error compatibility, no warnings / infos)
                throw FileComparisonFailure(e.message, e.expected, e.actual, File(e.filePath).absolutePath)
            } else {
                // Here we just check that we haven't crashed due to exception
                0L
            }
        } finally {
            if (configured) {
                rollbackCompilerOptions(project, module)
            }
        }
    }

    override fun tearDown() {
        FirResolution.enabled = false
        super.tearDown()
    }
}