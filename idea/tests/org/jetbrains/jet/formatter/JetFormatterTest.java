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

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;

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

    public void testCommentInFunctionLiteral() throws Exception {
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

    public void testFunctionWithInference() throws Exception {
        doTest();
    }

    public void testFunctionWithNewLineBrace() throws Exception {
        doTest();
    }

    public void testGetterAndSetter() throws Exception {
        doTest();
    }

    public void testIf() throws Exception {
        doTest();
    }

    public void testMultilineFunctionLiteral() throws Exception {
        doTest();
    }

    public void testMultilineFunctionLiteralWithParams() throws Exception {
        doTestWithInvert();
    }

    public void testParameters() throws Exception {
        doTestWithInvert();
    }

    public void testPropertyWithInference() throws Exception {
        doTest();
    }

    public void testRightBracketOnNewLine() throws Exception {
        doTestWithInvert();
    }

    public void testSaveSpacesInDocComments() throws Exception {
        doTest();
    }

    public void testSingleLineFunctionLiteral() throws Exception {
        doTestWithInvert();
    }

    public void testSpaceAroundExtendColon() throws Exception {
        doTestWithInvert();
    }

    public void testSpaceBeforeFunctionLiteral() throws Exception {
        doTest();
    }

    public void testSpacesAroundOperations() throws Exception {
        doTestWithInvert();
    }

    public void testSpacesAroundUnaryOperations() throws Exception {
        doTestWithInvert();
    }

    public void testUnnecessarySpacesInParametersLists() throws Exception {
        doTest();
    }

    public void testWhen() throws Exception {
        doTestWithInvert();
    }

    public void testWhenEntryExpr() throws Exception {
        doTest();
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
