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

package org.jetbrains.kotlin.formatter;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.SettingsConfigurator;

import java.io.File;

public abstract class AbstractTypingIndentationTestBase extends LightCodeInsightTestCase {
    public void doNewlineTest(String afterFilePath) throws Exception {
        doNewlineTest(afterFilePath, false);
    }

    public void doNewlineTestWithInvert(String afterInvFilePath) throws Exception {
        doNewlineTest(afterInvFilePath, true);
    }

    public void doNewlineTest(String afterFilePath, boolean inverted) throws Exception {
        String testFileName = afterFilePath.substring(0, afterFilePath.indexOf("."));
        String testFileExtension = afterFilePath.substring(afterFilePath.lastIndexOf("."));

        String originFilePath = testFileName + testFileExtension;
        String originalFileText = FileUtil.loadFile(new File(originFilePath), true);

        try {
            SettingsConfigurator configurator = FormatSettingsUtil.createConfigurator(originalFileText);
            if (!inverted) {
                configurator.configureSettings();
            }
            else {
                configurator.configureInvertedSettings();
            }

            doNewlineTest(originFilePath, afterFilePath);
        }
        finally {
            getSettings().clearCodeStyleSettings();
        }
    }

    private void doNewlineTest(String beforeFilePath, String afterFilePath) {
        configureByFile(beforeFilePath);
        type('\n');

        CaretModel caretModel = getEditor().getCaretModel();
        int offset = caretModel.getOffset();
        String actualTextWithCaret = new StringBuilder(getEditor().getDocument().getText()).insert(offset, EditorTestUtil.CARET_TAG).toString();

        KotlinTestUtils.assertEqualsToFile(new File(afterFilePath), actualTextWithCaret);
    }

    public static CodeStyleSettings getSettings() {
        return CodeStyleSettingsManager.getSettings(getProject());
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
