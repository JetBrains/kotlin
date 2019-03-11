/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.idea.test.AstAccessControl
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms

abstract class AbstractMultiFileSmartCompletionTest : KotlinCompletionTestCase() {
    override fun setUp() {
        super.setUp()
        setType(CompletionType.SMART)
    }

    protected fun doTest(testPath: String) {
        configureByFile(getTestName(false) + ".kt", "")
        AstAccessControl.testWithControlledAccessToAst(false, getFile().getVirtualFile(), getProject(), getTestRootDisposable(), {
            testCompletion(file.text, DefaultBuiltInPlatforms.jvmPlatform, { completionType, invocationCount ->
                setType(completionType)
                complete(invocationCount)
                myItems
            }, CompletionType.SMART, 1)
        })
    }

    override fun getTestDataPath(): String {
        return COMPLETION_TEST_DATA_BASE_PATH + "/smartMultiFile/" + getTestName(false) + "/"
    }
}