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

package org.jetbrains.jet.plugin.folding;

import com.google.common.base.Function;
import com.intellij.codeInsight.folding.JavaCodeFoldingSettings;
import com.intellij.codeInsight.folding.impl.JavaCodeFoldingSettingsImpl;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.testing.SettingsConfigurator;
import org.junit.Assert;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public abstract class AbstractKotlinFoldingTest extends LightCodeInsightFixtureTestCase {
    protected void doTest(@NotNull String path) {
        myFixture.testFolding(path);
    }

    protected void doSettingsFoldingTest(@NotNull String path) throws IOException{
        String fileText = FileUtil.loadFile(new File(path), true);

        String directText = fileText.replaceAll("~true~", "true").replaceAll("~false~", "false");
        directText += "\n\n// Generated from: " + path;

        Function<String, Void> doExpandSettingsTestFunction = new Function<String, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable String fileText) {
                doExpandSettingsTest(fileText);
                return null;
            }
        };

        doTestWithSettings(directText, doExpandSettingsTestFunction);

        String invertedText = fileText
                .replaceAll("~false~", "true").replaceAll("~true~", "false")
                .replaceAll(SettingsConfigurator.SET_TRUE_DIRECTIVE, "~TEMP_TRUE_DIRECTIVE~")
                .replaceAll(SettingsConfigurator.SET_FALSE_DIRECTIVE, SettingsConfigurator.SET_TRUE_DIRECTIVE)
                .replaceAll("~TEMP_TRUE_DIRECTIVE~", SettingsConfigurator.SET_FALSE_DIRECTIVE);
        invertedText += "\n\n// Generated from: " + path + " with !INVERTED! settings";

        doTestWithSettings(invertedText, doExpandSettingsTestFunction);
    }

    protected static void doTestWithSettings(@NotNull String fileText, @NotNull Function<String, Void> runnable) {
        JavaCodeFoldingSettings settings = JavaCodeFoldingSettings.getInstance();
        JavaCodeFoldingSettingsImpl restoreSettings = new JavaCodeFoldingSettingsImpl();
        restoreSettings.loadState((JavaCodeFoldingSettingsImpl) settings);

        try {
            SettingsConfigurator configurator = new SettingsConfigurator(fileText, settings);
            configurator.configureSettings();

            runnable.apply(fileText);
        }
        finally {
            ((JavaCodeFoldingSettingsImpl) JavaCodeFoldingSettings.getInstance()).loadState(restoreSettings);
        }
    }

    private void doExpandSettingsTest(String fileText) {
        try {
            VirtualFile tempFile = PlatformTestCase.createTempFile("kt", null, fileText, Charset.defaultCharset());
            assertFoldingRegionsForFile(tempFile.getPath(), true);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // Rewritten version of CodeInsightTestFixtureImpl.testFoldingRegions(verificationFileName, true).
    // Configure test with custom file name to force creating different editors for normal and inverted tests.
    private void assertFoldingRegionsForFile(String verificationFileName, boolean doCheckCollapseStatus) {
        String START_FOLD = "<fold\\stext=\'[^\']*\'(\\sexpand=\'[^\']*\')*>";
        String END_FOLD = "</fold>";

        String expectedContent;
        File file = new File(verificationFileName);

        try {
            expectedContent = FileUtil.loadFile(file);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotNull(expectedContent);

        expectedContent = StringUtil.replace(expectedContent, "\r", "");
        String cleanContent = expectedContent.replaceAll(START_FOLD, "").replaceAll(END_FOLD, "");

        myFixture.configureByText(file.getName(), cleanContent);
        String actual = ((CodeInsightTestFixtureImpl)myFixture).getFoldingDescription(doCheckCollapseStatus);

        Assert.assertEquals(expectedContent, actual);
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }
}
