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

package org.jetbrains.jet.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.project.TargetPlatform;

import java.io.File;
import java.util.Collections;
import java.util.List;

public abstract class JetCompletionMultiTestBase extends JetFixtureCompletionBaseTestCase {

    public static final String JAVA_FILE = "JAVA_FILE:";

    protected void doTest() {
        doTest(getTestName(false));
    }

    @Override
    protected void setUpFixture(@NotNull String testPath) {
        myFixture.configureByFiles(getFileNameList(testPath));
        PsiFile testFile = myFixture.getFile();
        String text = testFile.getText();
        String javaFilePath = InTextDirectivesUtils.findStringWithPrefixes(text, JAVA_FILE);
        if (javaFilePath != null) {
            myFixture.configureByFile(javaFilePath);
            myFixture.configureByFiles(getFileNameList(testPath));
        }
    }

    @NotNull
    private String[] getFileNameList(@NotNull String testPath) {
        String baseFile = testPath + "-1.kt";
        String secondaryFile = testPath + "-2.kt";
        if (new File(getTestDataPath() + "/" + secondaryFile).exists()) {
            return new String[] {baseFile, secondaryFile};
        }
        return new String[] {baseFile};
    }

    @Override
    public TargetPlatform getPlatform() {
        return TargetPlatform.JVM;
    }

    @NotNull
    @Override
    protected CompletionType completionType() {
        return CompletionType.BASIC;
    }

    @NotNull
    @Override
    protected List<String> getAdditionalDirectives() {
        return Collections.singletonList(JAVA_FILE);
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/completion/basic/multifile/";
    }
}
