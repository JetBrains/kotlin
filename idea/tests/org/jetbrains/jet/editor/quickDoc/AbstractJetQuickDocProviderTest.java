/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.editor.quickDoc;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.navigation.CtrlMouseHandler;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.jet.plugin.ProjectDescriptorWithStdlibSources;
import org.jetbrains.jet.test.util.UtilPackage;
import org.junit.Assert;

import java.io.File;
import java.util.List;

public abstract class AbstractJetQuickDocProviderTest extends JetLightCodeInsightFixtureTestCase {
    public void doTest(@NotNull String path) throws Exception {
        UtilPackage.configureWithExtraFile(myFixture, path, "_Data");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        assertNotNull("Can't find element at caret in file: " + path, element);

        DocumentationManager documentationManager = DocumentationManager.getInstance(myFixture.getProject());
        PsiElement targetElement = documentationManager.findTargetElement(myFixture.getEditor(), myFixture.getFile());

        String info = CtrlMouseHandler.getInfo(targetElement, element);

        File testDataFile = new File(path);
        String textData = FileUtil.loadFile(testDataFile, true);
        List<String> directives = InTextDirectivesUtils.findLinesWithPrefixesRemoved(textData, "INFO:");

        if (directives.isEmpty()) {
            throw new FileComparisonFailure(
                    "'// INFO:' directive was expected",
                    textData,
                    textData + "\n\n//INFO: " + info,
                    testDataFile.getAbsolutePath());
        }
        else if (directives.size() == 1) {
            assertNotNull(info);

            String expectedInfo = directives.get(0);

            // We can avoid testing for too long comments with \n character by placing '...' in test data
            if (expectedInfo.endsWith("...")) {
                if (!info.startsWith(StringUtil.trimEnd(expectedInfo, "..."))) {
                    wrapToFileComparisonFailure(info, path, textData);
                }
            }
            else if (!expectedInfo.equals(info)) {
                wrapToFileComparisonFailure(info, path, textData);
            }
        }
        else {
            Assert.fail("Too many '// INFO:' directives in file " + path);
        }
    }

    private static void wrapToFileComparisonFailure(String info, String filePath, String fileData) {
        int newLineIndex = info.indexOf('\n');
        if (newLineIndex != -1) {
            info = info.substring(0, newLineIndex) + "...";
        }

        String correctedFileText = fileData.replaceFirst("//\\s?INFO: .*", "// INFO: " + info);
        throw new FileComparisonFailure("Unexpected info", fileData, correctedFileText, new File(filePath).getAbsolutePath());
    }


    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return ProjectDescriptorWithStdlibSources.INSTANCE;
    }
}
