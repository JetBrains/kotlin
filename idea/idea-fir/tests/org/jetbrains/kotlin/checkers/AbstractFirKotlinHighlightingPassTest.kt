/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.idea.highlighter.AbstractHighlightingTest
import org.jetbrains.kotlin.idea.invalidateCaches
import org.jetbrains.kotlin.idea.test.withCustomCompilerOptions
import org.jetbrains.kotlin.idea.withPossiblyDisabledDuplicatedFirSourceElementsException
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.uitls.IgnoreTests
import org.jetbrains.kotlin.test.uitls.IgnoreTests.DIRECTIVES
import org.jetbrains.kotlin.test.uitls.IgnoreTests.cleanUpIdenticalFirTestFile
import org.jetbrains.kotlin.test.uitls.IgnoreTests.getFirTestFile
import java.io.File

abstract class AbstractFirKotlinHighlightingPassTest : AbstractKotlinHighlightingPassTest() {

    override val captureExceptions: Boolean = false

    override fun isFirPlugin(): Boolean = true

    override fun fileName(): String = getFirTestFile(originalTestFile()).name

    private fun originalTestFile(): File = testDataFile(super.fileName())

    override fun doTest(filePath: String) {
        IgnoreTests.runTestIfNotDisabledByFileDirective(originalTestFile().toPath(), DIRECTIVES.IGNORE_FIR) {
            myFixture.configureByFile(fileName())
            checkHighlighting(checkWarnings = false, checkInfos = false, checkWeakWarnings = false)
            cleanUpIdenticalFirTestFile(originalTestFile())
        }
    }

    override fun tearDown() {
        project.invalidateCaches(file as? KtFile)
        super.tearDown()
    }

    override fun checkHighlighting(
        checkWarnings: Boolean,
        checkInfos: Boolean,
        checkWeakWarnings: Boolean
    ): Long {
        val fileText = file.text
        val isDuplicatedHighlightingExpected =
            InTextDirectivesUtils.isDirectiveDefined(fileText, AbstractHighlightingTest.EXPECTED_DUPLICATED_HIGHLIGHTING_PREFIX)
        var result: Long? = null
        AbstractHighlightingTest.withExpectedDuplicatedHighlighting(isDuplicatedHighlightingExpected, /*isFirPlugin*/true) {
            withCustomCompilerOptions(fileText, project, module) {
                try {
                    withPossiblyDisabledDuplicatedFirSourceElementsException(fileText) {
                        result = myFixture.checkHighlighting(checkWarnings, checkInfos, checkWeakWarnings)
                    }
                } catch (e: FileComparisonFailure) {
                    throw FileComparisonFailure(e.message, e.expected, e.actual, File(e.filePath).absolutePath)
                }
            }
        }
        return result!!
    }
}
