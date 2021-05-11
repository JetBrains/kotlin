/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import org.jetbrains.kotlin.idea.completion.test.AbstractKeywordCompletionTest
import org.jetbrains.kotlin.test.utils.IgnoreTests

abstract class AbstractFirKeywordCompletionTest : AbstractKeywordCompletionTest() {
    override val captureExceptions: Boolean = false

    override fun executeTest(test: () -> Unit) {
        IgnoreTests.runTestIfEnabledByFileDirective(testDataFile().toPath(), IgnoreTests.DIRECTIVES.FIR_COMPARISON, ".after") {
            super.executeTest(test)
        }
    }
}