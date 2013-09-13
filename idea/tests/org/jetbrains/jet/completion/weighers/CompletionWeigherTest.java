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

package org.jetbrains.jet.completion.weighers;

import com.intellij.codeInsight.completion.CompletionAutoPopupTestCase;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

public class CompletionWeigherTest extends CompletionAutoPopupTestCase {
    public void testLocalValuesAndParams() {
        doTest("init", "initLocal", "initParam", "initGlobal");
    }

    public void testTemplatesAndKeywordsLast() {
        doTest("va", "values", "variables", "val ... = ...", "var ... = ...", "vararg");
    }

    public void testDeprecatedFun() {
        doTest("foo", "foo1", "foo3", "foo2");
    }

    public void testLocalsBeforeKeywords() {
        doTest("a", "a", "as");
    }

    public void testParametersBeforeKeywords() {
        doTest("fo", "fo", "for (... in ...) {...}");
    }

    public void testLocalsPropertiesKeywords() {
        doTest("a", "fals", "falt", "false");
    }

    public void doTest(String type, @NonNls String... expected) {
        new WriteCommandAction(myFixture.getProject(), myFixture.getFile()) {
            @Override
            protected void run(Result result) throws Throwable {
                myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/completion/weighers/");
                myFixture.configureByFile(getTestName(false) + ".kt");
            }
        }.execute();

        type(type);
        myFixture.assertPreferredCompletionItems(0, expected);
    }
}
