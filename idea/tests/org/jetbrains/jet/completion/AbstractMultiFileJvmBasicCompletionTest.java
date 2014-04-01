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

public abstract class AbstractMultiFileJvmBasicCompletionTest extends JetFixtureCompletionBaseTestCase {

    private static final String JAVA_FILE = "JAVA_FILE:";

    @Override
    protected void setUpFixture(@NotNull String testPath) {
        String[] kotlinTestFiles = getKotlinFiles(testPath);
        myFixture.configureByFiles(kotlinTestFiles);
        PsiFile testFile = myFixture.getFile();
        String text = testFile.getText();
        String javaFilePath = InTextDirectivesUtils.findStringWithPrefixes(text, JAVA_FILE);
        if (javaFilePath != null) {
            myFixture.configureByFile(javaFilePath);
            myFixture.configureByFiles(kotlinTestFiles);
        }
    }

    @NotNull
    private String[] getKotlinFiles(@NotNull String testPath) {
        String testFileName = testPath.substring(testPath.lastIndexOf("/") + 1, testPath.length());
        String secondaryFile = testFileName.replace(".kt", ".dependency.kt");
        if (new File(getTestDataPath() + "/" + secondaryFile).exists()) {
            return new String[] {testFileName, secondaryFile};
        }
        return new String[] {testFileName};
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
