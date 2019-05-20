/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.copyright

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import junit.framework.AssertionFailedError
import org.jetbrains.kotlin.idea.copyright.UpdateKotlinCopyright
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert
import java.io.File

abstract class AbstractUpdateKotlinCopyrightTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest(path: String) {
        myFixture.configureByFile(path)

        val text = myFixture.file.text.trim()
        val expectedNumberOfComments = InTextDirectivesUtils.getPrefixedInt(text, "// COMMENTS: ") ?: run {
            if (!text.isEmpty()) {
                throw AssertionFailedError("Every test should assert number of comments with `COMMENTS` directive")
            } else {
                0
            }
        }

        var commentsNumber = 0
        val comments = collectComments(UpdateKotlinCopyright.getCommentSearchRange(myFixture.file))
        for (comment in comments) {
            when (comment.text) {
                "/* PRESENT */" -> {
                    ++commentsNumber
                }
                "/* ABSENT */" -> {
                    throw AssertionFailedError("Unexpected comment found")
                }
                else -> {
                    throw AssertionFailedError("A comment with bad directive found: `$text`")
                }
            }
        }

        Assert.assertEquals("Wrong number of comments found", expectedNumberOfComments, commentsNumber)
    }

    override fun getTestDataPath() = File(PluginTestCaseBase.getTestDataPathBase(), "/copyright").path + File.separator

    companion object {
        private fun collectComments(range: UpdateKotlinCopyright.PsiRange): List<PsiComment> {
            val comments = mutableListOf<PsiComment>()
            val first = range.first
            val last = range.last
            if (first == null) return listOf()
            collectComments(first, last, comments)
            return comments
        }

        // Copied from UpdatePsiFileCopyright.collectComments()
        private fun collectComments(first: PsiElement, last: PsiElement?, comments: MutableList<PsiComment>) {
            if (first === last && first is PsiComment) {
                comments.add(first)
                return
            }
            var elem: PsiElement? = first
            while (elem !== last && elem != null) {
                if (elem is PsiComment) {
                    comments.add(elem)
                }

                elem = elem.nextSibling
            }
        }
    }
}