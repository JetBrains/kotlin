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

import com.intellij.codeInsight.folding.JavaCodeFoldingSettings;
import com.intellij.codeInsight.folding.impl.JavaCodeFoldingSettingsImpl;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.testing.SettingsConfigurator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public abstract class AbstractKotlinFoldingTest extends LightCodeInsightFixtureTestCase {
    protected void doTest(@NotNull String path) {
        myFixture.testFolding(path);
    }

    protected void doSettingsFoldingTest(@NotNull String path) {
        String fileText;
        try {
            fileText = StringUtil.convertLineSeparators(FileUtil.loadFile(new File(path)));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        JavaCodeFoldingSettings settings = JavaCodeFoldingSettings.getInstance();
        JavaCodeFoldingSettingsImpl restoreSettings = new JavaCodeFoldingSettingsImpl();
        restoreSettings.loadState((JavaCodeFoldingSettingsImpl) settings);

        try {
            String directText = fileText.replaceAll("~true~", "true").replaceAll("~false~", "false");
            directText += "\n\n// Generated from: " + path;

            doExpandSettingsTest(directText, settings);

            // Clean all regions in model to force IDEA treat all regions as new ones
            cleanAllFoldedRegions();

            String invertedText = fileText
                    .replaceAll("~false~", "true").replaceAll("~true~", "false")
                    .replaceAll(SettingsConfigurator.SET_TRUE_DIRECTIVE, "~TEMP_TRUE_DIRECTIVE~")
                    .replaceAll(SettingsConfigurator.SET_FALSE_DIRECTIVE, SettingsConfigurator.SET_TRUE_DIRECTIVE)
                    .replaceAll("~TEMP_TRUE_DIRECTIVE~", SettingsConfigurator.SET_FALSE_DIRECTIVE);
            invertedText += "\n\n// Generated from: " + path + " with !INVERTED! settings";

            doExpandSettingsTest(invertedText, settings);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        finally {
            ((JavaCodeFoldingSettingsImpl) JavaCodeFoldingSettings.getInstance()).loadState(restoreSettings);
        }
    }

    private void cleanAllFoldedRegions() {
        final FoldingModelEx model = (FoldingModelEx) myFixture.getEditor().getFoldingModel();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (FoldRegion region : model.getAllFoldRegions()) {
                    model.removeFoldRegion(region);
                }
            }
        };
        model.runBatchFoldingOperation(runnable);
    }

    private void doExpandSettingsTest(String fileText, JavaCodeFoldingSettings settings) throws IOException {
        SettingsConfigurator configurator = new SettingsConfigurator(fileText, settings);
        configurator.configureSettings();

        VirtualFile tempFile = PlatformTestCase.createTempFile("kt", null, fileText, Charset.defaultCharset());
        myFixture.testFoldingWithCollapseStatus(tempFile.getPath());
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }
}
