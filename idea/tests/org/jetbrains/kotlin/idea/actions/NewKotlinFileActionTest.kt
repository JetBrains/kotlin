/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.Assert
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class NewKotlinFileActionTest: LightCodeInsightFixtureTestCase() {
    companion object {
        private val EMPTY_PARTS_ERROR = "Name can't have empty parts"
        private val EMPTY_ERROR = "Name can't be empty"
    }

    fun testEmptyName() {
        validateName("", EMPTY_ERROR)
    }

    fun testSpaces() {
        validateName("    ", EMPTY_ERROR)
    }

    fun testEmptyEnd() {
        validateName("Foo/", EMPTY_PARTS_ERROR)
    }

    fun testEmptyPartInQualified() {
        validateName("a..b", EMPTY_PARTS_ERROR)
    }

    fun testFileWithKt() {
        validateName("test.kt", null)
    }

    fun testFileWithUnixPathKt() {
        validateName("a/b/test.kt", null)
    }

    fun testFileWithWinPathKt() {
        validateName("a\\b\\test.kt", null)
    }

    fun testSimpleFile() {
        validateName("some", null)
    }

    fun testSimpleFileWithPath() {
        validateName("a/bb\\some", null)
    }

    private fun validateName(name: String, errorMessage: String?) {
        val actualError = NewKotlinFileAction.nameValidator.getErrorText(name)
        Assert.assertEquals("Invalid error message", errorMessage, actualError)
    }
}