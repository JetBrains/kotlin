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

package org.jetbrains.kotlin.completion

import org.jetbrains.kotlin.completion.util.*
import org.jetbrains.jet.plugin.KotlinCompletionTestCase
import org.jetbrains.jet.plugin.PluginTestCaseBase
import org.jetbrains.jet.plugin.project.TargetPlatform
import org.jetbrains.jet.plugin.stubs.AstAccessControl
import com.intellij.codeInsight.completion.CompletionType

public abstract class AbstractMultiFileSmartCompletionTest : KotlinCompletionTestCase() {
    override fun setUp() {
        super.setUp()
        setType(CompletionType.SMART)
    }

    protected fun doTest(testPath: String) {
        configureByFile(getTestName(false) + ".kt", "")
        AstAccessControl.testWithControlledAccessToAst(false, getFile().getVirtualFile(), getProject(), getTestRootDisposable(), {
            testCompletion(getFile().getText(), TargetPlatform.JVM, { invocationCount ->
                complete(invocationCount)
                myItems
            }, 1)
        })
    }

    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/completion/smartMultiFile/" + getTestName(false) + "/"
    }
}
