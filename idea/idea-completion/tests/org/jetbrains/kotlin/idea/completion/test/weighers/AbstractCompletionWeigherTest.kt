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

package org.jetbrains.kotlin.idea.completion.test.weighers

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.idea.completion.test.RELATIVE_COMPLETION_TEST_DATA_BASE_PATH
import org.jetbrains.kotlin.idea.completion.test.configureWithExtraFile
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert

abstract class AbstractCompletionWeigherTest(val completionType: CompletionType, val relativeTestDataPath: String) : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest(path: String) {
        val pathPrefix = RELATIVE_COMPLETION_TEST_DATA_BASE_PATH + "/" + relativeTestDataPath
        assert(path.startsWith(pathPrefix))
        val relativePath = path.removePrefix(pathPrefix)

        myFixture.configureWithExtraFile(relativePath, ".Data", ".Data1", ".Data2", ".Data3", ".Data4", ".Data5", ".Data6")

        val text = myFixture.editor.document.text

        val items = InTextDirectivesUtils.findArrayWithPrefixes(text, "// ORDER:")
        Assert.assertTrue("""Some items should be defined with "// ORDER:" directive""", !items.isEmpty())

        myFixture.complete(completionType, InTextDirectivesUtils.getPrefixedInt(text, "// INVOCATION_COUNT:") ?: 1)
        myFixture.assertPreferredCompletionItems(InTextDirectivesUtils.getPrefixedInt(text, "// SELECTED:") ?: 0, *items)
    }
}

abstract class AbstractBasicCompletionWeigherTest() : AbstractCompletionWeigherTest(CompletionType.BASIC, "weighers/basic") {
    override fun getProjectDescriptor() = KotlinLightProjectDescriptor.INSTANCE
}

abstract class AbstractSmartCompletionWeigherTest() : AbstractCompletionWeigherTest(CompletionType.SMART, "weighers/smart") {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
