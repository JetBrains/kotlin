/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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