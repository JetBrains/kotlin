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

package org.jetbrains.kotlin.completion.handlers

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.JetLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

public abstract class AbstractCompletionHandlerTest(private val defaultCompletionType: CompletionType) : CompletionHandlerTestBase() {
    private val INVOCATION_COUNT_PREFIX = "INVOCATION_COUNT:"
    private val LOOKUP_STRING_PREFIX = "ELEMENT:"
    private val ELEMENT_TEXT_PREFIX = "ELEMENT_TEXT:"
    private val TAIL_TEXT_PREFIX = "TAIL_TEXT:"
    private val COMPLETION_CHAR_PREFIX = "CHAR:"
    private val COMPLETION_TYPE_PREFIX = "COMPLETION_TYPE:"

    protected open fun doTest(testPath: String) {
        setUpFixture(testPath)

        val fileText = FileUtil.loadFile(File(testPath))
        val invocationCount = InTextDirectivesUtils.getPrefixedInt(fileText, INVOCATION_COUNT_PREFIX) ?: 1
        val lookupString = InTextDirectivesUtils.findStringWithPrefixes(fileText, LOOKUP_STRING_PREFIX)
        val itemText = InTextDirectivesUtils.findStringWithPrefixes(fileText, ELEMENT_TEXT_PREFIX)
        val tailText = InTextDirectivesUtils.findStringWithPrefixes(fileText, TAIL_TEXT_PREFIX)

        val completionCharString = InTextDirectivesUtils.findStringWithPrefixes(fileText, COMPLETION_CHAR_PREFIX)
        val completionChar = when(completionCharString) {
            "\\n", null -> '\n'
            "\\t" -> '\t'
            else -> completionCharString.singleOrNull() ?: error("Incorrect completion char: \"$completionCharString\"")
        }

        val completionTypeString = InTextDirectivesUtils.findStringWithPrefixes(fileText, COMPLETION_TYPE_PREFIX)
        val completionType = when (completionTypeString) {
            "BASIC" -> CompletionType.BASIC
            "SMART" -> CompletionType.SMART
            null -> defaultCompletionType
            else -> error("Unknown completion type: $completionTypeString")
        }

        doTestWithTextLoaded(completionType, invocationCount, lookupString, itemText, tailText, completionChar, testPath + ".after")
    }

    protected open fun setUpFixture(testPath: String) {
        fixture.configureByFile(testPath)
    }

    override fun getProjectDescriptor() = JetLightProjectDescriptor.INSTANCE
}

public abstract class AbstractBasicCompletionHandlerTest() : AbstractCompletionHandlerTest(CompletionType.BASIC)

public abstract class AbstractSmartCompletionHandlerTest() : AbstractCompletionHandlerTest(CompletionType.SMART)

public abstract class AbstractCompletionCharFilterTest() : AbstractCompletionHandlerTest(CompletionType.BASIC)

public abstract class AbstractKeywordCompletionHandlerTest() : AbstractCompletionHandlerTest(CompletionType.BASIC)
