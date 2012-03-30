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
    public void testAddSpacesAroundOperations() throws Exception {
        getSettings().SPACE_AROUND_ASSIGNMENT_OPERATORS = true;
        getSettings().SPACE_AROUND_LOGICAL_OPERATORS = true;
        getSettings().SPACE_AROUND_EQUALITY_OPERATORS = true;
        getSettings().SPACE_AROUND_RELATIONAL_OPERATORS = true;
        getSettings().SPACE_AROUND_ADDITIVE_OPERATORS = true;
        getSettings().SPACE_AROUND_MULTIPLICATIVE_OPERATORS = true;
        getSettings().SPACE_AROUND_UNARY_OPERATOR = true;
        getJetSettings().SPACE_AROUND_RANGE = true;

        doTest();

        getSettings().clearCodeStyleSettings();
    }

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

    public void testFunctionCallParametersAlign() throws Exception {
        doTest();
    }

    public void testFunctionDefParametersAlign() throws Exception {
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

    public void testRemoveSpacesAroundOperations() throws Exception {
        getSettings().SPACE_AROUND_ASSIGNMENT_OPERATORS = false;
        getSettings().SPACE_AROUND_LOGICAL_OPERATORS = false;
        getSettings().SPACE_AROUND_EQUALITY_OPERATORS = false;
        getSettings().SPACE_AROUND_RELATIONAL_OPERATORS = false;
        getSettings().SPACE_AROUND_ADDITIVE_OPERATORS = false;
        getSettings().SPACE_AROUND_MULTIPLICATIVE_OPERATORS = false;
        getSettings().SPACE_AROUND_UNARY_OPERATOR = false;
        getJetSettings().SPACE_AROUND_RANGE = false;

        doTest();

        getSettings().clearCodeStyleSettings();
    }

    public void testRightBracketOnNewLine() throws Exception {
        doTestWithInvert();
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

    @Override
    public void doTest() throws Exception {
        String originalFileText = AbstractJetFormatterTest.loadFile(getTestName(false) + ".kt");

        String afterFileName = getTestName(false) + "_after.kt";
        String afterText = AbstractJetFormatterTest.loadFile(afterFileName);

        FormattingSettingsConfigurator configurator = new FormattingSettingsConfigurator(AbstractJetFormatterTest.loadFile(
                getTestName(false) + ".kt"));
        configurator.configureSettings(getSettings());

        doTextTest(originalFileText, afterText, String.format("Failure in NORMAL file: %s", afterFileName));

        getSettings().clearCodeStyleSettings();
    }

    public void doTestWithInvert() throws Exception {
        String originalFileText = AbstractJetFormatterTest.loadFile(getTestName(false) + ".kt");
        FormattingSettingsConfigurator configurator = new FormattingSettingsConfigurator(originalFileText);

        String afterFileName = getTestName(false) + "_after.kt";
        String afterText = AbstractJetFormatterTest.loadFile(afterFileName);
        configurator.configureSettings(getSettings());
        doTextTest(originalFileText, afterText, String.format("Failure in NORMAL file: %s", afterFileName));

        String afterInvertedFileName = getTestName(false) + "_after_inv.kt";
        String afterInvertedText = AbstractJetFormatterTest.loadFile(afterInvertedFileName);
        configurator.configureInvertedSettings(getSettings());
        doTextTest(originalFileText, afterInvertedText, String.format("Failure in INVERTED file: %s", afterInvertedFileName));

        getSettings().clearCodeStyleSettings();
    }
}
