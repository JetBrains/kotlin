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
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.caches.resolve.LibraryModificationTracker
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.resolve.TargetPlatform
import java.io.File

abstract class KotlinFixtureCompletionBaseTestCase : KotlinLightCodeInsightFixtureTestCase() {
    abstract fun getPlatform(): TargetPlatform

    protected open fun complete(completionType: CompletionType, invocationCount: Int): Array<LookupElement>?
            = myFixture.complete(completionType, invocationCount)

    protected abstract fun defaultCompletionType(): CompletionType
    protected open fun defaultInvocationCount(): Int = 0

    open fun doTest(testPath: String) {
        setUpFixture(testPath)

        try {
            val fileText = FileUtil.loadFile(File(testPath), true)
            assertTrue("\"<caret>\" is missing in file \"$testPath\"", fileText.contains("<caret>"));

            if (ExpectedCompletionUtils.shouldRunHighlightingBeforeCompletion(fileText)) {
                myFixture.doHighlighting()
            }

            testCompletion(fileText, getPlatform(), { completionType, count -> complete(completionType, count) }, defaultCompletionType(), defaultInvocationCount())
        }
        finally {
            tearDownFixture()
        }
    }

    protected open fun setUpFixture(testPath: String) {
        //TODO: this is a hacky workaround for js second completion tests failing with PsiInvalidElementAccessException
        LibraryModificationTracker.getInstance(project).incModificationCount()

        myFixture.configureByFile(testPath)
    }

    protected open fun tearDownFixture() {

    }
}
