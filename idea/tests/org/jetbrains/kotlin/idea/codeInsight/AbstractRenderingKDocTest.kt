/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.KotlinDocumentationProvider
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils


abstract class AbstractRenderingKDocTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(path: String) {
        myFixture.configureByFile(fileName())
        val file = myFixture.file

        val kDocProvider = KotlinDocumentationProvider()

        val comments = mutableListOf<String>()
        kDocProvider.collectDocComments(file) {
            val rendered = it.owner?.let { owner -> kDocProvider.generateRenderedDoc(owner) }
            if (rendered != null) {
                comments.add(rendered.replace("\n", ""))
            }
        }

        val expectedRenders = InTextDirectivesUtils.findLinesWithPrefixesRemoved(file.text, "// RENDER: ")
        UsefulTestCase.assertOrderedEquals(comments, expectedRenders)
    }
}
