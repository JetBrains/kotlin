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

package org.jetbrains.kotlin.completion.weighers

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File
import org.jetbrains.kotlin.idea.PluginTestCaseBase
import org.testng.Assert
import org.jetbrains.kotlin.test.util.configureWithExtraFile
import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.JetWithJdkAndRuntimeLightProjectDescriptor

public abstract class AbstractCompletionWeigherTest(val completionType: CompletionType, val relativeTestDataPath: String) : JetLightCodeInsightFixtureTestCase() {
    fun doTest(path: String) {
        myFixture.configureWithExtraFile(path, ".Data", ".Data1", ".Data2", ".Data3")

        val text = myFixture.getEditor().getDocument().getText()

        val items = InTextDirectivesUtils.findArrayWithPrefixes(text, "// ORDER:")
        Assert.assertTrue(!items.isEmpty(), """Some items should be defined with "// ORDER:" directive""")

        myFixture.complete(completionType, InTextDirectivesUtils.getPrefixedInt(text, "// INVOCATION_COUNT:") ?: 1)
        myFixture.assertPreferredCompletionItems(InTextDirectivesUtils.getPrefixedInt(text, "// SELECTED:") ?: 0, *items)
    }

    protected override fun getTestDataPath() : String? {
        return File(PluginTestCaseBase.getTestDataPathBase(), relativeTestDataPath).getPath() + File.separator
    }
}

public abstract class AbstractBasicCompletionWeigherTest() : AbstractCompletionWeigherTest(CompletionType.BASIC, "/completion/weighers/basic")

public abstract class AbstractSmartCompletionWeigherTest() : AbstractCompletionWeigherTest(CompletionType.SMART, "/completion/weighers/smart") {
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
