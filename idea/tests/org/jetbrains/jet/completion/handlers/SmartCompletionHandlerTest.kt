/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.completion.handlers

import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.jet.plugin.formatter.JetCodeStyleSettings
import com.intellij.codeInsight.lookup.LookupElement
import java.io.File
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.junit.Assert
import com.intellij.openapi.application.Result
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import kotlin.properties.Delegates

public class SmartCompletionHandlerTest() : CompletionHandlerTestBase() {
    override val completionType: CompletionType = CompletionType.SMART
    override val testDataRelativePath: String = "/completion/handlers/smart"

    fun testConstructor() = doTest()
    fun testConstructorWithParameters() = doTest()
    fun testConstructorForNullable() = doTest()
    fun testConstructorForJavaClass() = doTest()
    //fun testConstructorInsertsImport() = doTest() //TODO
    fun testJavaStaticMethod() = doTest(1, "Thread.currentThread", null, '\n')
    fun testClassObjectMethod1() = doTest(1, "K.bar", null, '\n')
    fun testClassObjectMethod2() = doTest(1, "K.bar", null, '\n')
    //fun testJavaStaticFieldInsertImport() = doTest(1, "Locale.ENGLISH", null, '\n') //TODO
    fun testTabReplaceIdentifier() = doTest(1, "ss", null, '\t')
    fun testTabReplaceExpression() = doTest(1, "sss", null, '\t')
    fun testTabReplaceExpression2() = doTest(1, "sss", null, '\t')
    fun testTabReplaceExpression3() = doTest(1, "sss", null, '\t')
    fun testTabReplaceOperand() = doTest(1, "b3", null, '\t')
}
