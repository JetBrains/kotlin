/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.idea.completion.test.AbstractMultiFileJvmBasicCompletionTest
import org.jetbrains.kotlin.idea.completion.test.testCompletion
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.uitls.IgnoreTests
import java.nio.file.Path
import java.nio.file.Paths

abstract class AbstractHighLevelMultiFileJvmBasicCompletionTest : AbstractMultiFileJvmBasicCompletionTest() {
    private val testDataFile: Path
        get() = Paths.get(testDataPath, getTestName(false) + ".kt")

    override fun doTest(testPath: String) {
        IgnoreTests.runTestIfEnabledByFileDirective(testDataFile, IgnoreTests.DIRECTIVES.FIR_COMPARISON) {
            configureByFile(getTestName(false) + ".kt", "")

            testCompletion(file.text, JvmPlatforms.unspecifiedJvmPlatform, { completionType, invocationCount ->
                setType(completionType)
                complete(invocationCount)
                myItems
            }, CompletionType.BASIC, 0)
        }
    }
}