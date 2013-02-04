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

package org.jetbrains.jet.plugin.quickfix;

import org.jetbrains.jet.JetTestCaseBuilder;

import java.util.Arrays;
import java.util.List;

public class AutoImportFixTest extends AbstractQuickFixMultiFileTest {

    public void testClassImport() throws Exception {
        doTest();
    }

    public void testExtensionFunctionImport() throws Exception {
        doTest();
    }

    public void testFunctionImport() throws Exception {
        doTest();
    }

    public void testImportInFirstPartInQualifiedExpression() throws Exception {
        doTest();
    }

    public void testImportInFirstPartInUserType() throws Exception {
        doTest();
    }

    public void testNoImportForFunInQualifiedNotFirst() throws Exception {
        doTest();
    }

    public void testNoImportForPrivateClass() throws Exception {
        doTest();
    }

    public void testNoImportInImports() throws Exception {
        doTest();
    }

    public void testNoImportInQualifiedExpressionNotFirst() throws Exception {
        doTest();
    }

    public void testNoImportInQualifiedUserTypeNotFirst() throws Exception {
        doTest();
    }

    public void testNoImportInSafeQualifiedExpressionNotFirst() throws Exception {
        doTest();
    }

    public void testPackageClass() throws Exception {
        doTest();
    }

    @Override
    protected String getCheckFileName() {
        return getTestName(true) + ".after.kt";
    }

    @Override
    protected List<String> getTestFileNames() {
        return Arrays.asList(getTestName(true) + ".before.Main.kt",
                             getTestName(true) + ".before.data.Sample.kt");
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBuilder.getHomeDirectory() + "/idea/testData/quickfix/autoImports/";
    }
}
