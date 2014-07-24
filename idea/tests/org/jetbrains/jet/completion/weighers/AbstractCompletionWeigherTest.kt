/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.completion.weighers

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.jet.InTextDirectivesUtils
import java.io.File
import org.jetbrains.jet.plugin.PluginTestCaseBase
import org.testng.Assert
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jet.test.util.configureWithExtraFile
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase

public abstract class AbstractCompletionWeigherTest() : JetLightCodeInsightFixtureTestCase() {
    fun doTest(path: String) {
        myFixture.configureWithExtraFile(path)

        val text = myFixture.getEditor().getDocument().getText()

        val items = InTextDirectivesUtils.findArrayWithPrefixes(text, "// ORDER:")
        Assert.assertTrue(!items.isEmpty(), """Some items should be defined with "// ORDER:" directive""")

        myFixture.complete(CompletionType.BASIC, InTextDirectivesUtils.getPrefixedInt(text, "// INVOCATION_COUNT:") ?: 1)
        myFixture.assertPreferredCompletionItems(InTextDirectivesUtils.getPrefixedInt(text, "// SELECTED:") ?: 0, *items)
    }

    protected override fun getTestDataPath() : String? {
        return File(PluginTestCaseBase.getTestDataPathBase(), "/completion/weighers").getPath() + File.separator
    }
}

