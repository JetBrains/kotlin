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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.liveTemplates.KotlinTemplateContextType
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

public class LiveTemplatesContextTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String =
            File(PluginTestCaseBase.getTestDataPathBase(), "/templates/context").getPath() + File.separator

    public fun testInDocComment() {
        myFixture.configureByFile(getTestName(false) + ".kt")
        assertInContexts(
                KotlinTemplateContextType.Generic::class.java,
                KotlinTemplateContextType.Comment::class.java)
    }

    public fun testTopLevel() {
        myFixture.configureByFile(getTestName(false) + ".kt")
        assertInContexts(
                KotlinTemplateContextType.Generic::class.java,
                KotlinTemplateContextType.TopLevel::class.java)
    }

    public fun testInExpression() {
        myFixture.configureByFile(getTestName(false) + ".kt")
        assertInContexts(
                KotlinTemplateContextType.Generic::class.java,
                KotlinTemplateContextType.Expression::class.java)
    }

    private fun assertInContexts(vararg expectedContexts: Class<out KotlinTemplateContextType>) {
        val allContexts = TemplateContextType.EP_NAME.getExtensions().filter { it is KotlinTemplateContextType }
        val enabledContexts = allContexts.filter { it.isInContext(myFixture.getFile(), myFixture.getCaretOffset()) }.map { it.javaClass }
        UsefulTestCase.assertSameElements(enabledContexts, *expectedContexts)
    }
}
