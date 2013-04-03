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

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.LightCompletionTestCase;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.jet.InTextDirectivesUtils;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.testing.ConfigLibraryUtil;
import org.jetbrains.jet.utils.ExceptionUtils;

public abstract class JetCompletionTestBase extends LightCompletionTestCase {
    private CompletionType type;

    @Override
    protected abstract String getTestDataPath();

    protected void doTest() {
        try {
            String testName = getTestName(false);
            configureByFileNoComplete(testName + ".kt");

            String fileText = getFile().getText();
            type = getCompletionType(testName, fileText);

            boolean withKotlinRuntime = InTextDirectivesUtils.getPrefixedInt(fileText, "// RUNTIME:") != null;

            try {
                if (withKotlinRuntime) {
                    ConfigLibraryUtil.configureKotlinRuntime(getModule(), getFullJavaJDK());
                }

                Integer completionTime = ExpectedCompletionUtils.getExecutionTime(fileText);

                complete(completionTime == null ? 1 : completionTime);

                ExpectedCompletionUtils.CompletionProposal[] expected = ExpectedCompletionUtils.itemsShouldExist(fileText);
                ExpectedCompletionUtils.CompletionProposal[] unexpected = ExpectedCompletionUtils.itemsShouldAbsent(fileText);
                Integer itemsNumber = ExpectedCompletionUtils.getExpectedNumber(fileText);

                assertTrue("Should be some assertions about completion",
                           expected.length != 0 || unexpected.length != 0 || itemsNumber != null);

                if (myItems == null) {
                    myItems = new LookupElement[0];
                }

                ExpectedCompletionUtils.assertContainsRenderedItems(expected, myItems, ExpectedCompletionUtils.isWithOrder(fileText));
                ExpectedCompletionUtils.assertNotContainsRenderedItems(unexpected, myItems);

                if (itemsNumber != null) {
                    assertEquals(
                            String.format(
                                    "Invalid number of completion items: %s",
                                    ExpectedCompletionUtils.listToString(ExpectedCompletionUtils.getItemsInformation(myItems))),
                            itemsNumber.intValue(), myItems.length);
                }
            }
            finally {
                if (withKotlinRuntime) {
                    ConfigLibraryUtil.unConfigureKotlinRuntime(getModule(), getProjectJDK());
                }
            }
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    protected CompletionType getCompletionType(String testName, String fileText) {
        return (testName.startsWith("Smart")) ? CompletionType.SMART : CompletionType.BASIC;
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    protected static Sdk getFullJavaJDK() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }

    @Override
    protected void complete(int time) {
        new CodeCompletionHandlerBase(type, false, false, true).invokeCompletion(getProject(), getEditor(), time, false, false);

        Lookup lookup = LookupManager.getActiveLookup(myEditor);
        myItems = lookup == null ? null : lookup.getItems().toArray(LookupElement.EMPTY_ARRAY);
        myPrefix = lookup == null ? null : lookup.itemPattern(lookup.getItems().get(0));
    }
}
