/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay.Krasko
 */
public class JetBasicCompletionTest extends JetCompletionTestBase {

    public void testBasicAny() {
        doTest();
    }

    public void testBasicInt() {
        doTest();
    }

    public void testBeforeDotInCall() {
        doTest();
    }

    public void testExtendClassName() {
        doTest();
    }

    public void testExtendQualifiedClassName() {
        doTest();
    }

    public void testExtensionFromStandardLibrary() {
        doTest();
    }

    public void testFromImports() {
        doTest();
    }

    public void testInCallExpression() {
        doTest();
    }

    public void testInEmptyImport() {
        doTest();
    }

    public void testInImport() {
        doTest();
    }

    public void testInMiddleOfNamespace() {
        doTest();
    }

    public void testInMiddleOfPackage() {
        doTest();
    }

    public void testInPackage() {
        doTest();
    }

    public void testInTypeAnnotation() {
        doTest();
    }

    public void testJavaClassNames() {
        doTest();
    }

    public void testJavaPackage() {
        doTest();
    }

    public void testNamedObject() {
        doTest();
    }

    public void testNoEmptyNamespace() {
        doTest();
    }

    public void testOverloadFunctions() {
        doTest();
    }

    public void testSubpackageInFun() {
        doTest();
    }

    public void testVariableClassName() {
        doTest();
    }



    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/basic").getPath() +
               File.separator;
    }
}
