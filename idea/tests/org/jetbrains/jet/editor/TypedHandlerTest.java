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
