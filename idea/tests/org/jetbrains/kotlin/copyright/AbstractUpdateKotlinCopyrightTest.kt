/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.copyright

import junit.framework.AssertionFailedError
import org.jetbrains.kotlin.idea.copyright.UpdateKotlinCopyright
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert
import java.io.File

abstract class AbstractUpdateKotlinCopyrightTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest(path: String) {
        myFixture.configureByFile(fileName())

        val fileText = myFixture.file.text.trim()
        val expectedNumberOfComments = InTextDirectivesUtils.getPrefixedInt(fileText, "// COMMENTS: ") ?: run {
            if (fileText.isNotEmpty()) {
                throw AssertionFailedError("Every test should assert number of comments with `COMMENTS` directive")
            } else {
                0
            }
        }

        val comments = UpdateKotlinCopyright.getExistentComments(myFixture.file)
        for (comment in comments) {
            val commentText = comment.text
            when {
                commentText.contains("PRESENT") -> {
                }
                commentText.contains("ABSENT") -> {
                    throw AssertionFailedError("Unexpected comment found: `$commentText`")
                }
                else -> {
                    throw AssertionFailedError("A comment with bad directive found: `$commentText`")
                }
            }
        }

        Assert.assertEquals(
            "Wrong number of comments found:\n${comments.joinToString(separator = "\n") { it.text }}\n",
            expectedNumberOfComments, comments.size
        )
    }

    override fun getTestDataPath() = File(PluginTestCaseBase.getTestDataPathBase(), "/copyright").path + File.separator
}