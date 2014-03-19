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

package org.jetbrains.jet.formatter;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.testing.SettingsConfigurator;

import java.io.File;

public class JetTypingIndentationTest extends JetTypingIndentationTestBase {
    public void testAfterCatch() {
        doFileNewlineTest();
    }

    public void testAfterFinally() {
        doFileNewlineTest();
    }

    public void testAfterImport() {
        doFileNewlineTest();
    }

    public void testAfterTry() {
        doFileNewlineTest();
    }

    public void testConsecutiveCallsAfterDot() {
        doFileNewlineTest();
    }

    public void testDoInFun() {
        doFileNewlineTest();
    }

    public void testEmptyParameters() {
        doFileNewlineTest();
    }

    public void testFor() {
        doFileNewlineTest();
    }

    public void testFunctionBlock() {
        doFileNewlineTest();
    }

    public void testFunctionWithInference() {
        doFileNewlineTest();
    }

    public void testIf() {
        doFileNewlineTest();
    }

    public void testNotFirstParameter() {
        doFileNewlineTest();
    }

    public void testPropertyWithInference() {
        doFileNewlineTest();
    }

    public void testSettingAlignMultilineParametersInCalls() throws Exception {
        doFileSettingNewLineTest();
    }

    public void testWhile() {
        doFileNewlineTest();
    }
}

