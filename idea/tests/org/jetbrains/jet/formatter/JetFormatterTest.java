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

package org.jetbrains.jet.formatter;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import org.jetbrains.jet.plugin.formatter.JetCodeStyleSettings;

/**
 * Based on com.intellij.psi.formatter.java.JavaFormatterTest
 */
public class JetFormatterTest extends AbstractJetFormatterTest {
    public void testBlockFor() throws Exception {
        doTest();
    }

    public void testClass() throws Exception {
        doTest();
    }

    public void testConsecutiveCalls() throws Exception {
        doTest();
    }

    public void testEmptyLineAfterPackage() throws Exception {
        doTest();
    }

    public void testForNoBraces() throws Exception {
        doTest();
    }

    public void testIf() throws Exception {
        doTest();
    }

    public void testParameters() throws Exception {
        getJetSettings().SPACE_AFTER_TYPE_COLON = true;
        getJetSettings().SPACE_BEFORE_TYPE_COLON = false;
        doTest();
    }

    public void testSpaceAroundTypeColon() throws Exception {
        getJetSettings().SPACE_AFTER_TYPE_COLON = false;
        getJetSettings().SPACE_BEFORE_TYPE_COLON = true;
        doTest();
    }

    public void testWhen() throws Exception {
        doTest();
    }

    public void testWhenEntryExpr() throws Exception {
        doTest();
    }

    public static JetCodeStyleSettings getJetSettings() {
        return getSettings().getCustomSettings(JetCodeStyleSettings.class);
    }

    public static CodeStyleSettings getSettings() {
        return CodeStyleSettingsManager.getSettings(getProject());
    }
}
