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

package org.jetbrains.jet.completion;

public class JetMultifileBasicCompletionTest extends JetCompletionMultiTestBase {
    public void testCompleteImportedFunction() {
        doTest();
    }

    public void testCompletionOnImportedFunction() {
        doTest();
    }

    public void testDoNotCompleteWithConstraints() {
        doTest();
    }

    public void testTopLevelFunction() throws Exception {
        doTest();
    }

    public void testExtensionFunctionOnImportedFunction() throws Exception {
        doTest();
    }

    public void todotestExtensionFunctionOnUnresolved() throws Exception {
        doTest();
    }

    public void testExtensionOnNullable() throws Exception {
        doTest();
    }

    public void todotestExtensionProperty() throws Exception {
        doTest();
    }

    public void testNotImportedJavaClass() throws Exception {
        doTest();
    }

    public void testInImportedFunctionLiteralParameter() throws Exception {
        doTest();
    }

    public void testJavaInnerClasses() throws Exception {
        doTest();
    }

    public void testNotImportedExtensionFunction() throws Exception {
        doTest();
    }

    public void testExtensionFunction() throws Exception {
        doTest();
    }

    public void testNotImportedObject() throws Exception {
        doTest();
    }
}
