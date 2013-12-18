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

package org.jetbrains.jet.plugin.navigation;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.util.List;

public abstract class AbstractGotoSuperTest extends LightCodeInsightFixtureTestCase {
    protected void doTest(String testPath) {
        List<String> parts = JetTestUtils.loadBeforeAfterText(testPath);

        myFixture.configureByText(JetFileType.INSTANCE, parts.get(0));

        CodeInsightActionHandler gotoSuperAction = (CodeInsightActionHandler) ActionManager.getInstance().getAction("GotoSuperMethod");
        gotoSuperAction.invoke(getProject(), myFixture.getEditor(), myFixture.getFile());

        myFixture.checkResult(parts.get(1));
    }
}