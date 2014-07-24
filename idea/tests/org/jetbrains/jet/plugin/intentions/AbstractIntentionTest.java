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

package org.jetbrains.jet.plugin.intentions;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightCodeInsightTestCase;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.DirectiveBasedActionUtils;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.testing.ConfigLibraryUtil;
import org.junit.Assert;

import java.io.File;
import java.util.List;

public abstract class AbstractIntentionTest extends LightCodeInsightTestCase {
    private static IntentionAction createIntention(File testDataFile) throws Exception {
        List<File> candidateFiles = Lists.newArrayList();

        File current = testDataFile.getParentFile();
        while (current != null) {
            File candidate = new File(current, ".intention");
            if (candidate.exists()) {
                candidateFiles.add(candidate);
            }
            current = current.getParentFile();
        }

        if (candidateFiles.isEmpty()) {
            throw new AssertionError(".intention file is not found for " + testDataFile +
                                     "\nAdd it to base directory of test data. It should contain fully-qualified name of intention class.");
        }
        if (candidateFiles.size() > 1) {
            throw new AssertionError("Several .intention files are available for " + testDataFile +
                                     "\nPlease remove some of them\n" + candidateFiles);
        }

        String className = FileUtil.loadFile(candidateFiles.get(0)).trim();
        return (IntentionAction) Class.forName(className).newInstance();
    }

    protected void doTest(@NotNull String path) throws Exception {
        IntentionAction intentionAction = createIntention(new File(path));

        configureByFile(path);

        String fileText = FileUtil.loadFile(new File(path), true);

        String minJavaVersion = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// MIN_JAVA_VERSION: ");
        if (minJavaVersion != null && !SystemInfo.isJavaVersionAtLeast(minJavaVersion)) return;

        boolean isWithRuntime = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// WITH_RUNTIME") != null;

        try {
            if (isWithRuntime) {
                ConfigLibraryUtil.configureKotlinRuntime(getModule(), getFullJavaJDK());
            }

            DirectiveBasedActionUtils.checkForUnexpectedErrors((JetFile) getFile());

            doTestFor(path, intentionAction, fileText);
        }
        finally {
            if (isWithRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntime(getModule(), getProjectJDK());
            }
        }
    }

    private void doTestFor(String path, IntentionAction intentionAction, String fileText) {
        String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// IS_APPLICABLE: ");
        boolean isApplicableExpected = isApplicableString == null || isApplicableString.equals("true");

        Assert.assertTrue(
                "isAvailable() for " + intentionAction.getClass() + " should return " + isApplicableExpected,
                isApplicableExpected == intentionAction.isAvailable(getProject(), getEditor(), getFile()));

        String intentionTextString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INTENTION_TEXT: ");

        if (intentionTextString != null) {
            assertEquals("Intention text mismatch.", intentionTextString, intentionAction.getText());
        }

        String shouldFailString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// SHOULD_FAIL_WITH: ");

        try {
            if (isApplicableExpected) {
                intentionAction.invoke(getProject(), getEditor(), getFile());
                // Don't bother checking if it should have failed.
                if (shouldFailString == null) {
                    String canonicalPathToExpectedFile = PathUtil.getCanonicalPath(path + ".after");
                    checkResultByFile(canonicalPathToExpectedFile);
                }
            }
            assertNull("Expected test to fail.", shouldFailString);
        }
        catch (IntentionTestException e) {
            assertEquals("Failure message mismatch.", shouldFailString, e.getMessage());
        }
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }

    protected static Sdk getFullJavaJDK() {
        return PluginTestCaseBase.fullJdk();
    }
}
