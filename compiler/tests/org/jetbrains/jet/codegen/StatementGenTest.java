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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;

public class StatementGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    private void doTest() {
        loadFile("statements/" + getTestName(true) + ".kt");
        String text = generateToText();
        // 'getstatic' means we refer to Unit.VALUE, which we shouldn't since these tests contain only statements
        assertNoGetStatic(text);
    }

    private void assertNoGetStatic(@NotNull String text) {
        assertFalse(text, text.toLowerCase().contains("getstatic"));
    }

    public void testIfSingleBranch() {
        doTest();
    }

    public void testIfThenElse() {
        doTest();
    }

    public void testIfThenElseEmpty() {
        doTest();
    }

    public void testTryCatchFinally() {
        doTest();
    }

    public void testWhen() {
        doTest();
    }

    public void testWhenSubject() {
        doTest();
    }
}
