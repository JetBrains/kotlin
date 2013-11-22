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

package org.jetbrains.jet.editor;

import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.IOException;

public class TypedHandlerTest extends LightCodeInsightTestCase {
    public void testTypeStringTemplateStart() {
        doCharTypeTest(
                '{',

                "val x = \"$<caret>\"",

                "val x = \"${}\""
        );
    }

    public void testAutoIndentRightOpenBrace() {
        doCharTypeTest(
                '{',

                "fun test() {\n" +
                "<caret>\n" +
                "}",

                "fun test() {\n" +
                "    {<caret>}\n" +
                "}"
        );
    }

    public void testAutoIndentLeftOpenBrace() {
        doCharTypeTest(
                '{',

                "fun test() {\n" +
                "      <caret>\n" +
                "}",

                "fun test() {\n" +
                "    {<caret>}\n" +
                "}"
        );
    }

    public void testTypeStringTemplateStartWithCloseBraceAfter() {
        doCharTypeTest(
                '{',

                "fun foo() { \"$<caret>\" }",

                "fun foo() { \"${}\" }"
        );
    }

    public void testTypeStringTemplateStartBeforeString() {
        doCharTypeTest(
                '{',

                "fun foo() { \"$<caret>something\" }",

                "fun foo() { \"${}something\" }"
        );
    }

    public void testKT3575() {
        doCharTypeTest(
                '{',

                "val x = \"$<caret>]\"",

                "val x = \"${}]\""
        );
    }

    public void testAutoCloseBraceInFunctionDeclaration() {
        doCharTypeTest(
                '{',

                "fun foo() <caret>",

                "fun foo() {<caret>}"
        );
    }

    public void testAutoCloseBraceInLocalFunctionDeclaration() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    fun bar() <caret>\n" +
                "}",

                "fun foo() {\n" +
                "    fun bar() {<caret>}\n" +
                "}"
        );
    }

    public void testAutoCloseBraceInAssignment() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    val a = <caret>\n" +
                "}",

                "fun foo() {\n" +
                "    val a = {<caret>}\n" +
                "}"
        );
    }

    public void testDoNotAutoCloseBraceInUnfinishedIfSurroundOnSameLine() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    if() <caret>foo()\n" +
                "}",

                "fun foo() {\n" +
                "    if() {foo()\n" +
                "}"
        );
    }

    public void testDoNotAutoCloseBraceInUnfinishedWhileSurroundOnSameLine() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    while() <caret>foo()\n" +
                "}",

                "fun foo() {\n" +
                "    while() {foo()\n" +
                "}"
        );
    }

    public void testDoNotAutoCloseBraceInUnfinishedWhileSurroundOnNewLine() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    while()\n" +
                "<caret>\n" +
                "    foo()\n" +
                "}",

                "fun foo() {\n" +
                "    while()\n" +
                "    {\n" +
                "    foo()\n" +
                "}"
        );
    }

    public void testDoNotAutoCloseBraceInUnfinishedIfSurroundOnOtherLine() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    if(true) <caret>\n" +
                "    foo()\n" +
                "}",

                "fun foo() {\n" +
                "    if(true) {<caret>\n" +
                "    foo()\n" +
                "}"
        );
    }

    public void testDoNotAutoCloseBraceInUnfinishedIfSurroundOnNewLine() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    if(true)\n" +
                "        <caret>\n" +
                "    foo()\n" +
                "}",

                "fun foo() {\n" +
                "    if(true)\n" +
                "    {<caret>\n" +
                "    foo()\n" +
                "}"
        );
    }

    public void testAutoCloseBraceInsideFor() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    for (elem in some.filter <caret>) {\n" +
                "    }\n" +
                "}",

                "fun foo() {\n" +
                "    for (elem in some.filter {<caret>}) {\n" +
                "    }\n" +
                "}"
        );
    }

    public void testAutoCloseBraceInsideForAfterCloseParen() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    for (elem in some.foo(true) <caret>) {\n" +
                "    }\n" +
                "}",

                "fun foo() {\n" +
                "    for (elem in some.foo(true) {<caret>}) {\n" +
                "    }\n" +
                "}"
        );
    }

    public void testAutoCloseBraceBeforeIf() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    <caret>if (true) {}\n" +
                "}",

                "fun foo() {\n" +
                "    {<caret>if (true) {}\n" +
                "}"
        );
    }

    public void testAutoCloseBraceInIfCondition() {
        doCharTypeTest(
                '{',

                "fun foo() {\n" +
                "    if (some.hello (12) <caret>)\n" +
                "}",

                "fun foo() {\n" +
                "    if (some.hello (12) {<caret>})\n" +
                "}"
        );
    }

    public void testTypeLtInFunDeclaration() throws Exception {
        doLtGtTest("fun <caret>");
    }

    public void testTypeLtInOngoingConstructorCall() throws Exception {
        doLtGtTest("fun test() { Collection<caret> }");
    }

    public void testTypeLtInClassDeclaration() throws Exception {
        doLtGtTest("class Some<caret> {}");
    }

    public void testTypeLtInPropertyType() throws Exception {
        doLtGtTest("val a: List<caret> ");
    }

    public void testTypeLtInExtensionFunctionReceiver() throws Exception {
        doLtGtTest("fun <T> Collection<caret> ");
    }

    public void testTypeLtInFunParam() throws Exception {
        doLtGtTest("fun some(a : HashSet<caret>)");
    }

    public void testTypeLtInFun() throws Exception {
        doLtGtTestNoAutoClose("fun some() { <<caret> }");
    }

    public void testTypeLtInLess() throws Exception {
        doLtGtTestNoAutoClose("fun some() { val a = 12; a <<caret> }");
    }

    public void testMoveThroughGT() throws Exception {
        configureFromFileText("a.kt", "val a: List<Set<Int<caret>>>");
        EditorTestUtil.performTypingAction(getEditor(), '>');
        EditorTestUtil.performTypingAction(getEditor(), '>');
        checkResultByText("val a: List<Set<Int>><caret>");
    }

    private void doCharTypeTest(char ch, String beforeText, String afterText) {
        try {
            configureFromFileText("a.kt", beforeText);
            EditorTestUtil.performTypingAction(getEditor(), ch);
            checkResultByText(afterText);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    private void doLtGtTestNoAutoClose(String initText) throws Exception {
        doLtGtTest(initText, false);
    }

    private void doLtGtTest(String initText, boolean shouldCloseBeInsert) throws Exception {
        configureFromFileText("a.kt", initText);

        EditorTestUtil.performTypingAction(getEditor(), '<');
        checkResultByText(shouldCloseBeInsert ? initText.replace("<caret>", "<<caret>>") : initText.replace("<caret>", "<<caret>"));

        EditorTestUtil.performTypingAction(getEditor(), EditorTestUtil.BACKSPACE_FAKE_CHAR);
        checkResultByText(initText);
    }

    private void doLtGtTest(String initText) throws Exception {
        doLtGtTest(initText, true);
    }
}
