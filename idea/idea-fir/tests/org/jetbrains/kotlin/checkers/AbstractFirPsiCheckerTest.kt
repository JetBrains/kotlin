/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.idea.test.withCustomCompilerOptions
import org.jetbrains.kotlin.idea.withPossiblyDisabledDuplicatedFirSourceElementsException
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractFirPsiCheckerTest : AbstractPsiCheckerTest() {
    override val captureExceptions: Boolean = false

    override fun isFirPlugin(): Boolean = true

    override fun setUp() {
        super.setUp()
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
        val fileText = file.text
        return withCustomCompilerOptions(fileText, project, module) {
            val doComparison = InTextDirectivesUtils.isDirectiveDefined(myFixture.file.text, "FIR_COMPARISON")
            try {
                withPossiblyDisabledDuplicatedFirSourceElementsException(fileText) {
                    myFixture.checkHighlighting(checkWarnings, checkInfos, checkWeakWarnings)
                }
            } catch (e: FileComparisonFailure) {
                if (doComparison) {
                    // Even this is very partial check (only error compatibility, no warnings / infos)
                    throw FileComparisonFailure(e.message, e.expected, e.actual, File(e.filePath).absolutePath)
                } else {
                    // Here we just check that we haven't crashed due to exception
                    0
                }
            }
        }
    }

    override fun tearDown() {
        super.tearDown()
    }
}