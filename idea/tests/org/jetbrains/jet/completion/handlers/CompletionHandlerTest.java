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

package org.jetbrains.jet.completion.handlers;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.LightCompletionTestCase;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;

/**
 * @author Nikolay Krasko
 */
public class CompletionHandlerTest extends LightCompletionTestCase {

    public void testClassCompletionImport() {
        doTest(CompletionType.CLASS_NAME, 1, "SortedSet");
    }

    public void testNoParamsFunction() {
        doTest();
    }

    public void testParamsFunction() {
        doTest();
    }

    public void testInsertJavaClassImport() {
        doTest();
    }

    public void testPropertiesSetter() {
        doTest();
    }

    public void testSingleBrackets() {
        configureByFile(getBeforeFileName());
        type('(');
        checkResultByFile(getAfterFileName());
    }

    public void testExistingSingleBrackets() {
        doTest();
    }

    public void testSureInsert() {
        doTest();
    }

    public void doTest() {
        doTest(CompletionType.BASIC, 2, null);
    }

    public void doTest(CompletionType type, int time, @Nullable String completeItem) {
        try {
            configureByFileNoComplete(getBeforeFileName());
            setType(type);

            complete(time);

            if (completeItem != null) {
                selectItem(LookupElementBuilder.create(completeItem), '\t');
            }

            checkResultByFile(getAfterFileName());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    protected String getBeforeFileName() {
        return getTestName(false) + ".kt";
    }

    protected String getAfterFileName() {
        return getTestName(false) + ".kt.after";
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/handlers/").getPath() + File.separator;
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }
}
