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
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.test.util.configureWithExtraFile
import org.junit.Assert

public abstract class AbstractCompletionWeigherTest(val completionType: CompletionType) : JetLightCodeInsightFixtureTestCase() {
    fun doTest(path: String) {
        myFixture.configureWithExtraFile(path, ".Data", ".Data1", ".Data2", ".Data3")

        val text = myFixture.getEditor().getDocument().getText()

        val items = InTextDirectivesUtils.findArrayWithPrefixes(text, "// ORDER:")
        Assert.assertTrue("""Some items should be defined with "// ORDER:" directive""", !items.isEmpty())

        myFixture.complete(completionType, InTextDirectivesUtils.getPrefixedInt(text, "// INVOCATION_COUNT:") ?: 1)
        myFixture.assertPreferredCompletionItems(InTextDirectivesUtils.getPrefixedInt(text, "// SELECTED:") ?: 0, *items)
    }

    override fun getTestDataPath() = JetTestUtils.getHomeDirectory()
}

public abstract class AbstractBasicCompletionWeigherTest() : AbstractCompletionWeigherTest(CompletionType.BASIC) {
    override fun getProjectDescriptor() = JetLightProjectDescriptor.INSTANCE
}

public abstract class AbstractSmartCompletionWeigherTest() : AbstractCompletionWeigherTest(CompletionType.SMART) {
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
