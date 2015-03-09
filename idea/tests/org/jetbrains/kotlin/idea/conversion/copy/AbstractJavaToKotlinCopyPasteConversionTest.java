/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.conversion.copy;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.JetWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;
import org.jetbrains.kotlin.idea.editor.JetEditorOptions;

public abstract class AbstractJavaToKotlinCopyPasteConversionTest extends JetLightCodeInsightFixtureTestCase {

    private static final String BASE_PATH = PluginTestCaseBase.getTestDataPathBase() + "/copyPaste/conversion";
    private JetEditorOptions oldState = null;

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        oldState = JetEditorOptions.getInstance().getState();
        JetEditorOptions.getInstance().setEnableJavaToKotlinConversion(true);
        JetEditorOptions.getInstance().setDonTShowConversionDialog(true);
    }

    @Override
    public void tearDown() throws Exception {
        JetEditorOptions.getInstance().loadState(oldState);
        super.tearDown();
    }

    public void doTest(@SuppressWarnings("UnusedParameters") String path) throws Exception {
        myFixture.setTestDataPath(BASE_PATH);
        String testName = getTestName(false);
        myFixture.configureByFile(testName + ".java");
        myFixture.performEditorAction(IdeActions.ACTION_COPY);
        myFixture.configureByFile(testName + ".to.kt");
        myFixture.performEditorAction(IdeActions.ACTION_PASTE);
        myFixture.checkResultByFile(testName + ".expected.kt");
    }
}
