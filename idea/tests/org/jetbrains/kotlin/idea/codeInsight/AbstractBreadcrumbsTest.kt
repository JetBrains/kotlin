/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightPlatformCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractBreadcrumbsTest : KotlinLightPlatformCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor? = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected open fun doTest(unused: String) {
        val fileName = fileName()
        assert(fileName.endsWith(".kt")) { fileName }
        myFixture.configureByFile(fileName)

        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        val provider = KotlinBreadcrumbsInfoProvider()
        val elements = generateSequence(element) { provider.getParent(it) }
            .filter { provider.acceptElement(it) }
            .toList()
            .asReversed()
        val crumbs = elements.joinToString(separator = "\n") { "  " + provider.getElementInfo(it) }
        val tooltips = elements.joinToString(separator = "\n") { "  " + provider.getElementTooltip(it) }
        val resultText = "Crumbs:\n$crumbs\nTooltips:\n$tooltips"
        KotlinTestUtils.assertEqualsToFile(testDataFile(File(fileName).nameWithoutExtension + ".txt"), resultText)
    }
}