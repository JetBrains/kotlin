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

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.navigation.CtrlMouseHandler;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.ArrayUtil;
import junit.framework.Assert;
import junit.framework.ComparisonFailure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.ProjectDescriptorWithStdlibSources;
import org.jetbrains.jet.test.util.UtilPackage;

import java.io.File;
import java.util.Collection;
import java.util.List;

public abstract class AbstractJetQuickDocProviderTest extends LightCodeInsightFixtureTestCase {
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
            String expectedInfo = directives.get(0);

            // We can avoid testing for too long comments with \n character by placing '...' in test data
            if (info != null && expectedInfo.endsWith("...")) {
                if (!info.startsWith(StringUtil.trimEnd(expectedInfo, "..."))) {
                    throw new ComparisonFailure(null, expectedInfo, info);
                }
            }
            else {
                Assert.assertEquals(expectedInfo, info);
            }
        }
        else {
            Assert.fail("Too many '// INFO:' directives in file " + path);
        }
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return ProjectDescriptorWithStdlibSources.INSTANCE;
    }
}
