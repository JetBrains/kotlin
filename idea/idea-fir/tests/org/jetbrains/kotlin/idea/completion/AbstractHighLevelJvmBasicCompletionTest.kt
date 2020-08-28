/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import org.jetbrains.kotlin.idea.completion.test.AbstractJvmBasicCompletionTest
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractHighLevelJvmBasicCompletionTest : AbstractJvmBasicCompletionTest() {
    override val captureExceptions: Boolean = false

    override fun executeTest(test: () -> Unit) {
        val doComparison = InTextDirectivesUtils.isDirectiveDefined(myFixture.file.text, "FIR_COMPARISON")
        try {
            test()
        } catch (e: Throwable) {
            if (doComparison) throw e
            return
        }
        if (!doComparison) {
            throw AssertionError("Looks like test is passing, please add // FIR_COMPARISON at the beginning of the file")
        }
    }
}