/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightPlatformCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractBreadcrumbsTest : KotlinLightPlatformCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor? = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/breadcrumbs"

    protected open fun doTest(path: String) {
        assert(path.endsWith(".kt")) { path }
        myFixture.configureByFile(path)

        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        val provider = KotlinBreadcrumbsInfoProvider()
        val elements = generateSequence(element) { provider.getParent(it) }
                .filter { provider.acceptElement(it) }
                .toList()
                .asReversed()
        val crumbs = elements.joinToString(separator = "\n") { "  " + provider.getElementInfo(it) }
        val tooltips = elements.joinToString(separator = "\n") { "  " + provider.getElementTooltip(it) }
        val resultText = "Crumbs:\n$crumbs\nTooltips:\n$tooltips"
        KotlinTestUtils.assertEqualsToFile(File(testDataPath + "/" + File(path).nameWithoutExtension + ".txt"), resultText)
    }
}