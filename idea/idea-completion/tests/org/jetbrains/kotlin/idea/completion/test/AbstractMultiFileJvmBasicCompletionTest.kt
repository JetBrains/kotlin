/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.idea.test.AstAccessControl
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

public abstract class AbstractMultiFileJvmBasicCompletionTest : KotlinCompletionTestCase() {
    protected fun doTest(testPath: String) {
        configureByFile(getTestName(false) + ".kt", "")
        // several tests require disabling this check after adding InclusiveRange, need to investigate why
//        val shouldFail = testPath.contains("NoSpecifiedType")
//        AstAccessControl.testWithControlledAccessToAst(shouldFail, getFile().getVirtualFile(), getProject(), getTestRootDisposable(), {
            testCompletion(getFile().getText(), JvmPlatform, { completionType, invocationCount ->
                setType(completionType)
                complete(invocationCount)
                myItems
            }, CompletionType.BASIC, 0)
//        })
    }

    override fun getTestDataPath(): String {
        return COMPLETION_TEST_DATA_BASE_PATH + "/basic/multifile/" + getTestName(false) + "/"
    }
}