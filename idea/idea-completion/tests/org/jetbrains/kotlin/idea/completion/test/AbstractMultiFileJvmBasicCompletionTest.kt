/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.idea.test.AstAccessControl
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms

abstract class AbstractMultiFileJvmBasicCompletionTest : KotlinCompletionTestCase() {
    protected fun doTest(testPath: String) {
        configureByFile(getTestName(false) + ".kt", "")
        val shouldFail = testPath.contains("NoSpecifiedType")
        AstAccessControl.testWithControlledAccessToAst(shouldFail, getFile().getVirtualFile(), getProject(), getTestRootDisposable(), {
            testCompletion(file.text, DefaultBuiltInPlatforms.jvmPlatform, { completionType, invocationCount ->
                setType(completionType)
                complete(invocationCount)
                myItems
            }, CompletionType.BASIC, 0)
        })
    }

    override fun getTestDataPath(): String {
        return COMPLETION_TEST_DATA_BASE_PATH + "/basic/multifile/" + getTestName(false) + "/"
    }
}