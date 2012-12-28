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
 * Test auto completion messages
 */
public class KeywordsCompletionTest extends JetCompletionTestBase {

    public void testAfterClassProperty() {
        doTest();
    }

    public void testAfterDot() {
        doTest();
    }

    public void testAfterSpaceAndDot() {
        doTest();
    }

    public void testclassObject() {
        doTest();
    }

    public void testInBlockComment() {
        doTest();
    }

    public void testInChar() {
        doTest();
    }

    public void testInClassBeforeFun() {
        doTest();
    }

    public void testInClassProperty() {
        doTest();
    }

    public void testInClassScope() {
        doTest();
    }

    public void testInClassTypeParameters() {
        doTest();
    }

    public void testInFunctionName() {
        doTest();
    }

    public void testInFunctionScope() {
        doTest();
    }

    public void testInNotFinishedGenericWithFunAfter() {
        doTest();
    }

    public void testInFunctionTypeReference() {
        doTest();
    }

    public void testInParametersList() {
        doTest();
    }

    public void testInPropertyTypeReference() {
        doTest();
    }

    public void testInMethodParametersList() {
        doTest();
    }

    public void testInModifierListInsideClass() {
        doTest();
    }

    public void testInString() {
        doTest();
    }

    public void testInTopProperty() {
        doTest();
    }

    public void testInTopScopeAfterPackage() {
        doTest();
    }

    public void testInTypeScope() {
        doTest();
    }

    public void testLineComment() {
        doTest();
    }

    public void testNoCompletionForCapitalPrefix() {
        doTest();
    }

    public void testPropertySetterGetter() {
        doTest();
    }

    public void testTopScope() {
        doTest();
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/keywords").getPath() +
               File.separator;
    }
}
