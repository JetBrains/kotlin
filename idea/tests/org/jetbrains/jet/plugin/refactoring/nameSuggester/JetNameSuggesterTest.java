/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.nameSuggester;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.refactoring.EmptyValidator;
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester;
import org.jetbrains.jet.plugin.refactoring.JetNameValidator;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringUtil;

import java.util.Arrays;

public class JetNameSuggesterTest extends LightCodeInsightFixtureTestCase {
    public void testArrayList() {
        doTest();
    }

    public void testGetterSure() {
        doTest();
    }

    public void testNameArrayOfClasses() {
        doTest();
    }

    public void testNameArrayOfStrings() {
        doTest();
    }

    public void testNameCallExpression() {
        doTest();
    }

    public void testNameClassCamelHump() {
        doTest();
    }

    public void testNameLong() {
        doTest();
    }

    public void testNameReferenceExpression() {
        doTest();
    }

    public void testNameString() {
        doTest();
    }

    public void testAnonymousObject() {
        doTest();
    }

    public void testAnonymousObjectWithSuper() {
        doTest();
    }

    public void testArrayOfObjectsType() {
        doTest();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/refactoring/nameSuggester");
    }

    private void doTest() {
        myFixture.configureByFile(getTestName(false) + ".kt");
        JetFile file = (JetFile) myFixture.getFile();
        final String expectedResultText = JetTestUtils.getLastCommentInFile(file);
        try {
            JetRefactoringUtil.selectExpression(myFixture.getEditor(), file, new JetRefactoringUtil.SelectExpressionCallback() {
                @Override
                public void run(@Nullable JetExpression expression) {
                    String[] names = JetNameSuggester.suggestNames(expression, EmptyValidator.INSTANCE$, "value");
                    Arrays.sort(names);
                    String result = StringUtil.join(names, "\n").trim();
                    assertEquals(expectedResultText, result);
                }
            });
        } catch (JetRefactoringUtil.IntroduceRefactoringException e) {
            throw new AssertionError("Failed to find expression: " + e.getMessage());
        }

    }
}
