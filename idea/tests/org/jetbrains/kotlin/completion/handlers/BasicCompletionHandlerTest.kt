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
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.idea.PluginTestCaseBase
import org.jetbrains.kotlin.idea.formatter.JetCodeStyleSettings
import java.io.File

deprecated("All tests from here to be moved to the generated test")
public class BasicCompletionHandlerTest : CompletionHandlerTestBase(){
    private fun checkResult(){
        fixture.checkResultByFile(getTestName(false) + ".kt.after")
    }

    override fun getTestDataPath() = File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers").getPath() + File.separator

    private fun doTest() {
        doTest(2, "*", null, null, '\n')
    }

    private fun doTest(time: Int, lookupString: String?, tailText: String?, completionChar: Char) {
        doTest(time, lookupString, null, tailText, completionChar)
    }

    private fun doTest(time: Int, lookupString: String?, itemText: String?, tailText: String?, completionChar: Char) {
        fixture.configureByFile(fileName())
        doTestWithTextLoaded(CompletionType.BASIC, time, lookupString, itemText, tailText, completionChar, fileName() + ".after")
    }

    fun testClassCompletionImport() = doTest(2, "SortedSet", null, '\n')

    fun testClassCompletionInMiddle() = doTest(1, "TimeZone", " (java.util)", '\t')

    fun testClassCompletionInImport() = doTest(1, "TimeZone", " (java.util)", '\t')

    fun testClassCompletionInLambda() = doTest(1, "String", " (kotlin)", '\n')

    fun testClassCompletionBeforeName() = doTest(1, "StringBuilder", " (java.lang)", '\n')

    fun testDoNotInsertImportForAlreadyImported() = doTest()

    fun testDoNotInsertDefaultJsImports() = doTest()

    fun testDoNotInsertImportIfResolvedIntoJavaConstructor() = doTest()

    fun testNonStandardArray() = doTest(2, "Array", "java.lang.reflect", '\n')

    fun testNoParamsFunction() = doTest()

    fun testParamsFunction() = doTest()

    fun testNamedParametersCompletion() = doTest()

    fun testNamedParametersCompletionOnEqual() = doTest(0, "paramTest", "paramTest =", null, '=')

    fun testNamedParameterKeywordName() = doTest(1, "class", "class =", null, '\n')

    fun testInsertJavaClassImport() = doTest()

    fun testInsertVoidJavaMethod() = doTest()

    fun testPropertiesSetter() = doTest()

    fun testExistingSingleBrackets() = doTest()

    fun testExtFunction() = doTest()

    fun testFunctionLiteralInsertOnSpace() = doTest(2, null, null, ' ')

    fun testInsertImportOnTab() = doTest(2, "ArrayList", null, '\t')

    fun testHigherOrderFunction() = doTest()

    fun testInsertFqnForJavaClass() = doTest(2, "SortedSet", "java.util", '\n')

    fun testHigherOrderFunctionWithArg() = doTest(2, "filterNot", null, '\n')

    fun testHigherOrderFunctionWithArgs1() = doTest(1, "foo", "foo", " { (String, Char) -> ... }", '\n')

    fun testHigherOrderFunctionWithArgs2() = doTest(1, "foo", "foo", "(p: (String, Char) -> Boolean)", '\n')

    fun testHigherOrderFunctionWithArgs3() = doTest(1, "foo", "foo", " { (String, Char) -> ... }", '\n')

    fun testForceParenthesisForTabChar() = doTest(0, "some", null, '\t')

    fun testTabInsertAtTheFileEnd() = doTest(0, "vvvvv", null, '\t')

    fun testTabInsertBeforeBraces() = doTest(0, "vvvvv", null, '\t')

    fun testTabInsertBeforeBrackets() = doTest(0, "vvvvv", null, '\t')

    fun testTabInsertBeforeOperator() = doTest(0, "vvvvv", null, '\t')

    fun testTabInsertBeforeParentheses() = doTest(0, "vvvvv", null, '\t')

    fun testTabInsertInsideBraces() = doTest(1, "vvvvv", null, '\t')

    fun testTabInsertInsideBrackets() = doTest(0, "vvvvv", null, '\t')

    fun testTabInsertInsideEmptyParentheses() = doTest(0, "vvvvv", null, '\t')

    fun testTabInsertInsideParentheses() = doTest(0, "vvvvv", null, '\t')

    fun testTabInsertInSimpleName() = doTest(0, "vvvvv", null, '\t')

    fun testInsertFunctionWithSingleParameterWithBrace() = doTest(0, "some", null, '{')

    fun testTabReplaceIdentifier() = doTest(1, "sss", null, '\t')
    fun testTabReplaceIdentifier2() = doTest(1, "sss", null, '\t')
    fun testTabReplaceThis() = doTest(1, "sss", null, '\t')
    fun testTabReplaceNull() = doTest(1, "sss", null, '\t')
    fun testTabReplaceTrue() = doTest(1, "sss", null, '\t')
    fun testTabReplaceNumber() = doTest(1, "sss", null, '\t')

    fun testSingleBrackets() {
        fixture.configureByFile(fileName())
        fixture.type('(')
        checkResult()
    }

    fun testInsertFunctionWithBothParentheses() {
        fixture.configureByFile(fileName())
        fixture.type("test()")
        checkResult()
    }

    fun testFunctionLiteralInsertWhenNoSpacesForBraces() {
        val settings = CodeStyleSettingsManager.getSettings(getProject())
        val jetSettings = settings.getCustomSettings(javaClass<JetCodeStyleSettings>())!!

        try {
            jetSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = false
            doTest()
        }
        finally {
            jetSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = true
        }
    }

    fun testObject() = doTest()

    fun testEnumMember() = doTest(1, "A", null, '\n')
    fun testEnumMember1() = doTest(1, "A", null, '\n')
    fun testClassFromClassObject() = doTest(1, "Some", null, '\n')
    fun testClassFromClassObjectInPackage() = doTest(1, "Some", null, '\n')

    fun testParameterType() = doTest(1, "StringBuilder", " (java.lang)", '\n')

    fun testLocalClassCompletion() = doTest(1, "LocalClass", null, '\n')
    fun testNestedLocalClassCompletion() = doTest(1, "Nested", null, '\n')

    fun testTypeArgOfSuper() = doTest(1, "X", null, '\n')

    fun testKeywordClassName() = doTest(1, "class", null, '\n')
    fun testKeywordFunctionName() = doTest(1, "fun", "fun", "()", '\n')

    fun testInfixCall() = doTest(1, "to", null, null, '\n')
    fun testInfixCallOnSpace() = doTest(1, "to", null, null, ' ')

    fun testImportedEnumMember() { doTest(1, "AAA", null, null, '\n') }

    fun testInnerClass() { doTest(1, "Inner", null, null, '\n') }
}
