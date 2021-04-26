/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.handlers

import org.jetbrains.kotlin.test.utils.IgnoreTests

abstract class AbstractHighLevelBasicCompletionHandlerTest : AbstractBasicCompletionHandlerTest() {
    override val captureExceptions: Boolean = false

    override fun doTest(testPath: String) {
        IgnoreTests.runTestIfEnabledByFileDirective(testDataFilePath(), IgnoreTests.DIRECTIVES.FIR_COMPARISON, ".after") {
            super.doTest(testPath)
        }
    }
}